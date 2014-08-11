package org.openrepose.servo.actors

import akka.actor.{Actor, ActorRef, Props}
import org.openrepose.servo.actors.NodeStoreMessages.Initialize

object TestReposeNodeActor {
  def props(forwardPoint:ActorRef) = Props(classOf[TestReposeNodeActor], forwardPoint)
}

//create a test actor that sends messages to something when it's turned on
class TestReposeNodeActor(forwardPoint: ActorRef) extends Actor {

  var nodeId:String = _
  var clusterId:String = _

  override def preStart() = {
    forwardPoint ! "Started"
  }

  override def postStop(): Unit = {
    forwardPoint ! s"Stopped clusterId: $clusterId nodeId: $nodeId"
  }

  override def receive: Receive = {
    case x@Initialize(cid, nid) => {
      clusterId = cid
      nodeId = nid
      forwardPoint forward x
    }
    case x => forwardPoint forward x
  }
}
