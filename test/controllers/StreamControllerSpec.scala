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
      contentType(res) mustBe Some("text/csv")
    }

    "be csv" in {
      val controller = new StreamController(stubControllerComponents())
      val res = controller.chunkedFromSource()
        .apply(FakeRequest(GET, "/stream/?segments=10000042,10000688,10000692"))
      val content = contentAsString(res)
      val lines = content.split("\n")
      lines.length must equal (28513)
      lines.foreach((item) => { 
        item.split(",").length must equal (6)
      })
    }
  }

  "StreamController monthsFilter" should {
    "filter by months" in {
      val controller = new StreamController(stubControllerComponents())
      val exampleData = List("123", "max", "estimated", "1950", "3", "2.5")
        .map(ByteString(_))
      val exampleList = List("3").map(ByteString(_))
      controller.monthsFilter(exampleData, exampleList) must be (true)
      val anotherList = List("4").map(ByteString(_))
      controller.monthsFilter(exampleData, anotherList) must be (false)
      val multiList = List("3", "4").map(ByteString(_))
      controller.monthsFilter(exampleData, multiList) must be (true)
      val fullList = (0 until 12).map(_.toString).toList.map(ByteString(_))
      controller.monthsFilter(exampleData, fullList) must be (true)
    }
  }

  "StreamController yearsFilter" should {
    "filter by years" in {
      val controller = new StreamController(stubControllerComponents())
      val exampleData = List("123", "max", "estimated", "1950", "3", "2.5")
        .map(ByteString(_))
      val example2Data = List("124", "max", "estimated", "1980", "4", "3.9")
        .map(ByteString(_))
      controller.yearsFilter(exampleData, "1950", "1980") must be (true)
      controller.yearsFilter(example2Data, "1950", "2015") must be (true)
      controller.yearsFilter(example2Data, "1950", "1960") must be (false)
    }
  }

  "StreamController getYearOrDefault" should {
    "return a year from a request" in {
      val controller = new StreamController(stubControllerComponents())
      val emptyQuery = FakeRequest("GET", "/stream/").queryString
      controller.getYearOrDefault("begin_year", emptyQuery, "1950")
        .must(be("1950"))
      val someQuery = FakeRequest("GET", "/stream/?end_year=1980").queryString
      controller.getYearOrDefault("end_year", someQuery, "1950")
        .must(be("1980"))
    }
  }

  "StreamController getSwitch" should {
    "check whether any key of a list is in queryString" in {
      val controller = new StreamController(stubControllerComponents())
      val emptyQuery = FakeRequest("GET", "/stream/").queryString
      val lst = List("test", "fest")
      controller.getSwitch(emptyQuery, lst) must be (false)
      val notEmptyQuery = FakeRequest("GET", "/stream/?test=fun").queryString
      controller.getSwitch(notEmptyQuery, lst) must be (true)
    }
  }

}
