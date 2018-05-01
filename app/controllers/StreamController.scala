package controllers

import java.net.URLDecoder
import javax.inject.Inject
import java.nio.file.{Paths, NoSuchFileException}
import play.api.mvc.{
  Request, AnyContent, AnyContentAsEmpty, AnyContentAsText,
  AbstractController, ControllerComponents}
import play.core.parsers.FormUrlEncodedParser
import akka.NotUsed
import akka.stream.SourceShape
import akka.stream.scaladsl.{
  FileIO, Source, Sink, GraphDSL, Merge, Flow, Partition, Concat}
import akka.util.ByteString
import akka.stream.alpakka.csv.scaladsl.CsvParsing

/**
 *  Stream CSV data according to requested stream segments (comids) and
 *  filters.
 */
class StreamController @Inject(
  ) (cc: ControllerComponents) extends AbstractController(cc) {

  def yearsFilter(in: List[ByteString], begin: String, end: String):
  Boolean = {
    val checkThisOne = in(3).utf8String
    begin <= checkThisOne && end >= checkThisOne
  }

  def monthsFilter(in: List[ByteString], monthsList: List[ByteString]):
    Boolean = {
      monthsList.foldLeft(false) {_ || in(4) == _}
    }

  /**
   * 1. Normalize query parameters to deal with different representations of
   * lists in urls: ?list=item1,item2 and ?list=item1&list=item2.
   * 2. Convert to ByteString in accordance with Akka.
   */
  def normalize(in: Seq[String]): List[ByteString] = {
    in.foldLeft(List[ByteString]()) { 
      _ ++ _.split(",").map(_.trim()).map(ByteString(_))
    }
  }

  /**
   * Extract query parameters from requests by keyword.
   */
  def getValues(key: String, in: Map[String, Seq[String]]): 
    List[ByteString] = 
  {
    val values = in.get(key)
    values match {
      case Some(values) => normalize(values)
      case None => List()
    }
  }

  /**
   * Get switch from query parameters and decide whether stream should pass
   * filterFlow.
   */
  def getSwitch(in: Map[String, Seq[String]], lst: List[String]): Boolean = {
    lst.foldLeft(false)(_ || !getValues(_, in).isEmpty)
  }

  /**
   * Limit values extracted from query parameters by a default list. Extend to
   * allowed options if parameter is not present or has no value after equals
   * sign.
   */
  def getLimitedValues(
    key: String, in: Map[String, Seq[String]], allowed: List[String]
  ) : List[ByteString] = {
    val ret = getValues(key, in)
    val opts = allowed.map(ByteString(_))
    if (ret.isEmpty || ret(0).isEmpty) {
      opts.toList
    } else {
      ret.filter(opts contains _).toList
    }
  }

  /**
   *  Check whether input is a valid year and return default if empty
   *  return String for further processing
   */
  def getYearOrDefault(key: String, in: Map[String, Seq[String]],
    default: String): String = {
      getValues(key, in).headOption.getOrElse(ByteString(default)).utf8String
  }

  /**
   * Format CSV output stream
   */
  def formatCsvLine(lst: List[ByteString]): ByteString = {
    lst.reduce(_ ++ ByteString(",") ++ _) ++ ByteString("\n")
  }

  /**
   * Create a default filename from what we have
   */
  def filenameFromQuery(query: Map[String, Seq[String]]): String = {
    val comids = getValues("comids", query).take(5)
    val statistics = getValues("statistics", query)
    val variables = getValues("variables", query)
    val begin_year = getValues("begin_year", query).take(1)
    val end_year = getValues("end_year", query).take(1)
    val months = getValues("months", query)
    val partList = List(ByteString("flow")) ++ comids ++ statistics ++
      variables ++ begin_year ++ end_year ++ months
    partList.map(_.utf8String.trim()).mkString("_") ++ ".csv"
  }

  /**
   * A play view that streams CSV data from file to download and applying
   * filters.
   */
  def chunkedFromSource() = Action {

    implicit request: Request[AnyContent] =>

    /**
     * Check for POST data, use GET data if not available
     */
    val query = request.body.asFormUrlEncoded.getOrElse(request.queryString)

    val outputFilename = filenameFromQuery(query)

    val csvHeaderLine = "comid,statistic,variable,year,month,value\n"

    val statistics = getLimitedValues("statistics", query,
      List("max", "min", "mean", "median"))

    val variables = getLimitedValues("variables", query,
      List("estimated", "p10", "p90", "observed"))

    val months = getLimitedValues("months", query,
      (1 until 13).map(_.toString).toList)

    val beginYear = getYearOrDefault("begin_year", query, "1950")

    val endYear = getYearOrDefault("end_year", query, "2015")

    val filterSwitch = getSwitch(query,
      List("begin_year", "end_year", "months"))

    /**
     * Convert comids in query params to list
     */
    val comidList = getValues("comids", query)
      .map(_.utf8String)
      .toList

    /**
     *  Flow generating a stream of rows from an incoming stream of comid's
     *  by multiplying with requested dimensions and retrieving files.
     */
    val fileFlow = Flow[String]
      .flatMapConcat({
        comid => Source(statistics.map(in => List(comid, in.utf8String)))
      })
      .flatMapConcat({
        item => Source(variables.map(in => item ++ List(in.utf8String)))
      })
      .flatMapConcat({
        inValue =>
          FileIO.fromPath(
            Paths.get("pdump/", inValue(0),  "/", inValue(1), "/", 
              inValue(2) + ".csv"))
          /**
           *  .recover catches NoSuchFileException and passes an empty
           *  ByteString to downstream stages. The use of .filterNot is
           *  somewhat murky here but works otherwise downstream stages
           *  would fail. Is there a better way to get the types right?
           *  TODO: check orElse instead
           */
            .recover({ case _: NoSuchFileException => ByteString() } )
            .filterNot({_ == ByteString()})
      })

    /**
     *  Flow, filters by query parameters, used only for years and months
     */
    val filterFlow = Flow[ByteString]
      .via(CsvParsing.lineScanner())
      .filter(yearsFilter(_, beginYear, endYear))
      .filter(monthsFilter(_, months))
      .map(formatCsvLine)

    val source = Source.fromGraph(GraphDSL.create() {
      implicit builder =>
        import GraphDSL.Implicits._

      /**
       * Stream comids requested by query parameters
       */
      val source1 = Source(comidList)

      /**
       * Stream source from index.csv file, when no comids are provided
       * Assume that this is faster than stat'ing a folder with ~130.000
       * subfolders.
       */
      val source2 = FileIO
        .fromPath(Paths.get("pdump/index.csv"))
        .via(CsvParsing.lineScanner()).map(_(0).utf8String)

      val merge = builder.add(Merge[ByteString](3))

      /**
       * Use Partition to circumvent filter function if no filter values
       * provided
       */
      val partition = builder.add(Partition[ByteString]
        (2, in => if (filterSwitch) 0 else 1))

      /**
       * Assemble the flow
       */
      source1 ~> fileFlow ~> filterFlow ~> merge.in(0)
      source2.filter(_ => comidList.isEmpty) ~> fileFlow ~> partition.in
      partition.out(0) ~> filterFlow ~> merge.in(1)
      partition.out(1) ~> merge.in(2)

      SourceShape(merge.out)
    })

    /**
     * Add CSV header and sink entire flow to chunked HTTP response using Play
     * framework
     */
    val header = Source(List(ByteString(csvHeaderLine)))
    val csvSource = Source
      .combine(
        header:Source[ByteString, NotUsed], 
        source:Source[ByteString, NotUsed])(Concat(_))
    Ok.chunked(csvSource)
      .withHeaders(
        CONTENT_DISPOSITION -> "attachment; filename=".concat(outputFilename))
      .as("text/csv")
  }

}
