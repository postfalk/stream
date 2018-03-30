package controllers

import javax.inject._
import java.nio.file.{Files, Paths, NoSuchFileException}

import scala.collection.immutable.Range
import scala.concurrent.Await
import scala.concurrent.duration.Duration

import play.api._
import play.api.mvc._
import play.api.http._
import play.api.libs.iteratee._

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream._
import akka.stream.stage._
import akka.stream.scaladsl._
import akka.util._

import akka.stream.alpakka.csv.scaladsl.{CsvParsing, CsvToMap}
import akka.stream.alpakka.s3.scaladsl._

/**
 * Stream CSV data according to requested stream segments and filters
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
    (0 until 4).foldLeft(true) {
      (agg, i) => {
        // exit early once agg false, not sure whether that makes sense
        if (agg == false) { return false }
        agg &&
          (query(i).isEmpty || query(i).foldLeft(false) { _ || in(i+1) == _ })
      }
    }
  }

  /**
   * 1. Normalize query parameters to accept different ways to represent
   * lists in urls: ?list=item1,item2 and ?list=item1&list=item2
   * 2. Set default values if query parameter is empty
   * 3. Convert to ByteString in accordance with Akka
   */
  def normalize(in: Seq[String]): Seq[ByteString] = {
    in.foldLeft(List[ByteString]()) { _ ++ _.split(",").map(ByteString(_)) }
  }

  /**
   * Extract query parameters from requests by keyword for further processing
   */
  def getValues(
    key: String,
    in: Map[String, Seq[String]]
  ) : Seq[ByteString] = {
    val values = in.get(key)
    values match {
      case Some(values) => normalize(values)
      case None => List()
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
    implicit val system = ActorSystem("Test")
    implicit val materializer = ActorMaterializer(
      ActorMaterializerSettings(system)
        .withInputBuffer(initialSize=1, maxSize=1)
        .withOutputBurstLimit(1)
        .withSyncProcessingLimit(8)
    )

    /**
     * S3 source seems to work, not yet connected to anything
     */
    /* val s3Client = S3Client()
    val (s3Source: Source[ByteString, NotUsed], metaData) = s3Client
      .download("unimpaired", "100000042.csv")
    */


    // s3Source.to(Sink.foreach(println(_))).run()
    /* println(Await.result(metaData, Duration("5 seconds")).contentLength) */

    /* FileIO.fromPath(Paths.get("dump/1000042.csv"))
      .recover({ case _: IllegalArgumentException => ByteString() })
      .to(Sink.foreach(println("here", _))).run() */

    /**
     * Keywords used for filtering, they will be matched by position in list
     */
    val keywords = List(
      "measurements", "variables", "years", "months")

    val filterList = getFilterList(request, keywords)

    /**
     * Set query before passing function to flow
     */
    def filterFunction(in: List[ByteString]): Boolean =
      myFilter(in: List[ByteString], filterList)

    /**
     *  construct the source
     */
    val flow = Flow[String]
      .flatMapConcat({
        comid => 
          /* val (s3Source: Source[ByteString, NotUsed], _) = s3Client
            .download("unimpaired", comid + ".csv") */
          FileIO.fromPath(Paths.get("dump/" + comid + ".csv"))
          /** Recover catches file-does-not-exist errors and passes an empty
           *  ByteString to downstream stages instead.
           */
            .recover({ case _: NoSuchFileException => ByteString()})
          /* s3Source */
            .via(CsvParsing.lineScanner())  // this step makes it very slow
            .map(List(ByteString(comid)) ++ _)
            .filter(filterFunction)
            .map(formatCsvLine)
            .fold(ByteString())(_ ++ _)
        })

    val source = Source.fromGraph(GraphDSL.create() {
      implicit builder =>
      import GraphDSL.Implicits._

      /**
       * Create list from query parameters and convert to immutable list
       */
      val list = getValues("segments", request.queryString)
        .map(_.utf8String)
        .toList

      val in1 = Source(list)
      /**
       * List stream source from index.csv file, when no specific files are
       * requested.
       * TODO: Test whether DirectoryIO from Alpakka can do this job
       * effectively.
       */
      val in2 = FileIO.fromPath(Paths.get("dump/index.csv"))
        .via(CsvParsing.lineScanner()).map(_(0).utf8String)
      val merge = builder.add(Merge[ByteString](2))
      // val bcast = builder.add(Broadcast[String](2)) 

      /**
       *  Connect graph: in2 only used if list from request is empty
       */
      in1 ~> flow ~> merge.in(0)
      in2.filter(_ => list.isEmpty) ~> flow ~> merge.in(1)

      SourceShape(merge.out)
    })

    /* val experimentalSource = FileIO.fromPath(Paths.get("dump/index.csv"))
        .via(CsvParsing.lineScanner()).map(_(0).utf8String)
        .flatMapConcat(comid => {
          FileIO.fromPath(Paths.get("dump/" + comid + ".csv"))
            .recover({ case _: NoSuchFileException => ByteString() })
            .via(Framing.delimiter(ByteString("\n"), 256, allowTruncation=true))
            .map(ByteString(comid + ",") ++ _ ++ ByteString("\n"))
        })
        // .filter(filterFunction)
        // .map(formatCsvLine) */

    /**
     * Sink flow to chunked HTTP response using Play framework
     * TODO: Switch to Akka http eventually
     */
    Ok.chunked(source) as "text/csv"
  }

}
