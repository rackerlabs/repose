package org.openrepose.servo.actors

import akka.actor.{Actor, PoisonPill, Props}
import akka.event.Logging
import org.openrepose.servo.{CommandGenerator, ReposeNode}
import org.openrepose.servo.actors.NodeStoreMessages.Initialize
import org.openrepose.servo.actors.ReposeLauncherProtocol.{ProcessCheck, ProcessExited}

import scala.concurrent.Future
import scala.sys.process.{Process, ProcessLogger}

object ReposeLauncher {
  //I expect something that can take a repose node and return a props
  // I should be able to do some magic with a partially applied function to incorporate the command generator
  type LauncherPropsFunction = ReposeNode => Props

  def props(command: Seq[String],
            environment: Map[String, String] = Map.empty[String, String]) = {
    Props(classOf[ReposeLauncher], command, environment)
  }
}

object ReposeLauncherProtocol {

  case object ProcessCheck

  case class ProcessExited(value: Int)

}

//This should get the default behavior to escalate!
case class ProcessAbnormalTermination(msg: String, cause: Throwable = null) extends Throwable(msg, cause)

class ReposeLauncher(command: Seq[String], environment: Map[String, String]) extends Actor {

  val log = Logging(context.system, this)

  log.debug(s"Initializing actor for ${command mkString " "}")

  val builder = Process(command, None, environment.toList: _*)
  //Fire that sucker up
  val process = builder.run(ProcessLogger(
    stdout => log.info(stdout),
    stderr => log.warning(stderr)
  ))
  //schedule some other thread to watch the process until it exits
  //Unfortunately the only way to do this stuff is to block up a thread.
  //Grab an execution context to run this future in
  implicit val executionContext = context.dispatcher
  val myRef = context.self
  Future {
    //I can assume this exists, because I just made it
    process.exitValue()
  } onComplete { t =>
    //Note, these could be dead letters. Not sure how to check on that
    //It's not terribly important that they might send letters to dead actors, just log noise
    t.map(value => {
      //Send myself a message about the exit value
      myRef ! ProcessExited(value)
    })
    //Kill myself!
    myRef ! PoisonPill
  }

  override def postStop() = {
    process.destroy()
    process.exitValue() //I think this might be needed to have it block
  }

  override def receive: Receive = {
    case ProcessExited(value) => {
      if (value != 0) {
        val msg = s"Repose Node Execution terminated abnormally. Value: $value"
        log.error(msg)
        throw ProcessAbnormalTermination(msg)
      }
    }
    //Don't use Initialize any more at all, it's super deprecated
    //TODO: remove this case when I get all the tests updated
    case Initialize(reposeNode) => ???
  }
}
