package org.openrepose.servo.actors.nodeStore

import akka.actor._
import akka.testkit.{TestProbe, ImplicitSender, TestKit}
import org.junit.runner.RunWith
import org.openrepose.servo.actors.NodeStoreMessages.{ConfigurationUpdated, Initialize}
import org.openrepose.servo.actors.NodeStore
import org.openrepose.servo.{ContainerConfig, ReposeNode, TestUtils}
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

  val initial = ConfigurationUpdated(Some(nodeList1), Some(containerConfig1))

  before {
    probe = TestProbe()

    nodeStoreVar = system.actorOf(NodeStore.props(propsFunc(probe.ref)))
    nodeStoreVar ! initial
    probe.expectMsg(1 second, "Started repose:repose_node1")
  }

  after {
    nodeStoreVar ! PoisonPill
  }


  describe("The Node Store with nodeList1 and container config 1 running") {
    def someTests: Option[ContainerConfig] => Unit = { cc =>
      it("will do nothing when the node list is the same") {
        nodeStoreVar ! initial

        probe.expectNoMsg(1 second)
      }

      it("will start a new local node when NodeList2 is sent") {
        nodeStoreVar ! ConfigurationUpdated(Some(nodeList2), cc)
        probe.expectMsg(1 second, "Started repose:repose_node2")
      }

      it("will start node2 and stop repose_node1 when NodeList3 is sent") {
        nodeStoreVar ! ConfigurationUpdated(Some(nodeList3), cc)

        //Expect all these messages within 1 second, no ordering,
        // But I'm also getting the shutdown messages?
        probe.expectMsgAllOf(1 second,
          "Started repose:repose_node2",
          "Stopped repose:repose_node1")
      }

    }

    describe("keeping the same configuration sent") {
      someTests(Some(containerConfig1))
    }
    describe("without sending any configuration at all") {
      someTests(None)
    }

    describe("with a differing container config") {
      it("will restart all running nodes, even if none were sent") {
        nodeStoreVar ! ConfigurationUpdated(None, Some(containerConfig2))

        probe.expectMsgAllOf(1 second,
          "Stopped repose:repose_node1",
          "Started repose:repose_node1"
        )
      }
      it("will stop existing nodes, and start the nodes from list 2") {
        nodeStoreVar ! ConfigurationUpdated(Some(nodeList2), Some(containerConfig2))

        probe.expectMsgAllOf(1 second,
        "Stopped repose:repose_node1",
        "Started repose:repose_node1",
        "Started repose:repose_node2"
        )
      }
    }

    it("will stop all nodes when told to shut down") {
      nodeStoreVar ! PoisonPill
      probe.expectMsg(1 second, "Stopped repose:repose_node1")
    }
  }
}
