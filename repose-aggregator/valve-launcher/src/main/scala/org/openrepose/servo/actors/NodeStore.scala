package org.openrepose.servo.actors

import java.util.UUID

import akka.actor.{PoisonPill, Props, ActorRef, Actor}
import akka.event.Logging
import org.openrepose.servo.{ContainerConfig, ReposeNode}
import org.openrepose.servo.actors.NodeStoreMessages.{ConfigurationUpdated, Initialize}
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

  //A mutable variable holding the state for container configuration
  var containerConfig:ContainerConfig = _

  def nodeKey(node: ReposeNode): String = {
    node.clusterId + node.nodeId
  }

  override def receive: Receive = {
    case configUpdated:ConfigurationUpdated => {
      //An updated config!
      log.info("New configuration received!")
      //If we were handed a container configuration, check to see if it's different
      val newCC = configUpdated.containerConfig.exists(cc => {
        val oldcc = containerConfig
        containerConfig = cc
        cc != oldcc
      })

      //Unbox the node list to either the one we're sent, or the existing running node list
      val nodeList = configUpdated.nodeList.map { list =>
        //Got a list of nodes to deal with
        list
      } getOrElse {
        //Using the same existing list, because with a new container config, we might need to restart all the nodes
        runningNodes.values.toList
      }

      //The stop list is either all the nodes, if its a new container config, or just the nodes that aren't in
      // the newly sent list
      val stopList = if(newCC) {
        runningNodes.values
      } else {
        runningNodes.filterNot(n => nodeList.contains(n._2)).map(_._2)
      }

      /**
       * Determine a list of nodes to start, either all the nodes we were just sent (which will either be the
       * newly sent nodes, or the existing list of nodes), or the differences between the nodes that are running
       * and the nodes that we were just sent
       */
      val startList = if(newCC) {
        nodeList
      } else {
        nodeList.filterNot(n => runningNodes.contains(nodeKey(n)))
      }.toList

      //Stop all the nodes we were told to stop
      stopList.foreach(n => {
        val nk = nodeKey(n)
        //Kill the children!
        childActors(nk) ! PoisonPill
        //Remove this node from the running node list
        runningNodes.remove(nk)
      })

      //Start all the nodes we need to start
      startList.foreach(n => {
        val uuid = UUID.randomUUID.toString
        val nk = nodeKey(n)
        //Get a props for this actor

        val nodeProps = actorPropsFunction(n)
        log.info(s"node props: ${nodeProps}")

        val actor = context.actorOf(nodeProps, s"${n.clusterId}_${n.nodeId}_runner_${uuid}")
        //Once the actor is created, it's already running, the props and everything are the necessary commands

        //Persist our jank
        childActors(nk) = actor
        runningNodes(nk) = n
      })

    }
  }
}
