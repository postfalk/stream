package controllers

import java.nio.file.{Paths, NoSuchFileException}
import play.api.libs.json.{JsValue, JsNumber, JsString, JsArray, JsNull}
import play.api.mvc.{Request, AnyContent}
import akka.NotUsed
import akka.stream.alpakka.csv.scaladsl.CsvParsing
import akka.stream.scaladsl.{FileIO, Source, Concat}
import akka.util.ByteString

/**
 * A somewhat flimsy trait containing methods shared between the Stream and 
 * the FunctionalControllers
 */
trait APIController {

  def allowedParams: List[String]
  def csvHeaderLine: String
  def dataDirectory: String
  def filenameFromRequest(request: Request[AnyContent]): String
  def sourceFactory(query: Map[String, Seq[String]]): 
    Source[ByteString, NotUsed]

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
   * filterFlow
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
   * Filter CSV stream by column number
   */
  def colFilter(in: List[ByteString], valueList: List[ByteString], col:Int):
    Boolean = valueList.foldLeft(false) {_ || in(col) == _ }

  /**
   * Decide whether there is a valid comid list or whether no limiting list
   * is provided. This prevents incidental download of the entire dataset when
   * API endpoint is requested.
   */
  def comidsListFilter(in: Map[String, Seq[String]]): Boolean =
    if (in.keySet.exists(_ == "comids"))
      in("comids")(0) == "0"
    else
      false

  /**
   * Get comid source from query parameters or file
   *
   * Stream comids requested by query parameters or from index.csv file when
   * comids not provided as parameters
   *
   */
  def getComidsSource(query: Map[String, Seq[String]]): Source[String, Any] = {

    val comidList = getValues("comids", query)
      .map(_.utf8String)
      .toList

    /** Not sure whether that is the most elegant way to switch between
      *  sources but works for now.
      */
    if (comidsListFilter(query))
      FileIO
        .fromPath(Paths.get("pdump/index.csv"))
        .recover({ case _: NoSuchFileException => ByteString() } )
        .filterNot({_ == ByteString()})
        .via(CsvParsing.lineScanner())        
        .map(_(0).utf8String)
    else
      Source(comidList)
  }

  /**
   * Format CSV output stream
   */
  def formatCsvLine(lst: List[ByteString]): ByteString =
    lst.reduce(_ ++ ByteString(",") ++ _) ++ ByteString("\n")

  def anyJsonTypeToList(in: JsValue): List[String] =
    in match {
      case _:JsNumber => { List(in.toString) }
      case _:JsString => { List(in.as[String]) }
      case _:JsArray => { 
        in.as[JsArray].value.map(anyJsonTypeToList _)
          .toList.map(_.head)
      }
      case _ => { List() }
    }

  def extractQueryFromJson(json: Option[JsValue], allowed: List[String]):
    Option[Map[String, Seq[String]]] =
      json match {
        case None => None: Option[Map[String, Seq[String]]]
        case Some(json) => {
          val tuples = allowed.map(
            (item) => { (item, anyJsonTypeToList((json \ item)
              .asOpt[JsValue].getOrElse(JsNull))) })
          val map = tuples.toMap
          Some(map)
        }
      }

  /**
    * Check for POST data, use URL query parameters if not available
    */
  def getQuery(request: Request[AnyContent]):Map[String, Seq[String]] =
    extractQueryFromJson(request.body.asJson, allowedParams)
      .getOrElse(
        request.body.asFormUrlEncoded.getOrElse(request.queryString))

  /**
    * Add CSV header and create source from flow
    */
  def csvSourceFactory(request: Request[AnyContent]):
      Source[ByteString, NotUsed] = {
    val query = getQuery(request)
    val header = Source(List(ByteString(csvHeaderLine)))
    val source = sourceFactory(query)
    Source
      .combine(
        header:Source[ByteString, NotUsed],
        source:Source[ByteString, NotUsed])(Concat(_))
  }

}
