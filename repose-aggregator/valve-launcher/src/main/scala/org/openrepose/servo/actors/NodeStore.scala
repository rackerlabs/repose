package org.openrepose.servo.actors

import java.util.UUID

import akka.actor.{PoisonPill, Props, ActorRef, Actor}
import akka.event.Logging
import org.openrepose.servo.{ContainerConfig, ReposeNode}
import org.openrepose.servo.actors.NodeStoreMessages.Initialize
import org.openrepose.servo.actors.ReposeLauncher.LauncherPropsFunction

//A bit of logic to give me a mutable map
//List exists as MutableList, don't know why map doesn't

import scala.collection.mutable.{Map => MutableMap}


object NodeStore {
  def props(propsFunction: LauncherPropsFunction) = Props(classOf[NodeStore], propsFunction)
}

object NodeStoreMessages {

  case class ConfigurationUpdated(nodeList:Option[List[ReposeNode]], containerConfig:Option[ContainerConfig])

  //TODO Blow away this guy
  case class Initialize(nodeInfo: ReposeNode)

}

class NodeStore(actorPropsFunction: LauncherPropsFunction) extends Actor {
  val log = Logging(context.system, this)

  log.info("NODE STORE TURNED ON")

  //A pair of mutable values so I can change stuff.
  //I can't change the list/map out, but I can change what's in the list/map
  val runningNodes: MutableMap[String, ReposeNode] = MutableMap.empty[String, ReposeNode]
  val childActors: MutableMap[String, ActorRef] = MutableMap.empty[String, ActorRef]

  def nodeKey(node: ReposeNode): String = {
    node.clusterId + node.nodeId
  }

  override def receive: Receive = {
    case list: List[ReposeNode] => {
      log.info("RECEIVED NODE LIST")
      //Figure out what nodes we need to stop
      // Stuff that's not in the sent list should be stopped!
      val stopList = runningNodes.filterNot(n => list.contains(n._2)).map(_._2)

      //Figure out what nodes we need to start
      val startList = list.filterNot(n => runningNodes.contains(nodeKey(n)))

      stopList.foreach(n => {
        val nk = nodeKey(n)
        //Kill the children!
        childActors(nk) ! PoisonPill
        //Remove this node from the running node list
        runningNodes.remove(nk)
      })

      startList.foreach(n => {
        val uuid = UUID.randomUUID.toString
        val nk = nodeKey(n)
        //Get a props for this actor

        val nodeProps = actorPropsFunction(n)
        log.info(s"node props: ${nodeProps}")

        val actor = context.actorOf(nodeProps, s"${n.clusterId}_${n.nodeId}_runner_${uuid}")
        //TODO: I should not send the initialize any longer
        actor ! Initialize(n)

        //Persist our jank
        childActors(nk) = actor
        runningNodes(nk) = n
      })
    }
  }
}
