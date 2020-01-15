package controllers

import akka.NotUsed

import java.nio.file.{Paths, NoSuchFileException}
import javax.inject._
import play.api._
import play.api.mvc._

import akka.stream.scaladsl.{
  FileIO, Source, Sink, GraphDSL, Merge, Flow, Partition, Concat}
import akka.stream._
import akka.stream.stage.GraphStage
import akka.util.ByteString

import akka.stream.alpakka.csv.scaladsl.CsvParsing

/**
 * This controller serves functional flow data
 */

class FunctionalController @Inject() (
  cc: ControllerComponents) extends AbstractController(cc) with APIController {

  val allowedParams = List("comids", "ffms", "wyts", "sources")
  val csvHeaderLine = "comid,ffm,wyt,p10,p25,p50,p75,p90,source\n"
  val dataDirectory = "pdump/ffm/"
  val registeredFfms = List("ds_dur_ws", "ds_mag_50", "ds_mag_90", "ds_tim",
    "fa_mag", "fa_tim", "peak_10", "peak_20", "peak_50", "sp_dur", "sp_mag",
    "sp_tim", "wet_bfl_dur", "wet_bfl_mag_10", "wet_bfl_mag_50", "wet_tim")
  val registeredWyts = List("all", "wet", "dry", "moderate")
  val registeredSources = List("model", "obs")

  def filenameFromRequest(request: Request[AnyContent]): String = {
    val query = getQuery(request)
    val comids = getValues("comids", query).take(5)
    val ffms = getValues("ffms", query)
    val wyts = getValues("wyts", query)
    val sources = getValues("sources", query)
    val partList = List(ByteString("ffm")) ++ comids ++ ffms ++ wyts ++ 
      sources
    partList.map(_.utf8String.trim()).mkString("_") ++ ".csv"
  }

  /**
   * Creating a streamed source from query parameters
   */
  def sourceFactory(query: Map[String, Seq[String]]): 
    Source[ByteString, NotUsed] = {

      val ffms = getLimitedValues("ffms", query, registeredFfms)
      val wyts = getLimitedValues("wyts", query, registeredWyts)
      val sources = getLimitedValues("sources", query, registeredSources)

      Source.fromGraph(GraphDSL.create() {
        implicit builder => import GraphDSL.Implicits._

        val source = getComidsSource(query)

        val filterSwitch = getSwitch(query, List("ffms", "wyts", "sources"))

        val fileFlow = Flow[String]
          .flatMapConcat({
            comid =>
              FileIO.fromPath(
                Paths.get(dataDirectory, comid + ".csv"))
              .recover({ case _: NoSuchFileException => ByteString() } )
              .filterNot({_ == ByteString()})
          })

        val filterFlow = Flow[ByteString]
          .via(CsvParsing.lineScanner())
          .filter(colFilter(_, ffms, 1))
          .filter(colFilter(_, wyts, 2))
          .filter(colFilter(_, sources, 8))
          .map(formatCsvLine)

        val partition = builder.add(
          Partition[ByteString](2, in => if (filterSwitch) 0 else 1))

        val merge = builder.add(Merge[ByteString](2))

        // this is the actual flow graph
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
