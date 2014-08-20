package org.openrepose.servo.actors

import java.nio.charset.StandardCharsets
import java.nio.file.{StandardOpenOption, Paths, Files}

import akka.actor.{PoisonPill, ActorSystem}
import akka.testkit.{EventFilter, ImplicitSender, TestKit}
import com.typesafe.config.ConfigFactory
import org.junit.runner.RunWith
import org.openrepose.servo.{SystemModelParseException, ReposeNode, TestUtils}
import org.scalatest.junit.JUnitRunner
import org.scalatest.{BeforeAndAfterAll, FunSpecLike, Matchers}

@RunWith(classOf[JUnitRunner])
class SystemModelWatcherTest(_system: ActorSystem) extends TestKit(_system)
with ImplicitSender with FunSpecLike with Matchers with BeforeAndAfterAll with TestUtils {

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

    lazy val nodeList1 = List(ReposeNode("repose", "repose_node1", "localhost", httpPort = Some(8080), httpsPort = None))
    lazy val nodeList2 = List(
        ReposeNode("repose", "repose_node1", "localhost", httpPort = Some(8080), httpsPort = None),
        ReposeNode("repose", "repose_node2", "localhost", httpPort = Some(8081), httpsPort = None)
    )

    describe("System Model Watcher Actor watches the configured directory for changes") {
        it("will notify when something has changed with a changed message") {
            //Create an actor and give it a destination and who to tell about changes
            val configRoot = tempDir("servo").toString
            //Set up the config directory with a valid System Model

            val smwActor = system.actorOf(SystemModelWatcher.props(configRoot, testActor))
            //tickle something in the config root directory

            writeSystemModel(configRoot, systemModel1)
            //expect a message from the actor notifying us of repose local nodes
            //I can just send a List[ReposeNode] I don't need any other info
            expectMsg(1000 millis, nodeList1)
            smwActor ! PoisonPill
        }

        it("will not notify if there are no changes") {
            //Create an actor and give it a destination and who to tell about changes
            val configRoot = tempDir("servo").toString
            //Set up the config directory with a valid System Model

            writeSystemModel(configRoot, systemModel1)

            val smwActor = system.actorOf(SystemModelWatcher.props(configRoot, testActor))
            //tickle something in the config root directory

            //expect a message from the actor notifying us of repose local nodes
            //I can just send a List[ReposeNode] I don't need any other info
            expectNoMsg(1 second)

            smwActor ! PoisonPill
        }

        it("Logs a failure message when unable to parse the system model") {
            val configRoot = tempDir("servo").toString
            val systemModelFail = resourceContent("/system-model-test/not-valid-xml.xml")
            val smwActor = system.actorOf(SystemModelWatcher.props(configRoot, testActor))

            //Expect that I log an error message with a SystemModelParseException in it
            EventFilter[SystemModelParseException](occurrences = 1) intercept {
                writeSystemModel(configRoot, systemModelFail)
            }

            //No more messages, in other words, don't give me work to do!
            expectNoMsg(1 second)
        }
    }
}
