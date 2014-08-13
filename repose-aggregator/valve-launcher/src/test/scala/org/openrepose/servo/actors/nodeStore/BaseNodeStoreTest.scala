package org.openrepose.servo.actors.nodeStore

import akka.actor.ActorRef
import akka.testkit.TestKit
import org.openrepose.servo.ReposeNode

trait BaseNodeStoreTest {
  this:TestKit =>

  lazy val nodeList1 = List(ReposeNode("repose", "repose_node1", "localhost", httpPort = Some(8080), httpsPort = None))

  lazy val nodeList2 = List(
    ReposeNode("repose", "repose_node1", "localhost", httpPort = Some(8080), httpsPort = None),
    ReposeNode("repose", "repose_node2", "localhost", httpPort = Some(8081), httpsPort = None)
  )

  lazy val nodeList3 = List(ReposeNode("repose", "repose_node2", "localhost", httpPort = Some(8081), httpsPort = None))

  //Create a props that the other actor can use to "start stuff"
  def testStartActorProps(probe:ActorRef) = TestReposeNodeActor.props(probe)

}
