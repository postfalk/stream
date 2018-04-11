package controllers

import javax.inject.Inject
import java.nio.file.{Paths, NoSuchFileException}
import play.api.mvc.{
  Action, Request, AnyContent, AbstractController, ControllerComponents}
import akka.NotUsed
import akka.event.Logging
import akka.actor.ActorSystem
import akka.stream.stage.{
  GraphStage, GraphStageLogic, InHandler, OutHandler}
import akka.stream.{
  Attributes, Inlet, Outlet, SourceShape, FlowShape}
import akka.stream.scaladsl.{
  FileIO, Source, Sink, GraphDSL, Merge, Flow, Partition, Concat}
import akka.util.ByteString
import akka.stream.alpakka.csv.scaladsl.CsvParsing

/**
 *  Experiment with custom flow stage for in-memory processing
 *  (i.e. filtering) of chunks. This stage chunks stream along CSV line
 *  boundaries. A chunk would contain many CSV lines but ensure that a line
 *  break coincident with end of the chunk. This stage holds state between
 *  chunks (leftover from incoming chunk between last line break and chunk
 *  end.)
 *
 *  Currently unused but tested. Might come in handy in later improvements.
 */
class MyCSVStage extends GraphStage[FlowShape[ByteString, ByteString]] {

  private val in = Inlet[ByteString](Logging.simpleName(this) + ".in")
  private val out = Outlet[ByteString](Logging.simpleName(this) + ".out")
  override val shape = FlowShape(in, out)

  override def createLogic(inheritedAttributes: Attributes) = 
    new GraphStageLogic(shape) with InHandler with OutHandler {

    setHandlers(in, out, this)
    var leftover = ByteString()

    override def onPush(): Unit = {
      val elem = leftover ++ grab(in)
      val endPosition = elem.lastIndexOf(10)
      val send = elem.slice(0, endPosition+1)
      leftover = elem.slice(endPosition+1, elem.size+1)
      push(out, send)
    }

    override def onPull(): Unit = {
      pull(in)
    }

    override def onUpstreamFinish(): Unit = {
      emit(out, leftover)
      completeStage()
    }
  }
}

/**
 *  Stream CSV data according to requested stream segments and filters
 */
class StreamController @Inject(
  ) (cc: ControllerComponents) extends AbstractController(cc) {

  /**
   * Filter function to filter stream by query parameters
   */
  def myFilter(
    in: List[ByteString],
    query: List[Seq[ByteString]]): Boolean =
  {
    (0 until 2).foldLeft(true) {
      (agg, i) => {
        // exit early once agg false, not sure whether that makes sense
        if (agg == false) { return false }
        agg &&
          (query(i).isEmpty || query(i).foldLeft(false) { _ || in(i+3) == _ })
      }
    }
  }

  /**
   * 1. Normalize query parameters to accept different way of representing
   * lists in urls: ?list=item1,item2 and ?list=item1&list=item2.
   * 2. Set default values if query parameter is empty.
   * 3. Convert to ByteString in accordance with Akka.
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
  ) : Seq[ByteString] = {
    val ret = getValues(key, in)
    val opts = allowed.map(ByteString(_))
    if (ret.isEmpty || ret(0).isEmpty) {
      opts
    } else {
      ret.filter(opts contains _)
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
   * Format CSV output stream
   */
  def formatCsvLine(lst: List[ByteString]): ByteString = {
    lst.reduce(_ ++ ByteString(",") ++ _) ++ ByteString("\n")
  }

  /**
   * A play view that streams CSV data from file to download and applying 
   * filters.
   * TODO: decide in what scope helper functions should be placed.
   */
  def chunkedFromSource() = Action {

    implicit request: Request[AnyContent] =>

    val measurements = getLimitedValues(
      "measurements", request.queryString,
      List("max", "min", "mean", "median"))
    val variables = getLimitedValues(
      "variables", request.queryString, 
      List("estimated", "p10", "p90", "observed"))
    /**
     * Keywords used for filtering, they will be matched by position in list
     */
    val keywords = List("years", "months")

    /**
     * Create list from query parameters and convert to immutable list
    */
    val filterList = getFilterList(request, keywords)

    /**
     * Set query before passing function to flow
     */
    def filterFunction(in: List[ByteString]): Boolean =
      myFilter(in: List[ByteString], filterList)

    val list = getValues("segments", request.queryString)
      .map(_.utf8String)
      .toList

    /**
     *  Checks whether filterList is empty or not
     */
    def partitionFunction(in:List[Seq[ByteString]]):Boolean = {
      in.foldLeft(true) {(acc, i) => acc && i.isEmpty }
    }

    /**
     *  flow generating a stream of rows from a list of comid's by retrieving 
     *  files
     */
    val fileFlow = Flow[String]
      .flatMapConcat({
        comid => Source(measurements.toList.map(in => List(comid, in.utf8String)))
      })
      .flatMapConcat({
        item => Source(variables.toList.map(in => item ++ List(in.utf8String)))
      })
      .flatMapConcat({
        inValue => 
          FileIO.fromPath(
            Paths.get("pdump/", inValue(0),  "/", inValue(1), "/", inValue(2) + ".csv"))
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
     *  Flow that filters by query parameters
     */
    val filterFlow = Flow[ByteString]
      .via(CsvParsing.lineScanner())
      .filter(filterFunction)
      .map(formatCsvLine)

    val source = Source.fromGraph(GraphDSL.create() {
      implicit builder =>
      import GraphDSL.Implicits._

      val source1 = Source(list)
      /**
       * List stream source from index.csv file, when no specific comids are
       * requested.
       * TODO: Test whether DirectoryIO from Alpakka can do this job better
       */
      val source2 = FileIO.fromPath(Paths.get("pdump/index.csv"))
        .via(CsvParsing.lineScanner()).map(_(0).utf8String)
      val merge = builder.add(Merge[ByteString](3))
      val partition = builder.add(Partition[ByteString]
        (2, in => if (partitionFunction(filterList)) 1 else 0))

      /**
       *  Connect graph: source2 only used if list from request is empty
       */
      source1 ~> fileFlow ~> filterFlow ~> merge.in(0)
      source2.filter(_ => list.isEmpty) ~> fileFlow ~> partition.in

      partition.out(0) ~> filterFlow ~> merge.in(1)
      partition.out(1) ~> merge.in(2)

      SourceShape(merge.out)
    })

    /**
     * Add header and sink flow to chunked HTTP response using Play framework
     * TODO: Switch to Akka http eventually
     */
    val header = Source(
      List(ByteString("comid,measurement,variable,year,month,value\n")))
    val csvSource = Source
      .combine(header:Source[ByteString, NotUsed], 
        source:Source[ByteString, NotUsed])(Concat(_))
    Ok.chunked(csvSource) as "text/csv"
  }

}
