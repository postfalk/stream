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

  "StreamController GET /stream/" should {

    "return Ok and content-type text/csv" in {
      implicit lazy val materializer: Materializer = app.materializer
      val controller = new StreamController(stubControllerComponents())
      val res = controller.chunkedFromSource()
        .apply(FakeRequest(GET, "/stream/"))
      status(res) mustBe OK
      contentType(res) mustBe Some("text/csv")
    }

    "be csv" in {
      implicit lazy val materializer: Materializer = app.materializer
      val controller = new StreamController(stubControllerComponents())
      val lines = contentAsString(
        controller.chunkedFromSource()
        .apply(FakeRequest(GET,
          "/stream/?comids=10000042,10000688,10000692"))
      ).split("\n")
      lines.length must equal (28513)
      lines(0) must be ("comid,statistic,variable,year,month,value")
      lines.foreach((item) => {
        item.split(",").length must equal (6)
      })
    }

    "filter according to schema" in {
      implicit lazy val materializer: Materializer = app.materializer
      val controller = new StreamController(stubControllerComponents())
      val lines1 = contentAsString(
        controller.chunkedFromSource()
        .apply(FakeRequest(GET,
          "/stream/?comids=10000042&statistics=min,max"))
      ).split("\n")
      lines1 must have length 4753
      lines1.tail.foreach((item) => {
        Some(item.split(",")(1)) must contain oneOf ("min", "max")
      })
      val lines2 = contentAsString(
        controller.chunkedFromSource()
        .apply(FakeRequest(GET,
          "/stream/?comids=10000042&variables=estimated"))
      ).split("\n")
      lines2 must have length 3169
      lines2.tail.foreach(item => {item.split(",")(2) must be ("estimated")})
      val lines3 = contentAsString(
        controller.chunkedFromSource()
        .apply(FakeRequest(GET,
          "/stream/?comids=10000042&months=1,5"))
      ).split("\n")
      lines3 must have length 1585
      lines3.tail.foreach((item) => {
         Some(item.split(",")(4)) must contain oneOf ("1", "5")
      })
      val lines4 = contentAsString(
        controller.chunkedFromSource()
        .apply(FakeRequest(GET,
          "/stream/?comids=10000042&months=1&begin_year=1980&end_year=1982"))
      ).split("\n")
      lines4.length must equal (37)
      lines4.tail.foreach((item) => {
        Some(item.split(",")(3)) must contain oneOf ("1980", "1981", "1982")
        item.split(",")(4) must be ("1")
      })
      val lines5 = contentAsString(
        controller.chunkedFromSource()
        .apply(FakeRequest(GET,
          "/stream/?comids=10000042&months=1&begin_year=2018"))
      ).split("\n")
      lines5 must have length 1
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

  "StreamController scanRequestBody" should {
    "attempt to scan the body in" {
      val controller = new StreamController(stubControllerComponents())
      val body = """{"comids": [10000042]}"""
      val empty = FakeRequest("POST", "/stream", FakeHeaders(), body)
    }
  }

}
