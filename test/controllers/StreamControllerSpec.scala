package controllers

import scala.concurrent.duration.Duration

import play.api.Application
import org.scalatest.TestData

import org.scalatestplus.play._
import org.scalatestplus.play.guice._
import play.api.test._
import play.api.libs.Files
import play.api.libs.json._
import play.api.test.Helpers._
import play.api.mvc._

import scala.concurrent._
import akka.stream.Materializer
import akka.util._

import test.fakeApp

class StreamControllerSpec extends PlaySpec
  with GuiceOneAppPerTest with Injecting with Results
{

  override def newAppForTest(testData: TestData): Application = fakeApp

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

    "be also csv using GET with router" in {
      implicit lazy val materializer: Materializer = app.materializer      
      val request = FakeRequest(GET, "/stream/")
      val stream = route(app, request).get
      status(stream) mustBe OK
      contentType(stream) mustBe Some("text/csv")
      contentAsString(stream) must include(
        "comid,statistic,variable,year,month,value")
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

 "StreamController POST /stream/" should {

    "return Ok and content-type text/csv" in {
      implicit lazy val materializer: Materializer = app.materializer
      val controller = new StreamController(stubControllerComponents())
      val res = controller.chunkedFromSource()
        .apply(FakeRequest(POST, "/stream/"))
      status(res) mustBe OK
      contentType(res) mustBe Some("text/csv")
    }

    "be csv" in {
      implicit lazy val materializer: Materializer = app.materializer
      val controller = new StreamController(stubControllerComponents())
      val request = FakeRequest(POST, "/stream/")
        .withFormUrlEncodedBody("comids" -> "10000042,10000688")
      val lines = contentAsString(
        controller.chunkedFromSource().apply(request)).split("\n")
      lines.length must equal (19009)
      lines(0) must be ("comid,statistic,variable,year,month,value")
      lines.foreach((item) => {
        item.split(",").length must equal (6)
      })
    }

    "be also csv using router" in {
      implicit lazy val materializer: Materializer = app.materializer
      val request = FakeRequest(POST, "/stream/")
      val stream = route(app, request).get
      status(stream) mustBe OK
      contentType(stream) mustBe Some("text/csv")
      contentAsString(stream) must include(
        "comid,statistic,variable,year,month,value")
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

  "StreamController filenameFromQuery" should {
    "return a filename" in {
      val controller = new StreamController(stubControllerComponents())
      val query = Map[String, Seq[String]]()
      controller.filenameFromQuery(query) must be ("flow.csv")
      val longComidQuery = Map("comids" -> 
        Seq("1042, 1688, 1002, 2003, 3002, 1222"))
      controller.filenameFromQuery(longComidQuery)
        .must(be(("flow_1042_1688_1002_2003_3002.csv")))
      val shortComidQuery = Map("comids" -> Seq("1042, 1688"))
      controller.filenameFromQuery(shortComidQuery)
        .must(be(("flow_1042_1688.csv")))
      val complexQuery = Map("comids" -> Seq("1042"), 
        "statistics" -> Seq("min,max"), "variables" -> Seq("estimated"),
        "begin_year" -> Seq("1980,2000"), "end_year" -> Seq("1980"),
        "months" -> Seq("1","2"))
      controller.filenameFromQuery(complexQuery)
        .must(be(("flow_1042_min_max_estimated_1980_1980_1_2.csv")))
    }
  }

  "StreamController normalize" should {
    "clean queries" in {
      val controller = new StreamController(stubControllerComponents())
      val query = Seq("min", "max", "mean, median")
      val expected = List("min", "max", "mean", "median").map(ByteString(_))
      controller.normalize(query) must be (expected)
    }
  }

  "StreamController getValues" should {
    "return values from key" in {
      val controller = new StreamController(stubControllerComponents())
      val query = Map(
        "comids" -> Seq("19992,29901"),
        "variables" -> Seq("p10", "p90"))
        "begin_year" -> Seq("1990")
      controller.getValues("comids", query)
        .must(be((List("19992", "29901").map(ByteString(_)))))
      controller.getValues("variables", query)
        .must(be((List("p10", "p90").map(ByteString(_)))))
      controller.getValues("unsinn", query) must be (List())
    }
  }

  "StreamController getLimitedValues" should {
    "only return values in list" in {
      val controller = new StreamController(stubControllerComponents())
      val query = Map(
        "variables" -> Seq("min", "max", "frogs")
      )
      controller.getLimitedValues("variables", query, 
        List("min", "max", "mean"))
          .must(be((List("min", "max").map(ByteString(_)))))
    }
  }

  "StreamController formatCsvLine" should {
    "create a CSV line from a list of ByteString" in {
      val controller = new StreamController(stubControllerComponents())
      val data = List("1000", "max", "estimated", "1900", "2", "3.0")
        .map(ByteString(_))
      controller.formatCsvLine(data)
        .must(be((ByteString("1000,max,estimated,1900,2,3.0\n"))))
    }
  }

  "StreamController extractQueryFromJson" should {
    "create a query compatible with the rest of app" in {
      val controller = new StreamController(stubControllerComponents())
      val emptyRequest = FakeRequest()
      controller.extractQueryFromJson(emptyRequest.body.asJson, 
        List("statistics")) must be (None)
      val wrongRequest = FakeRequest()
        .withFormUrlEncodedBody("statistics" -> "min, max")
      controller.extractQueryFromJson(wrongRequest.body.asJson,
        List("statistcis")) must be (None)
      val request = FakeRequest().withJsonBody(
        Json.parse(
          """{"statistics": ["min", "max"], "variables": ["p10"]}"""))
      val result = Option(Map("statistics" -> Seq("min", "max"), 
        "variables" -> Seq("p10")))
      controller.extractQueryFromJson(request.body.asJson,
        List("statistics", "variables"))
        .must(be((result)))
    }
  }

  "StreamController anyJsonTypeToList" should {
    "convert JsValues" in {
      val controller = new StreamController(stubControllerComponents())
      controller.anyJsonTypeToList(JsNumber(2)) must be (List("2"))
      controller.anyJsonTypeToList(JsString("2")) must be (List("2"))
      controller.anyJsonTypeToList(
        JsArray(IndexedSeq(JsNumber(2), JsNumber(3))))
        .must(be((List("2", "3"))))
      controller
        .anyJsonTypeToList(JsArray(IndexedSeq(JsString("2"), JsString("3"))))
    }
  }

}
