package controllers

import javax.inject._
import java.nio.file.{Paths, Files} 

import play.api._
import play.api.mvc._
import play.api.http._
import akka.stream.scaladsl._
import akka.util._

/**
* Very simple endless streaming example for speed testing.
* Reaches 130 Mb/s on my MacBook Pro
*/

class StreamController @Inject(
  ) (cc: ControllerComponents) extends AbstractController(cc) {

  def chunkedFromSource() = Action { 
    val source = FileIO.fromPath(Paths.get("10002070.csv"))
    // val source = Source.repeat("t" * 1024 )
    Ok.chunked(source).as("text/csv")
  }
}
