package controllers

import java.net.URLDecoder
import javax.inject.Inject
import java.nio.file.{Paths, NoSuchFileException}
import play.api.libs.json.{Json, JsValue, JsNumber, JsString, JsArray, JsNull}
import play.api.mvc.{
  Request, AnyContent, AnyContentAsEmpty, AnyContentAsJson,
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
  ) (cc: ControllerComponents) 
  extends AbstractController(cc) with APIController {

  val allowedParams = List("comids", "statistics", "variables", "begin_year",
    "end_year", "months")
  val csvHeaderLine = "comid,statistic,variable,year,month,value\n"
  val dataDirectory = "pdump/"

  /**
   *  Years filter is specific to StreamController
   */
  def yearsFilter(in: List[ByteString], begin: String, end: String):
    Boolean = {
      val checkThisOne = in(3).utf8String
      begin <= checkThisOne && end >= checkThisOne
    }

  /**
   *  Month filter is specific to StreamController albeit it might work
   *  as special case of a more general filter class
   */
  def monthsFilter(in: List[ByteString], monthsList: List[ByteString]):
    Boolean = {
      monthsList.foldLeft(false) {_ || in(4) == _}
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
   * Create a default filename from what we have
   */
  def filenameFromRequest(request: Request[AnyContent]): String = {
    val query = getQuery(request)
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

  def sourceFactory(query: Map[String, Seq[String]]): 
    Source[ByteString, NotUsed] = {

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
            Paths.get(dataDirectory, inValue(0),  "/", inValue(1), "/", 
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

    Source.fromGraph(GraphDSL.create() {
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

      /** Not sure whether that is the most elegant way to switch between 
       *  sources but works for now.
       */
      val source = if (comidsListFilter(query))
        { source2 } else { source1 }

      val merge = builder.add(Merge[ByteString](2))

      /**
       * Use Partition to circumvent filter function if no filter values
       * provided
       */
      val partition = builder.add(Partition[ByteString]
        (2, in => if (filterSwitch) 0 else 1))

      /**
       * Assemble the flow
       */
      source ~> fileFlow ~> partition.in
      partition.out(0) ~> filterFlow ~> merge.in(0)
      partition.out(1) ~> merge.in(1)
      SourceShape(merge.out)
    })
  }

  /**
   * A play view applying filters and streaming CSV data from files to 
   * download
   *
   * This is still duplicated because of dependency injection
   * TODO: improve
   */
  def chunkedFromSource() = Action {
    implicit request: Request[AnyContent] =>
    val filename = filenameFromRequest(request)
    val source = csvSourceFactory(request)
    Ok.chunked(source)
      .withHeaders(
        CONTENT_DISPOSITION -> "attachment; filename=".concat(filename))
      .as("text/csv")
  }


}
