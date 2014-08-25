package org.openrepose.servo.actors.nodeStore

import akka.actor._
import akka.testkit.{TestKit, TestProbe}
import org.junit.runner.RunWith
import org.openrepose.servo.{ReposeNode, TestUtils}
import org.openrepose.servo.actors.NodeStore
import org.openrepose.servo.actors.NodeStoreMessages.{ConfigurationUpdated, Initialize}
import org.scalatest.junit.JUnitRunner
import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll, FunSpecLike, Matchers}

@RunWith(classOf[JUnitRunner])
class NodeStoreNodeList2Test(_system: ActorSystem) extends TestKit(_system)
with FunSpecLike with Matchers with BeforeAndAfter with BeforeAndAfterAll with TestUtils with BaseNodeStoreTest {

  import scala.concurrent.duration._

  def this() = this(ActorSystem("NodeStoreList2Spec"))

  override def afterAll() = {
    TestKit.shutdownActorSystem(system)
  }

  //Variable used in before/after blocks to handle my actor
  var nodeStoreVar: ActorRef = _

  //Using a standalone test probe works so much better here, because I'm actually creating a new nodestore each time
  var probe: TestProbe = _

  before {
    probe = TestProbe()

    nodeStoreVar = system.actorOf(NodeStore.props(propsFunc(probe.ref)))
    nodeStoreVar ! ConfigurationUpdated(Some(nodeList2), Some(containerConfig1))

    probe.expectMsgAllOf(1 seconds,
      "Started repose:repose_node1",
      "Started repose:repose_node2")
  }

  after {
    nodeStoreVar ! PoisonPill
  }

  describe("The Node Store with nodeList2 running") {

    it("will do nothing when the same list is sent") {
      nodeStoreVar ! ConfigurationUpdated(Some(nodeList2), Some(containerConfig1))

      probe.expectNoMsg(1 second)
    }

    it("will stop all nodes when told to shut down") {
      nodeStoreVar ! PoisonPill
      probe.expectMsgAllOf(1 second, "Stopped repose:repose_node1", "Stopped repose:repose_node2")
    }
  }
}
