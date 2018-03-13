package controllers

import javax.inject._
import java.nio.file.{Files, Paths} 
import scala.concurrent.{Await, Future}
import scala.concurrent.duration.Duration

import play.api._
import play.api.mvc._
import play.api.http._
import play.api.libs.iteratee._

import akka.stream._
import akka.stream.stage._

import akka.stream.scaladsl._
import akka.util._
import akka.actor.ActorSystem

import akka.stream.alpakka.csv.scaladsl.{CsvParsing, CsvToMap}

/**
* Very simple endless streaming example for speed testing.
* Reaches 130 Mb/s on my MacBook Pro
*/

class StreamController @Inject(
  ) (cc: ControllerComponents) extends AbstractController(cc) {

  def chunkedFromSource() = Action { 

    implicit val system = ActorSystem("Stream")
    implicit val materializer = ActorMaterializer()

    // def parseline[List[ByteString]] = (In) immutable.Seq[Out]) {In}

    val flow = Sink.fold[ByteString, ByteString](ByteString(""))(_ ++ _)

    val source = FileIO.fromPath(Paths.get("10002070.csv"))
      .via(CsvParsing.lineScanner()).mapConcat(identity) //.mapConcat(identity)


      // .fold[List[ByteString]](List())(_ ++ _)


      //.via(CsvToMap.withHeaders(
      //  "measurement", "variable", "year", "month", "value"))


    source.runWith(Sink.foreach(println(_)))

    // val enumerator() : Enumerator[String] = Enumerator.enumerate(source)
    // val sink = Sink.ignore //foreach[String](println(_))
    // val runnable: RunnableGraph[None] = source.toMat(sink)(Keep.right)
    // val print: Future[None] = runnable.run() */
    // Ok.chunked(source).as("text/csv") */
    // implicit val system = ActorSystem("Stream")
    // implicit val materializer = ActorMaterializer()

    // val source = Source(1 to 10)
    // val sink = Sink.fold[Int, Int](0)(_ + _)

    // connect the Source to the Sink, obtaining a RunnableGraph
    // val runnable: RunnableGraph[Future[Int]] = source.toMat(sink)(Keep.right)

    // materialize the flow and get the value of the FoldSink
    // val duration = Duration(5, "seconds")
    // val sum: Future[Int] = runnable.run()
    // val res = Await.result(sum, duration)
    // println(res)


    // val enumerator: Enumerator[String] = Enumeratee(source)

    Ok.chunked(source).as("text/plain")
    // Ok("hello")
  }
}
