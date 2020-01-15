package controllers

// scala
import scala.concurrent.duration.Duration
// scalatest
import org.scalatest.TestData
import org.scalatestplus.play._
import org.scalatestplus.play.guice.GuiceOneAppPerTest
// play
import play.api.Application
import play.api.test.{ FakeRequest, Injecting }
import play.api.libs.Files
import play.api.libs.json.{ JsArray, JsNumber, Json, JsString }
import play.api.test.Helpers._
// akka
import akka.stream.Materializer
import akka.util.ByteString


class FunctionalControllerSpec extends PlaySpec
  with GuiceOneAppPerTest with Injecting {

  "FunctionalController GET /ffm/" should {

    "return ok and content-type text/csv" in {
      implicit lazy val materializer: Materializer = app.materializer
      val controller = new FunctionalController(stubControllerComponents())
      val res = controller.chunkedFromSource()
        .apply(FakeRequest(GET, "/ffm/"))
      status(res) mustBe OK
      contentType(res) mustBe Some("text/csv")
    }

    "be csv" in {
      implicit lazy val materializer: Materializer = app.materializer
      val controller = new FunctionalController(stubControllerComponents())
      val lines = contentAsString(
        controller.chunkedFromSource()
        .apply(FakeRequest(GET,
          "/stream/?comids=10000042,10000688,10000692"))
      ).split("\n")
      lines.length must equal (129)
      lines(0) must be ("comid,ffm,wyt,p10,p25,p50,p75,p90,source")
      lines.foreach((item) => {
        item.split(",").length must equal (9)
      })
      lines.drop(1).foreach((item) => {
        List("moderate", "dry", "wet", "all") must contain (item.split(",")(2))
      })
      lines.drop(1).foreach((item) => {
        List("model", "obs") must contain (item.split(",")(8))
      })
    }

    "filter by comid" in {
      implicit lazy val materializer: Materializer = app.materializer
      val controller = new FunctionalController(stubControllerComponents())
      val lines = contentAsString(
        controller.chunkedFromSource()
          .apply(FakeRequest(GET,
          "/ffm/?comids=10000042"))
        ).split("\n")
      lines must have length 65
      lines.drop(1).foreach((item) => {
        item.split(",")(0) must be ("10000042")
      })
    }

    "request nothing and all" in {
      implicit lazy val materializer: Materializer = app.materializer
      val controller = new FunctionalController(stubControllerComponents())
      var lines = contentAsString(
        controller.chunkedFromSource()
          .apply(FakeRequest(GET, "/ffm/"))
        ).split("\n")
      lines must have length 1
      lines(0) must be ("comid,ffm,wyt,p10,p25,p50,p75,p90,source")
      lines = contentAsString(
        controller.chunkedFromSource()
          .apply(FakeRequest(GET, "/ffm/?comids=0"))
        ).split("\n")
      lines must have length 129
    }

    "filter according schema" in {
      implicit lazy val materializer: Materializer = app.materializer
      val controller = new FunctionalController(stubControllerComponents())
      var lines = contentAsString(
        controller.chunkedFromSource()
         .apply(FakeRequest(GET, "/ffm/?comids=10000042&wyts=dry"))
        ).split("\n")
      lines must have length 17
      lines.drop(1).foreach((item) => {
        item.split(",")(2) must be ("dry")
      })
    }
  }

  "FunctionalController POST /ffm/" should {
     
    "filter according schema" in {
      implicit lazy val materializer: Materializer = app.materializer
      val controller = new FunctionalController(stubControllerComponents())
      val request = FakeRequest(POST, "/ffm/")
        .withFormUrlEncodedBody("comids" -> "10000042", "ffms" -> "ds_dur_ws,fa_tim")
     val lines = contentAsString(
        controller.chunkedFromSource().apply(request)).split("\n")
     lines must have length 9
     lines.drop(1).foreach((item) => {
       item.split(",")(0) must be ("10000042")
       List("ds_dur_ws", "fa_tim") must contain (item.split(",")(1))
     })
    }

  }

}
