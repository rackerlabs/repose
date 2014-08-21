package org.openrepose.servo.actors

import akka.actor.{Actor, PoisonPill, Props}
import akka.event.Logging
import org.openrepose.servo.actors.NodeStoreMessages.Initialize
import org.openrepose.servo.actors.ReposeLauncherProtocol.{ProcessCheck, ProcessExited}

import scala.concurrent.Future
import scala.sys.process.{Process, ProcessLogger}

object ReposeLauncher {
  def props(command: Seq[String],
            environment: Map[String, String] = Map.empty[String, String],
            warFilePath: String) = {
    Props(classOf[ReposeLauncher], command, environment, warFilePath)
  }
}

object ReposeLauncherProtocol {

  case object ProcessCheck

  case class ProcessExited(value: Int)

}

class ReposeLauncher(command: Seq[String], environment: Map[String, String], warFilePath: String) extends Actor {

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
        log.error(s"Command terminated abnormally. Value: $value")
      }
    }
    case Initialize(reposeNode) => {
      clusterId = reposeNode.clusterId
      nodeId = reposeNode.nodeId

      //Build the additonal params
      val args = if (reposeNode.httpPort.isDefined) {
        Seq("--port", reposeNode.httpPort.get.toString)
      } else {
        Seq.empty[String]
      }

      val cid = reposeNode.clusterId
      val nid = reposeNode.nodeId

      //modify our environment to include ClusterID and NodeID always
      val newEnv = environment + ("CLUSTER_ID" -> cid) + ("NODE_ID" -> nid)

      val newCommand = command ++ args ++ Seq(warFilePath)

      //Start up the thingy!
      //See: http://www.scala-lang.org/api/2.10.3/index.html#scala.sys.process.ProcessCreation
      // Magic :_* is from http://stackoverflow.com/questions/10842851/scala-expand-list-of-tuples-into-variable-length-argument-list-of-tuples
      val builder = Process(newCommand, None, newEnv.toList: _*) //Will add CWD and environment variables eventually

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
