package org.openrepose.servo.actors.nodeStore

import akka.actor._
import akka.testkit.{ImplicitSender, TestKit}
import org.junit.runner.RunWith
import org.openrepose.servo.actors.NodeStoreMessages.Initialize
import org.openrepose.servo.actors.{TestReposeNodeActor, NodeStore}
import org.openrepose.servo.{ReposeNode, TestUtils}
import org.scalatest.junit.JUnitRunner
import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll, FunSpecLike, Matchers}

@RunWith(classOf[JUnitRunner])
class NodeStoreTest2(_system: ActorSystem) extends TestKit(_system)
 with ImplicitSender with FunSpecLike with Matchers with BeforeAndAfter with BeforeAndAfterAll with TestUtils with BaseNodeStoreTest {

   import scala.concurrent.duration._

   def this() = this(ActorSystem("NodeStoreSpec"))

   override def afterAll() = {
     TestKit.shutdownActorSystem(system)
   }

   //Variable used in before/after blocks to handle my actor
   var nodeStoreVar:ActorRef = _

   describe("The Node Store") {
     describe("when NodeList1 is running") {
       before {
         nodeStoreVar = system.actorOf(NodeStore.props(testStartActorProps))
         nodeStoreVar ! nodeList1 //Send it nodeList1 to run stuff...
         expectMsg(1 second, "Started")
         expectMsg(1 second, Initialize("repose", "repose_node1"))
       }

       after {
         nodeStoreVar ! PoisonPill
         expectMsg(1 second, "Stopped")
       }

       it("will do nothing when the same list is sent") {
         nodeStoreVar ! nodeList1

         expectNoMsg(1 second)
       }
       it("will start a new local node when NodeList2 is sent") {
         pending
       }
       it("will stop repose_node1 when NodeList3 is sent") {
         pending
       }
       it("will stop all nodes when told to shut down") {
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
