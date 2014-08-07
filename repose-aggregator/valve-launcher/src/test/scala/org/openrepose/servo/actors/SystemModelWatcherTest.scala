package org.openrepose.servo.actors

import java.nio.file.Files

import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestKit}
import org.junit.runner.RunWith
import org.openrepose.servo.{ReposeNode, TestUtils}
import org.scalatest.junit.JUnitRunner
import org.scalatest.{BeforeAndAfterAll, FunSpecLike, Matchers}

@RunWith(classOf[JUnitRunner])
class SystemModelWatcherTest(_system: ActorSystem) extends TestKit(_system)
with ImplicitSender with FunSpecLike with Matchers with BeforeAndAfterAll with TestUtils {

  import scala.concurrent.duration._

  def this() = this(ActorSystem("SystemModelWatcherSpec"))

  override def afterAll() = {
    TestKit.shutdownActorSystem(system)
  }

  def updateSystemModel(configRoot: String, content: String): Unit = {

  }

  lazy val systemModel1 = resourceContent("/actorTesting/system-model-1.cfg.xml")
  lazy val systemModel2 = resourceContent("/actorTesting/system-model-2.cfg.xml")

  lazy val nodeList1 = List(ReposeNode("repose", "repose_node1", "localhost", httpPort = Some(8080), httpsPort = None))
  lazy val nodeList2 = List(
    ReposeNode("repose", "repose_node1", "localhost", httpPort = Some(8080), httpsPort = None),
    ReposeNode("repose", "repose_node2", "localhost", httpPort = Some(8081), httpsPort = None)
  )

  describe("System Model Watcher Actor watches the configured directory for changes") {
    it("will notify when something has changed with a changed message") {
      //Create an actor and give it a destination and who to tell about changes
      val configRoot = Files.createTempDirectory("servo").toString
      //Set up the config directory with a valid System Model

      val smwActor = system.actorOf(SystemModelWatcher.props(configRoot, testActor))
      //tickle something in the config root directory

      updateSystemModel(configRoot, systemModel1)
      //expect a message from the actor notifying us of repose local nodes
      //I can just send a List[ReposeNode] I don't need any other info
      expectMsg(1000 millis, nodeList1)
    }
    it("will not notify if there are no changes") {
      pending
    }
  }
}
