package org.openrepose.servo.actors.nodeStore

import akka.actor._
import org.openrepose.servo.ReposeNode
import org.openrepose.servo.actors.NodeStoreMessages.Initialize
import org.openrepose.servo.actors.ProcessAbnormalTermination

object FailureTestActor {
  def props(node:ReposeNode, forwardPoint: ActorRef) = Props(classOf[FailureTestActor], node, forwardPoint)
}

//create a test actor that sends messages to something when it's turned on
class FailureTestActor(node:ReposeNode, forwardPoint: ActorRef) extends Actor {

  println("IM GONNA WRECK IT!")
  //This happens to handle an exception on creation!
  throw ProcessAbnormalTermination("IM GONNA WRECK IT!")

  override def preStart() = {
    forwardPoint ! s"Started ${node.clusterId}:${node.nodeId}"
  }

  override def toString() = {
    s"TestActor: ${node.clusterId}:${node.nodeId}"
  }

  override def postStop(): Unit = {
    val message = s"Stopped ${node.clusterId}:${node.nodeId}"
    forwardPoint ! message
  }

  override def receive: Receive = {
    case x => forwardPoint forward x
  }
}
