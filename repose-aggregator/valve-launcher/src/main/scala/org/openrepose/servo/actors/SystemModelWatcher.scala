package org.openrepose.servo.actors

import akka.actor.{Props, ActorRef, Actor}
import akka.actor.Actor.Receive

object SystemModelWatcher {
  def props(directory: String, notifyActor: ActorRef) = Props(classOf[SystemModelWatcher], directory, notifyActor)
}

class SystemModelWatcher(directory: String, notifyActor: ActorRef) extends Actor {

  override def receive: Receive = ???
}
