package org.openrepose.servo.actors

import akka.actor.{Actor, Props}
import akka.event.Logging
import org.openrepose.servo.actors.NodeStoreMessages.Initialize
import org.openrepose.servo.actors.ReposeLauncherProtocol.ProcessCheck

import scala.concurrent.Future
import scala.sys.process.{ProcessLogger, Process}

object ReposeLauncher {
  def props(command: Seq[String]) = Props(classOf[ReposeLauncher], command)
}

object ReposeLauncherProtocol {

  case object ProcessCheck

}

class ReposeLauncher(command: Seq[String]) extends Actor {

  import scala.concurrent.duration._

  val log = Logging(context.system, this)

  var clusterId: String = _
  var nodeId: String = _

  var process: scala.sys.process.Process = _

  override def receive: Receive = {
    case Initialize(cid, nid) => {
      clusterId = cid
      nodeId = nid

      //Start up the thingy!
      //See: http://www.scala-lang.org/api/2.10.3/index.html#scala.sys.process.ProcessCreation
      val builder = Process(command) //WIll add CWD and environment variables eventually

      //Fire that sucker up
      println("running command")
      process = builder.run(ProcessLogger(
        stdout => log.info(stdout)
      ))
      //Change our state, we're now running!

      //schedule some other thread to watch the process until it exits
      //Unfortunately the only way to do this stuff is to block up a thread.
      //Grab an execution context to run this future in
      implicit val executionContext = context.dispatcher
      Future {
        println("Starting up future!")
        process.exitValue()
      } onComplete { t =>
        //Stop myself?
        println("Future completed!")
        context.stop(context.self)
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
