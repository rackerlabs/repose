package org.openrepose.servo.actors.nodeStore

import akka.actor._
import akka.testkit.{TestProbe, ImplicitSender, TestKit}
import org.junit.runner.RunWith
import org.openrepose.servo.actors.NodeStoreMessages.Initialize
import org.openrepose.servo.actors.{TestReposeNodeActor, NodeStore}
import org.openrepose.servo.{ReposeNode, TestUtils}
import org.scalatest.junit.JUnitRunner
import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll, FunSpecLike, Matchers}

@RunWith(classOf[JUnitRunner])
class NodeStoreNodeList1Test(_system: ActorSystem) extends TestKit(_system)
with FunSpecLike with Matchers with BeforeAndAfter with BeforeAndAfterAll with TestUtils with BaseNodeStoreTest {

  import scala.concurrent.duration._

  def this() = this(ActorSystem("NodeStoreList1Spec"))

  override def afterAll() = {
    TestKit.shutdownActorSystem(system)
  }

  //Variable used in before/after blocks to handle my actor
  var nodeStoreVar: ActorRef = _

  //Using a standalone test probe works so much better here, because I'm actually creating a new nodestore each time
  var probe: TestProbe = _

  before {
    probe = TestProbe()

    nodeStoreVar = system.actorOf(NodeStore.props(testStartActorProps(probe.testActor)))
    nodeStoreVar ! nodeList1 //Send it nodeList1 to run stuff...
    probe.expectMsgAllOf(1 second,
      "Started",
      Initialize("repose", "repose_node1"))
  }

  after {
    nodeStoreVar ! PoisonPill
  }

  describe("The Node Store with nodeList1 running") {

    it("will do nothing when the same list is sent") {
      nodeStoreVar ! nodeList1

      probe.expectNoMsg(1 second)
    }
    it("will start a new local node when NodeList2 is sent") {
      nodeStoreVar ! nodeList2
      probe.expectMsg(1 second, "Started")
      probe.expectMsg(1 second, Initialize("repose", "repose_node2"))
    }

    it("will start node2 and stop repose_node1 when NodeList3 is sent") {
      nodeStoreVar ! nodeList3

      //Expect all these messages within 1 second, no ordering,
      // But I'm also getting the shutdown messages?
      probe.expectMsgAllOf(1 second,
        "Started",
        Initialize("repose", "repose_node2"),
        "Stopped clusterId: repose nodeId: repose_node1")
    }

    it("will stop all nodes when told to shut down") {
      nodeStoreVar ! PoisonPill
      probe.expectMsg(1 second, "Stopped clusterId: repose nodeId: repose_node1")
    }
  }
}
