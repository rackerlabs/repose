package org.openrepose.servo.actors.nodeStore

import akka.actor._
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import org.apache.log4j.BasicConfigurator
import org.junit.runner.RunWith
import org.openrepose.servo.actors.NodeStore
import org.openrepose.servo.actors.NodeStoreMessages.ConfigurationUpdated
import org.openrepose.servo.{ReposeNode, TestUtils}
import org.scalatest.junit.JUnitRunner
import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll, FunSpecLike, Matchers}


@RunWith(classOf[JUnitRunner])
class NodeFailureTest(_system: ActorSystem) extends TestKit(_system)
with FunSpecLike with Matchers with BeforeAndAfter with BeforeAndAfterAll with TestUtils with BaseNodeStoreTest with ImplicitSender {


  def this() = this(ActorSystem("NodeStoreFailureSpec"))

  override def afterAll() = {
    TestKit.shutdownActorSystem(system)
  }

  //Variable used in before/after blocks to handle my actor
  var nodeStoreVar: ActorRef = _


  describe("The Node Store when a node fails") {
    it("passes the exception up so it gets to the guardian") {
      BasicConfigurator.configure()
      val probe = TestProbe()

      val nodeStoreProps = NodeStore.props({ node:ReposeNode =>
        FailureTestActor.props(node, probe.ref)
      })

      val nodeStore = system.actorOf(nodeStoreProps)
      println(s"Node store is: ${nodeStore}")

      watch(nodeStore)

      nodeStore ! ConfigurationUpdated(Some(nodeList1), Some(containerConfig1))

      expectMsgPF() {
        case t@Terminated(actor) => {
          println("it crashed!")
        }
      }

    }
  }
}
