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
      lines(0) must be ("comid,ffm,wyt,p10,p25,p50,p75,p90,unit,source")
      lines.foreach((item) => {
        item.split(",").length must equal (10)
      })
      lines.drop(1).foreach((item) => {
        List("moderate", "dry", "wet", "all") must contain (item.split(",")(2))
      })
      lines.drop(1).foreach((item) => {
        List("model", "obs") must contain (item.split(",")(9))
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
      lines(0) must be ("comid,ffm,wyt,p10,p25,p50,p75,p90,unit,source")
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

    "have data for all the ffm values" in {
      implicit lazy val materializer: Materializer = app.materializer

      def runTest(tuple:(String, Int)): Unit = {

        val controller = new FunctionalController(
          stubControllerComponents())
        val request = FakeRequest(POST, "/ffm/").withFormUrlEncodedBody(
          "comids" -> "12068176", "ffms" -> tuple._1)
        val lines = contentAsString(
          controller.chunkedFromSource()
            .apply(request))
              .split("\n")
              .drop(1)
          lines must have length tuple._2
      }

      // query all ffms
      val tests = List(
        ("", 70), ("ds_dur_ws", 4), ("ds_mag_50", 4), ("ds_mag_90", 4), 
        ("ds_tim", 4), ("fa_mag", 4), ("fa_tim", 4), ("peak_10", 4),
        ("peak_2", 4), ("peak_5", 4), ("peak_dur_10", 1), ("peak_dur_2", 1),
        ("peak_dur_5", 1), ("peak_fre_10", 1), ("peak_fre_2", 1), 
        ("peak_fre_5", 1), ("sp_dur", 4), ("sp_mag", 4), ("sp_tim", 4),
        ("wet_bfl_dur", 4), ("wet_bfl_mag_10", 4), ("wet_bfl_mag_50", 4),
        ("wet_tim", 4))

      // make sure sum of all query results equals number of fields
      tests.drop(1).map(t => t._2).foldLeft(0)(_ + _) must be (70)

    }
  }
}
