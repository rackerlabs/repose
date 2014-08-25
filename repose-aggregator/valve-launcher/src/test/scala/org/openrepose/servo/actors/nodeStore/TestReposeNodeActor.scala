package org.openrepose.servo.actors.nodeStore

import akka.actor.{Actor, ActorRef, Props}
import org.openrepose.servo.ReposeNode
import org.openrepose.servo.actors.NodeStoreMessages.Initialize

object TestReposeNodeActor {
  def props(node:ReposeNode, forwardPoint: ActorRef) = Props(classOf[TestReposeNodeActor], node, forwardPoint)
}

//create a test actor that sends messages to something when it's turned on
class TestReposeNodeActor(node:ReposeNode, forwardPoint: ActorRef) extends Actor {

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
