package org.openrepose.servo.actors.nodeStore

import akka.actor._
import akka.testkit.{TestKit, TestProbe}
import org.junit.runner.RunWith
import org.openrepose.servo.actors.NodeStore
import org.openrepose.servo.actors.NodeStoreMessages.Initialize
import org.openrepose.servo.{ReposeNode, TestUtils}
import org.scalatest.junit.JUnitRunner
import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll, FunSpecLike, Matchers}

@RunWith(classOf[JUnitRunner])
class EmptyNodeStoreTest(_system: ActorSystem) extends TestKit(_system)
with FunSpecLike with Matchers with BeforeAndAfter with BeforeAndAfterAll with TestUtils with BaseNodeStoreTest {

  import scala.concurrent.duration._

  def this() = this(ActorSystem("NodeStoreEmptySpec"))

  override def afterAll() = {
    TestKit.shutdownActorSystem(system)
  }

  //Variable used in before/after blocks to handle my actor
  var nodeStoreVar: ActorRef = _

  describe("The Node Store when no nodes are running") {
    it("will start nodes that are sent to it") {
      val probe = TestProbe()
      //The actor needs to have given to it a Props of the kind of actor to start
      val nodeStore = system.actorOf(NodeStore.props(testStartActorProps(probe.testActor)))

      nodeStore ! nodeList1

      probe.expectMsg(1 second, "Started")
      //Have to expect an initialize message, can't figure out how to handle it through props
      probe.expectMsg(1 second, Initialize(ReposeNode("repose", "repose_node1", "localhost", Some(8080), None)))
      nodeStore ! PoisonPill
    }

    it("will not do anything if an empty list of nodes is sent to it") {
      val probe = TestProbe()
      val nodeStore = system.actorOf(NodeStore.props(testStartActorProps(probe.testActor)))

      nodeStore ! List.empty[ReposeNode]

      probe.expectNoMsg(1 second)
      nodeStore ! PoisonPill
    }
  }
}
