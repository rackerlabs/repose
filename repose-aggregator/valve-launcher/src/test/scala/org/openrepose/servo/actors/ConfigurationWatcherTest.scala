package org.openrepose.servo.actors

import java.io.File

import akka.actor.{Terminated, PoisonPill, ActorSystem}
import akka.testkit.{TestProbe, EventFilter, ImplicitSender, TestKit}
import com.typesafe.config.ConfigFactory
import org.junit.runner.RunWith
import org.openrepose.servo._
import org.openrepose.servo.actors.NodeStoreMessages.ConfigurationUpdated
import org.scalatest.junit.JUnitRunner
import org.scalatest.{BeforeAndAfterAll, FunSpecLike, Matchers}

@RunWith(classOf[JUnitRunner])
class ConfigurationWatcherTest(_system: ActorSystem) extends TestKit(_system)
with FunSpecLike with Matchers with BeforeAndAfterAll with TestUtils {

  import scala.concurrent.duration._

  //An actor system with a test event listener so I can snag log messages!
  def this() = this(ActorSystem("SystemModelWatcherSpec", ConfigFactory.parseString(
    """
      |    akka.loggers = [akka.testkit.TestEventListener]
    """.stripMargin)))


  override def afterAll() = {
    TestKit.shutdownActorSystem(system)
  }

  lazy val systemModel1 = resourceContent("/actorTesting/system-model-1.cfg.xml")
  lazy val systemModel2 = resourceContent("/actorTesting/system-model-2.cfg.xml")

  lazy val containerConfig1 = resourceContent("/actorTesting/container-1.cfg.xml")
  lazy val containerConfig2 = resourceContent("/actorTesting/container-2.cfg.xml")

  lazy val nodeList1 = List(ReposeNode("repose", "repose_node1", "localhost", httpPort = Some(8080), httpsPort = None))
  lazy val nodeList2 = List(
    ReposeNode("repose", "repose_node1", "localhost", httpPort = Some(8080), httpsPort = None),
    ReposeNode("repose", "repose_node2", "localhost", httpPort = Some(8081), httpsPort = None)
  )

  def cleanConfigDir(configRoot:String):Unit = {
    val f = new File(configRoot)

    f.listFiles().foreach {f =>
      f.delete()
    }
  }

  //This now has to watch a couple configuration files and maintain state
  describe("Configuration Watcher Actor watches the configured directory for changes") {
    it("Notifies on various filesystem changed events") {
      val probe = TestProbe()
      val configRoot = tempDir("servo").toString

      //Verify a simple system model
      val smwActor = system.actorOf(ConfigurationWatcher.props(configRoot, probe.ref))
      writeSystemModel(configRoot, systemModel1)
      probe.expectMsg(2 second, ConfigurationUpdated(Some(nodeList1), None))

      //add a container configuration, verify new sending
      writeContainerConfig(configRoot, containerConfig1)
      probe.expectMsg(2 second, ConfigurationUpdated(None, Some(ContainerConfig("log4j.properties", Some(KeystoreConfig("keystore1", "lePassword", "leKeyPassword"))))))

      //Can't get a dual file configuration message, the watcher loop is too short....
      //Have to delete the files, because of threaded concurrent access within the JVM
      cleanConfigDir(configRoot)

      writeSystemModel(configRoot, systemModel2)
      probe.expectMsg(2 second, ConfigurationUpdated(Some(nodeList2), None))

      writeContainerConfig(configRoot, containerConfig2)
      probe.expectMsg(2 second, ConfigurationUpdated(None, Some(ContainerConfig("log4j.properties", None))))

      smwActor ! PoisonPill
    }
    it("will not notify if there are no changes") {
      val probe = TestProbe()
      //Create an actor and give it a destination and who to tell about changes
      val configRoot = tempDir("servo").toString
      //Set up the config directory with a valid System Model

      writeSystemModel(configRoot, systemModel1)

      val smwActor = system.actorOf(ConfigurationWatcher.props(configRoot, probe.ref))
      //tickle something in the config root directory

      //expect a message from the actor notifying us of repose local nodes
      //I can just send a List[ReposeNode] I don't need any other info
      probe.expectNoMsg(1 second)

      smwActor ! PoisonPill
    }

    it("Logs a failure message when unable to parse any of the configuration files") {
      val probe = TestProbe()
      val configRoot = tempDir("servo").toString
      val systemModelFail = resourceContent("/system-model-test/not-valid-xml.xml")
      val smwActor = system.actorOf(ConfigurationWatcher.props(configRoot, probe.ref))

      //Expect that I log an error message with a SystemModelParseException in it
      EventFilter[SystemModelParseException](occurrences = 1) intercept {
        writeSystemModel(configRoot, systemModelFail)
      }

      //No more messages, in other words, don't give me work to do!
      probe.expectNoMsg(1 second)
    }
  }
}
