package org.openrepose.servo.actors.nodeStore

import akka.actor.{Props, ActorRef}
import akka.testkit.TestKit
import org.openrepose.servo.actors.ReposeLauncher._
import org.openrepose.servo.{KeystoreConfig, ContainerConfig, ReposeNode}

trait BaseNodeStoreTest {
  this: TestKit =>

  lazy val nodeList1 = List(ReposeNode("repose", "repose_node1", "localhost", httpPort = Some(8080), httpsPort = None))

  lazy val nodeList2 = List(
    ReposeNode("repose", "repose_node1", "localhost", httpPort = Some(8080), httpsPort = None),
    ReposeNode("repose", "repose_node2", "localhost", httpPort = Some(8081), httpsPort = None)
  )

  lazy val nodeList3 = List(ReposeNode("repose", "repose_node2", "localhost", httpPort = Some(8081), httpsPort = None))

  lazy val containerConfig1 = ContainerConfig("log4j.properties", None)
  lazy val containerConfig2 = ContainerConfig("log4j.properties", Some(KeystoreConfig("keystore", "keystorePass", "keyPass")))

  //Create a props that the other actor can use to "start stuff"
  def testStartActorProps(node: ReposeNode, probe: ActorRef) = TestReposeNodeActor.props(node, probe)

  //TODO: finish defining a function to hand to the thing to create an actor
  def propsFunc(probe: ActorRef): LauncherPropsFunction = { node: ReposeNode =>
    testStartActorProps(node, probe)
  }

}
