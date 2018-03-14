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
 * Very simple endless streaming example for speed testing.
 * Reaches 130 Mb/s on my MacBook Pro
 */
class StreamController @Inject(
  ) (cc: ControllerComponents) extends AbstractController(cc) {

  /**
   * A play view that streams CSV data from file to download
   * and applying filters.
   */
  def chunkedFromSource() = Action {

    implicit request: Request[AnyContent] =>

    val params = request.queryString

    /**
     * Extract query parameters from request for further processing
     */
    def getValues(in: Map[String, Seq[String]], key: String) : Seq[String] = {
      val values = in.get(key)
      values match {
        case Some(values) => values
        case None => Seq()
      }
    }
  
    /** 
     * Normalize query parameters in order to accept both ways to represent
     * lists: ?list=item1,item2 and ?list=item1&list=item2 
     */
    def normalize(in: Seq[String]): Seq[String] = {
      in.foldLeft(Seq[String]()){ _ ++ _.split(",") }
    }

    /**
     * Filter function to filter stream by query parameters
     */
    def myFilter(in: List[ByteString], req: Request[AnyContent]) = {
      val params = req.queryString
      val values = getValues(params, "measurements")
      // normalize(values).foreach(println(_))
      // println(in(0) == ByteString("max"))
      in(0) == ByteString("max")
    }

    /**
     * Set request before passing function to flow
     */
    def filterFunction(in: List[ByteString]) = myFilter(in: List[ByteString], request)

    /**
     * Format CSV output stream
     */
    def formatCsvLine(lst: List[ByteString]): ByteString = { 
      lst.reduce(_ ++ ByteString(",") ++ _) ++ ByteString("\n") 
    }

    val source = FileIO.fromPath(Paths.get("10002070.csv"))
      .via(CsvParsing.lineScanner())
      .filter(filterFunction)
      .map(formatCsvLine)

    Ok.chunked(source).as("text/plain")
  }

}
