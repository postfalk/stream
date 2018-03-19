package controllers

import javax.inject._
import java.nio.file.{Files, Paths} 

import play.api._
import play.api.mvc._
import play.api.http._
import play.api.libs.iteratee._

import akka.stream._
import akka.stream.stage._
import akka.stream.scaladsl._
import akka.util._

import akka.stream.alpakka.csv.scaladsl.{CsvParsing, CsvToMap}

/**
 * Simple streaming example  
 */
class StreamController @Inject(
  ) (cc: ControllerComponents) extends AbstractController(cc) {

  /**
   * A play view that streams CSV data from file to download
   * and applying filters.
   */
  def chunkedFromSource() = Action {

    implicit request: Request[AnyContent] =>

    /**
     * Keywords that can be used for filtering
     */
    val keywords = List(
      "measurements", "variables", "years", "months")

    /** 
     * 1. Normalize query parameters in order to accept both ways to represent
     * lists: ?list=item1,item2 and ?list=item1&list=item2
     * 2. Set default values if query parameter is empty
     * 3. Convert to ByteString in accordance with Akka
     */
    def normalize(in: Seq[String]): Seq[ByteString] = {
      in.foldLeft(Seq[ByteString]()) { _ ++ _.split(",").map(ByteString(_)) }
    }

    /**
     * Extract query parameters from request by keyword for further processing
     */
    def getValues(key: String, in: Map[String, Seq[String]]) : Seq[ByteString] = {
      val values = in.get(key)
      values match {
        case Some(values) => normalize(values)
        case None => Seq()
      }
    }

    /**
     * Create a list of query values to be applied by the filter function.
     * They are currently mapped by position in the list
     */
    def getFilterList(
      in: Request[AnyContent],
      keys: List[String] = keywords): List[Seq[ByteString]] = {
        keys.map(getValues(_, in.queryString))
    }

    val filterList = getFilterList(request, keywords)

    /**
     * Filter function to filter stream by query parameters
     */
    def myFilter(
      in: List[ByteString], 
      query: List[Seq[ByteString]]): Boolean = 
    { 
      (query(0).isEmpty || query(0).foldLeft(false) { _  || in(0) == _}) &&
      (query(1).isEmpty || query(1).foldLeft(false) { _  || in(1) == _}) &&
      (query(2).isEmpty || query(2).foldLeft(false) { _  || in(2) == _}) &&
      (query(3).isEmpty || query(3).foldLeft(false) { _  || in(3) == _})
    }

    /**
     * Set request before passing function to flow
     */
    def filterFunction(in: List[ByteString]): Boolean = 
      myFilter(in: List[ByteString], filterList)

    /**
     * Format CSV output stream
     */
    def formatCsvLine(lst: List[ByteString]): ByteString = { 
      lst.reduce(_ ++ ByteString(",") ++ _) ++ ByteString("\n") 
    }

    /**
     * Build flow as source
     */
    val source = FileIO.fromPath(Paths.get("10002070.csv"))
      .via(CsvParsing.lineScanner())
      .filter(filterFunction)
      .map(formatCsvLine)

    /**
     * Sink flow to chunked HTTP response
     */
    Ok.chunked(source).as("text/plain")
  }

}
