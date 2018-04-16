package controllers

import akka.event.Logging
import akka.util.ByteString
import akka.stream.{Attributes, Inlet, Outlet, FlowShape}
import akka.stream.stage.{
  GraphStage, GraphStageLogic, InHandler, OutHandler}

/**
 *  Experiment with custom flow stage for in-memory processing
 *  (i.e. filtering) of chunks. This stage chunks stream along CSV line
 *  boundaries. A chunk would contain many CSV lines but ensure that a line
 *  break coincident with end of the chunk. This stage holds state between
 *  chunks (leftover from incoming chunk between last line break and chunk
 *  end.)
 *
 *  Currently unused but tested. Might come in handy for later improvement of
 *  the filter stage.
 */
class MyCSVStage extends GraphStage[FlowShape[ByteString, ByteString]] {

  private val in = Inlet[ByteString](Logging.simpleName(this) + ".in")
  private val out = Outlet[ByteString](Logging.simpleName(this) + ".out")
  override val shape = FlowShape(in, out)

  override def createLogic(inheritedAttributes: Attributes) = 
    new GraphStageLogic(shape) with InHandler with OutHandler {

    setHandlers(in, out, this)
    var leftover = ByteString()

    override def onPush(): Unit = {
      val elem = leftover ++ grab(in)
      val endPosition = elem.lastIndexOf(10)
      val send = elem.slice(0, endPosition+1)
      leftover = elem.slice(endPosition+1, elem.size+1)
      if (send.length > 0) {
        push(out, send)
      }
    }

    override def onPull(): Unit = {
      pull(in)
    }

    override def onUpstreamFinish(): Unit = {
      emit(out, leftover)
      completeStage()
    }
  }
}
