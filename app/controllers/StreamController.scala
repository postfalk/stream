package controllers

import javax.inject.Inject
import java.nio.file.{Paths, NoSuchFileException}
import play.api.mvc.{
  Request, AnyContent, AbstractController, ControllerComponents}
import akka.NotUsed
import akka.stream.SourceShape
import akka.stream.scaladsl.{
  FileIO, Source, Sink, GraphDSL, Merge, Flow, Partition, Concat}
import akka.util.ByteString
import akka.stream.alpakka.csv.scaladsl.CsvParsing


/**
 *  Stream CSV data according to requested stream segments and filters
 */
class StreamController @Inject(
  ) (cc: ControllerComponents) extends AbstractController(cc) {

  val csvHeaderLine = "comid,measurement,variable,year,month,value\n"

  /**
   * Filter function to filter stream by query parameters
   * TODO: Generalize!
   */
  def myFilter(
    in: List[ByteString],
    query: List[Seq[ByteString]]): Boolean =
  {
    (0 until 2).foldLeft(true) {
      (agg, i) => {
        agg &&
          (query(i).isEmpty || query(i).foldLeft(false) { _ || in(i+3) == _ })
      }
    }
  }

  /**
   * 1. Normalize query parameters to deal with different representations of
   * lists in urls: ?list=item1,item2 and ?list=item1&list=item2.
   * 2. Convert to ByteString in accordance with Akka.
   */
  def normalize(in: Seq[String]): Seq[ByteString] = {
    in.foldLeft(List[ByteString]()) { _ ++ _.split(",").map(ByteString(_)) }
  }

  /**
   * Extract query parameters from requests by keyword.
   */
  def getValues(
    key: String, in: Map[String, Seq[String]]
  ) : Seq[ByteString] = {
    val values = in.get(key)
    values match {
      case Some(values) => normalize(values)
      case None => List()
    }
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
   * Create a list of query values to be applied by the filter function. Query
   * values are mapped by list position.
   */
  def getFilterList(
    in: Request[AnyContent],
    keys: List[String]): List[Seq[ByteString]] = {
      keys.map(getValues(_, in.queryString))
  }
  /**
   * Additional manipulations of the filterList. Currently extends
   * yearList if yearBegin and yearEnd exist
   */
  def extendFilterList(
    in: Request[AnyContent], 
    lst: List[Seq[ByteString]]): List[Seq[ByteString]] = {
      val yearsBegin = getValues("years_begin", in.queryString)
      val yearsEnd = getValues("years_end", in.queryString)
    lst
  }

  /**
   * Format CSV output stream
   */
  def formatCsvLine(lst: List[ByteString]): ByteString = {
    lst.reduce(_ ++ ByteString(",") ++ _) ++ ByteString("\n")
  }

  /**
    *  Checks whether filterList is empty or not
    */
  def partitionFunction(in:List[Seq[ByteString]]):Boolean = {
    in.foldLeft(true) {(acc, i) => acc && i.isEmpty }
  }

  /**
    * Present some data from query params in filename. Replace illegal
    * characters (TODO: incomplete) and limit filename to 64 characters.
    */
  def queryToFilename(in: String): String = {
    in.replace('=', '_').replace('&', '_').slice(0, 60)
  }

  /**
   * A play view that streams CSV data from file to download and applying 
   * filters.
   */
  def chunkedFromSource() = Action {

    implicit request: Request[AnyContent] =>

    val filename = "flow_" + queryToFilename(request.rawQueryString) +
      ".csv"

    val measurements = getLimitedValues("measurements", request.queryString,
      List("max", "min", "mean", "median"))

    val variables = getLimitedValues("variables", request.queryString,
      List("estimated", "p10", "p90", "observed"))

    /**
     * Keywords used for filtering, they will be matched by position in list
     */
    val keywords = List("years", "months")

    /**
     * Create list from query parameters and convert to immutable list
    */
    val filterList = extendFilterList(
      request, getFilterList(request, keywords))

    /**
     * Set query before passing function to flow
     */
    def filterFunction(in: List[ByteString]): Boolean =
      myFilter(in: List[ByteString], filterList)

    /**
     * Convert segments in query params to a list
     */
    val segmentList = getValues("segments", request.queryString)
      .map(_.utf8String)
      .toList

    /**
     *  Flow generating a stream of rows from an incoming stream of comid's 
     *  by multiplying with requested dimensions and retrieving files.
     */
    val fileFlow = Flow[String]
      .flatMapConcat({
        comid => Source(measurements.map(in => List(comid, in.utf8String)))
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
           */
            .recover({ case _: NoSuchFileException => ByteString() } )
            .filterNot({_ == ByteString()})
      })

    /**
     *  Flow filtering by query parameters, used only for years and months
     */
    val filterFlow = Flow[ByteString]
      .via(CsvParsing.lineScanner())
      .filter(filterFunction)
      .map(formatCsvLine)

    val source = Source.fromGraph(GraphDSL.create() {
      implicit builder =>
        import GraphDSL.Implicits._

      /**
       * Stream segments requested through query parameters
       */
      val source1 = Source(segmentList)

      /**
       * Stream source from index.csv file, when no segments are provided
       * Assuming that this is faster than stating a folder with ~130.000
       * subfolders.
       */
      val source2 = FileIO
        .fromPath(Paths.get("pdump/index.csv"))
        .via(CsvParsing.lineScanner()).map(_(0).utf8String)

      val merge = builder.add(Merge[ByteString](3))

      /**
       * Using Partition here to circumvent filter function if no filter
       * values provided
       */
      val partition = builder.add(Partition[ByteString]
        (2, in => if (partitionFunction(filterList)) 1 else 0))

      /**
       * Assembling the flow
       */
      source1 ~> fileFlow ~> filterFlow ~> merge.in(0)
      source2.filter(_ => segmentList.isEmpty) ~> fileFlow ~> partition.in
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
        CONTENT_DISPOSITION -> "attachment; filename=".concat(filename))
      .as("text/csv")
  }

}
