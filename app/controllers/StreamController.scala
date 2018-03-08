package controllers

import javax.inject._
import play.api._
import play.api.mvc._
import play.api.http._
import akka.stream.scaladsl._
import akka.util._


/**
 * Very simple endless streaming example for speed testing
 */

class StreamController @Inject()(cc: ControllerComponents) extends AbstractController(cc) {

  def chunkedFromSource() = Action { 
    val source = Source.repeat("t" * 1024 )
    Ok.chunked(source).as("text/csv")
  }
}
