package org.openrepose.servo.actors

import akka.actor.{Actor, PoisonPill, Props}
import akka.event.Logging
import org.openrepose.servo.actors.NodeStoreMessages.Initialize
import org.openrepose.servo.actors.ReposeLauncherProtocol.{ProcessCheck, ProcessExited}

import scala.concurrent.Future
import scala.sys.process.{Process, ProcessLogger}

object ReposeLauncher {
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

  import scala.concurrent.duration._

  val log = Logging(context.system, this)

  var clusterId: String = _
  var nodeId: String = _

  var process: Option[scala.sys.process.Process] = None


  override def postStop() = {
    process.map(_.destroy())
  }

  override def receive: Receive = {
    case ProcessExited(value) => {
      if (value != 0) {
        val msg = s"Repose Node Execution terminated abnormally. Value: $value"
        log.error(msg)
        throw ProcessAbnormalTermination(msg)
      }
    }
    case Initialize(reposeNode) => {
      clusterId = reposeNode.clusterId
      nodeId = reposeNode.nodeId

      //Environment needs to deal only with the repose opts, which are handed in

      //Start up the thingy!
      //See: http://www.scala-lang.org/api/2.10.3/index.html#scala.sys.process.ProcessCreation
      // Magic :_* is from http://stackoverflow.com/questions/10842851/scala-expand-list-of-tuples-into-variable-length-argument-list-of-tuples
      val builder = Process(command, None, environment.toList: _*)

      //Fire that sucker up
      process = Some(builder.run(ProcessLogger(
        stdout => log.info(stdout),
        stderr => log.warning(stderr)
      )))
      //Change our state, we're now running!

      //schedule some other thread to watch the process until it exits
      //Unfortunately the only way to do this stuff is to block up a thread.
      //Grab an execution context to run this future in
      implicit val executionContext = context.dispatcher
      val myRef = context.self
      Future {
        //I can assume this exists, because I just made it
        process.get.exitValue()
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
    }
  }

  //TODO: might not need this
  def scheduleCheck() = {
    implicit val executionContext = context.system.dispatcher
    context.system.scheduler.scheduleOnce(500 millis) {
      context.self ! ProcessCheck
    }

  }
}
