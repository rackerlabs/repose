package org.openrepose.servo.actors.nodeStore

import akka.actor.{Actor, ActorRef, Props}
import org.openrepose.servo.actors.NodeStoreMessages.Initialize
import org.openrepose.servo.actors.ProcessAbnormalTermination

object FailureTestActor {
  def props(forwardPoint: ActorRef) = Props(classOf[FailureTestActor], forwardPoint)
}

//create a test actor that sends messages to something when it's turned on
class FailureTestActor(forwardPoint: ActorRef) extends Actor {

  var nodeId: String = _
  var clusterId: String = _

  override def preStart() = {
    forwardPoint ! "Started"
  }

  override def toString() = {
    s"TestActor: $clusterId:$nodeId"
  }

  override def postStop(): Unit = {
    val message = s"Stopped clusterId: $clusterId nodeId: $nodeId"
    forwardPoint ! message
  }

  override def receive: Receive = {
    case x@Initialize(reposeNode) => {
      clusterId = reposeNode.clusterId
      nodeId = reposeNode.nodeId
      forwardPoint forward x
      println("IM GONNA WRECK IT!")
      throw ProcessAbnormalTermination("I'M GONNA WRECK IT")
    }
    case x => forwardPoint forward x
  }
}
