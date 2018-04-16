import org.scalatestplus.play.PlaySpec

import akka.stream.scaladsl.{Flow, Sink, Source}
import akka.util.ByteString
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer

import flows.MyCSVStage

class MyCSVStageSpec extends PlaySpec {

  implicit val system = ActorSystem("test")
  implicit val materializer = ActorMaterializer()

  // mock chunked ByteString as returned from FileIO
  val example = {"1,min,estimated,1980,1,6\n" ++
    "2,min,estimated,1980,2,5\n" ++
    "3,min,estimated,1980,3,3.4\n" ++ 
    "4,min,estimated,1980,3,7.8\n" ++ 
    "5,min,estimated,1980,3,.2\n" ++ 
    "6,min,estimated,1980,3,9"
  }.grouped(70).toList.map(ByteString(_))

  // validate returned chunk
  def testFunction(in: ByteString): Boolean = {
    val Pattern = {"""^\d{1,9},(min|max|median|mean),""" +
      """(estimated|p10|p90|observed),\d{4},\d{1,2},[\d\.]{1,}$"""}.r
    val lines = in.utf8String.split("\n")
    lines.foldLeft(true) {
      (a, b) => {
        a && (b.matches(Pattern.toString) || b.length == 0)
      }
    }
  }

  "MyCSVStage" must {
    "chop CSV in orderly chunks" in {
      val flow = Flow.fromGraph(new MyCSVStage())
      val source = Source(example)
      val res = source
        .via(flow)
        .to(Sink.foreach((item) => testFunction(item) must be (true)))
        .run()
    }
  }

}
