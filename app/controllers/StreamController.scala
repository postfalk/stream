package controllers

import javax.inject._
import java.nio.file.{Files, Paths}
import scala.collection.immutable.Range

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
    implicit val materializer = ActorMaterializer()

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
     * Create list from query parameters and convert to immutable list
     */
    val list = getValues("segments", request.queryString)
      .map(_.utf8String)
      .toList

    /**
     *  construct the source
     */
    val source = Source(list)
      .flatMapConcat{
        comid =>
        FileIO.fromPath(Paths.get(comid + ".csv"))
          /** Recover catches error if file does not exist and passes an empty
           *  ByteString instead down the processing pipeline. It would be 
           *  nicer to catch a more specific error. See discussion:
           *  https://github.com/akka/akka/issues/24512
           */
          .recover({ case _: IllegalArgumentException => ByteString() })
          .via(CsvParsing.lineScanner())
          .map(List(ByteString(comid)) ++ _)
      }
      .filter(filterFunction)
      .map(formatCsvLine)

    /**
     * Sink flow to chunked HTTP response using Play framework
     * TODO: Switch to Akka http eventually
     */
    Ok.chunked(source) as "text/plain"
  }

}
