package controllers

import scala.concurrent.duration.Duration

import org.scalatestplus.play._
import org.scalatestplus.play.guice._
import play.api.test._
import play.api.test.Helpers._
import play.api.mvc._

import scala.concurrent._
import akka.stream.Materializer
import akka.util._


class StreamControllerSpec extends PlaySpec with GuiceOneAppPerTest with Injecting with Results {

  implicit lazy val materializer: Materializer = app.materializer

  "StreamController GET /stream/" should {

    "return Ok and text/plain" in {
      val controller = new StreamController(stubControllerComponents())
      val res = controller.chunkedFromSource()
        .apply(FakeRequest(GET, "/stream/"))
      status(res) mustBe OK
      contentType(res) mustBe Some("text/plain")
    }

    "should be csv" in {
      val controller = new StreamController(stubControllerComponents())
      val res = controller.chunkedFromSource()
        .apply(FakeRequest(GET, "/stream/"))
      val content = contentAsString(res)
      val lines = content.split("\n")
      lines.length must equal (19008)
      lines.foreach((item) => { 
        item.split(",").length must equal (6)
      })
    }
  }

  "StreamController myFilter" should {
    "filter properly" in {
      val controller = new StreamController(stubControllerComponents())
      val query = List(
        Seq(ByteString("min")), 
        Seq(ByteString("estimated")), 
        Seq[ByteString](),
        Seq[ByteString]())
      var in = "1000,min,estimated,1980,2,1.0"
        .split(",").map(ByteString(_)).toList
      controller.myFilter(in, query) must be (true)
      in = "1000,max,estimated,1980,2,1.0"
        .split(",").map(ByteString(_)).toList
      controller.myFilter(in, query) must be (false)
      }

  }
}
