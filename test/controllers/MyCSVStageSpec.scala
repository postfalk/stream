import org.scalatestplus.play.PlaySpec

import akka.stream.scaladsl.{Flow, Sink, Source}
import akka.util.ByteString
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer

import controllers.MyCSVStage


class MyCSVStageSpec extends PlaySpec {

  implicit val system = ActorSystem("test")
  implicit val materializer = ActorMaterializer()

  // mock chunked ByteString as returned from FileIO
  val example = {"1,min,estimated,1980,1,6\n" ++
    "2,min,estimated,1980,2,5\n" ++
    "3,min,estimated,1980,3,3.4\n" ++ 
    "4,min,estimated,1980,3,7.8\n" ++ 
    "5,min,estimated,1980,3,.2\n" ++ 
    "6,min,estimated,1980,3,9\n"
  }
     .grouped(70).toList.map(ByteString(_))

  "MyCSVStage" must {
    "chop CSV in orderly chunks" in {
      val flow = Flow.fromGraph(new MyCSVStage())
      val source = Source(example)
      source.via(flow).to(Sink.foreach((s) => println(s.utf8String))).run()
    }
  }

}
