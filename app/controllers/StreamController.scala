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
 * Simple streaming example  
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
          (query(i).isEmpty || query(i).foldLeft(false) { _ || in(i) == _ })
      }
    }
  }

  /** 
   * 1. Normalize query parameters in order to accept both ways to represent
   * lists in urls: ?list=item1,item2 and ?list=item1&list=item2
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
   * A play view that streams CSV data from file to download
   * and applying filters.
   */
  def chunkedFromSource() = Action {

    implicit request: Request[AnyContent] =>
    implicit val system = ActorSystem("Test")
    implicit val materializer = ActorMaterializer() 

    /**
     * Keywords that can be used for filtering, they will be matched by 
     * position
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
     * Build flow as source
     */
    val source = FileIO.fromPath(Paths.get("10002070.csv"))
      .via(CsvParsing.lineScanner())
      .filter(filterFunction)
      .map(formatCsvLine)

    val src = GraphDSL.create() { 
      implicit builder: GraphDSL.Builder[NotUsed] => 

        import GraphDSL.Implicits._
        
        val sink = Sink.foreach[ByteString](println(_))
        val f = Flow[ByteString].map((item) => { item  })
        val stream = source ~> f
    
      SourceShape(stream.outlet)
    }

    val new_source = Source.fromGraph(src)

    /**
     * Sink flow to chunked HTTP response
     */
    Ok.chunked(new_source).as("text/plain")
  }

}
