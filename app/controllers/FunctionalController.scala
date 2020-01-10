package controllers

import akka.NotUsed

import javax.inject._
import play.api._
import play.api.mvc._

import akka.stream.scaladsl.{
  FileIO, Source, Sink, GraphDSL, Merge, Flow, Partition, Concat}
import akka.util.ByteString

/**
 * This controller serves functional flow data
 */

class FunctionalController @Inject() (
  cc: ControllerComponents) extends AbstractController(cc) with APIController {

  val allowedParams = List()
  val csvHeaderLine = "comid,ffm,wyt,p10,p25,p50,p75,p90,source\n"
  val dataDirectory = "pdump/ffm/"
  def filenameFromRequest(request: Request[AnyContent]): String = {"ffm.csv"}
  def sourceFactory(query: Map[String, Seq[String]]): Source[ByteString, NotUsed] = {Source(List(ByteString("test")))}

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
