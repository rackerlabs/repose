package org.openrepose.servo.actors

import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestKit}
import org.junit.runner.RunWith
import org.openrepose.servo.{ReposeNode, TestUtils}
import org.scalatest.junit.JUnitRunner
import org.scalatest.{BeforeAndAfterAll, FunSpecLike, Matchers}

@RunWith(classOf[JUnitRunner])
class NodeStoreTest(_system: ActorSystem) extends TestKit(_system)
with ImplicitSender with FunSpecLike with Matchers with BeforeAndAfterAll with TestUtils {

  def this() = this(ActorSystem("NodeStoreSpec"))

  override def afterAll() = {
    TestKit.shutdownActorSystem(system)
  }

  lazy val nodeList1 = List(ReposeNode("repose", "repose_node1", "localhost", httpPort = Some(8080), httpsPort = None))
  lazy val nodeList2 = List(
    ReposeNode("repose", "repose_node1", "localhost", httpPort = Some(8080), httpsPort = None),
    ReposeNode("repose", "repose_node2", "localhost", httpPort = Some(8081), httpsPort = None)
  )
  lazy val nodeList3 = List(ReposeNode("repose", "repose_node2", "localhost", httpPort = Some(8081), httpsPort = None))

  describe("The Node Store") {
    describe("when no nodes are running") {
      it("will start nodes that are sent to it") {
        pending
      }
      it("will not do anything if an empty list of nodes is sent to it") {
        pending
      }
    }
    describe("when NodeList1 is running") {
      it("will do nothing when the same list is sent") {
        pending
      }
      it("will start a new local node when NodeList2 is sent"){
        pending
      }
      it("will stop repose_node1 when NodeList3 is sent") {
        pending
      }
      it("will stop all nodes when told to shut down"){
        pending
      }
    }
    describe("when NodeList2 is running") {
      it("will stop all nodes when told to shut down") {
        pending
      }
    }
  }

}
