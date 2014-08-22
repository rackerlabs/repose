package org.openrepose.servo.actors

import java.io.File

import akka.actor.{Props, PoisonPill, Terminated, ActorSystem}
import akka.event.Logging.Info
import akka.testkit.{CustomEventFilter, EventFilter, TestProbe, TestKit}
import com.typesafe.config.ConfigFactory
import org.junit.runner.RunWith
import org.openrepose.servo.{CommandGenerator, TestUtils, ReposeNode}
import org.openrepose.servo.actors.NodeStoreMessages.Initialize
import org.scalatest.junit.JUnitRunner
import org.scalatest.{FunSpecLike, Matchers, BeforeAndAfterAll}

import scala.io.Source

@RunWith(classOf[JUnitRunner])
class ReposeLauncherTest(_system: ActorSystem) extends TestKit(_system)
with FunSpecLike with Matchers with BeforeAndAfterAll with TestUtils {

  import scala.concurrent.duration._

  def this() = this(ActorSystem("ReposeLauncherSpec", ConfigFactory.parseString(
    """
      |akka.loggers = [akka.testkit.TestEventListener]
    """.stripMargin)))


  val testNode = ReposeNode("testCluster", "testNode", "localhost", Some(8080), None)

  override def afterAll() = {
    TestKit.shutdownActorSystem(system)
  }

  def launcherProps(preOpts:Seq[String], env:Map[String,String] = Map.empty[String,String]):Props = {
    val cg = new CommandGenerator(preOpts ++ Seq("--"), "configRoot", "launcherPath", "warFile")
    ReposeLauncher.props(cg.commandLine(testNode), env)
  }

  describe("The Repose Launcher") {
    it("will execute the command and remain running until the command exits") {

      //Create a test probe to watch for DEATH
      val probe = TestProbe()

      //create an actor with that command
      val props = launcherProps(Seq("bash", "-c", "sleep 1"))

      //Won't actually start until the initialize is sent
      val actor = system.actorOf(props)
      probe.watch(actor)

      //TODO: if I can get the entire command in there, it won't need to start with an initialize any longer
      actor ! Initialize(testNode)

      probe.expectMsgPF(3 seconds) {
        case Terminated(theActor) => {
          theActor should equal(actor) //make sure we got a death from our actor
        }
      }
    }
    it("will log standard out to info") {
      val probe = TestProbe()
      val props = launcherProps(Seq("bash", "-c", "echo 'lololol'"))

      val actor = system.actorOf(props)

      EventFilter.info(pattern = ".*lololol.*", occurrences = 1) intercept {
        actor ! Initialize(testNode)
      }
    }
    it("sets the command line parameter --port when given an HTTP port (not https)") {
      val probe = TestProbe()
      val props = launcherProps(Seq("bash", "-c", "echo $@"))

      val actor = system.actorOf(props)
      EventFilter.info(pattern = "--port 8080", occurrences = 1) intercept {
        actor ! Initialize(testNode)
      }
    }
    it("has the war file path at the end") {
      val probe = TestProbe()
      val props = launcherProps(Seq("bash", "-c", "echo $@"))

      val actor = system.actorOf(props)
      EventFilter.info(pattern = "warFile$", occurrences = 1) intercept {
        actor ! Initialize(testNode)
      }
    }
    it("Generates the necessary jetty configuration when given an HTTPS port (not http)") {
      //TODO: how does this even work?
      // I have to generate a jetty configuration file and pass it to the launcher for this part
      // But that also means I have to find a SSL cert and SSL key, no clue how existing repose got those...
      pending
    }
    it("Generates the necessary jetty configuration when both HTTP and HTTPS ports are specified") {
      pending
    }
    it("sets passed in environment variables") {
      val probe = TestProbe()
      val props = launcherProps(List("bash", "-c", "echo $ENV_VAR"), Map("ENV_VAR" -> "LOLWUT"))

      val actor = system.actorOf(props)

      EventFilter.info(message = "LOLWUT", occurrences = 1) intercept {
        actor ! Initialize(testNode)
      }
    }
    it("sets the system properties: repose-node-id, repose-cluster-id and powerapi-config-directory") {
      val probe = TestProbe()
      val props = launcherProps(Seq("bash", "-c", "echo $@", "--"))

      val actor = system.actorOf(props)

      EventFilter.custom({
        case Info(ref, clazz, msg: String)
          if msg.contains("-Drepose-node-id=") &&
            msg.contains("-Drepose-cluster-id") &&
            msg.contains("-Dpowerapi-config-directory") => true
      }, 1) intercept {
        actor ! Initialize(testNode)
      }


    }
    it("will log standard error out to warn") {
      val probe = TestProbe()
      val props = launcherProps(Seq("bash", "-c", "echo >&2 'standardError'"))

      val actor = system.actorOf(props)

      EventFilter.warning("standardError", occurrences = 1) intercept {
        actor ! Initialize(testNode)
      }
    }
    it("terminates the command and closes the streams when dying") {
      val probe = TestProbe()
      //Std out constant noise!

      //Create a temp file
      val f = tempFile("testing", ".txt")

      val fileName = f.getAbsolutePath
      val props = launcherProps(Seq("bash", "-c", "while true; do echo 'test' >> " + fileName + "; sleep 0.1; done"))

      val actor = system.actorOf(props)

      //Turn it on, it should start doing stuff
      actor ! Initialize(testNode)

      //Give it a bit of time to do a couple things
      Thread.sleep(500)

      //TERMINATE IT
      actor ! PoisonPill //Kill the actor before the process dies

      //Within a second or so, the file should stop receiving "tests"
      def fileLines(): Int = Source.fromFile(f).getLines().count(_ => true)

      var lineCount = 0
      var sames = 0
      val duration = 2.seconds
      val maxTime = System.currentTimeMillis() + duration.toMillis
      while (sames < 4 && maxTime > System.currentTimeMillis()) {
        val newLineCount = fileLines()
        //println(s"old: $lineCount, new: $newLineCount")
        if (newLineCount == lineCount) {
          //yay -- it was the same
          sames = sames + 1
        } else {
          lineCount = newLineCount
          sames = 0
          //Boo
        }
        Thread.sleep(200)
      }

      sames should be >= 4
    }

    it("logs an error and crashes the actor if the command executes abnormally") {
      val otherSystem = ActorSystem("CrashySystem", ConfigFactory.parseString(
        """
          |akka.loggers = [akka.testkit.TestEventListener]
        """.stripMargin))
      //I have to explicitly put my actor system into the implicit params for this
      val probe = TestProbe()(otherSystem)

      val props = launcherProps(Seq("bash", "-c", "exit 1"))

      val actor = otherSystem.actorOf(props)

      probe.watch(actor)

      //This is a bit more gross when you can't use the implicit actor system
      EventFilter.error("Repose Node Execution terminated abnormally. Value: 1", occurrences = 1).
        intercept(actor ! Initialize(testNode))(otherSystem)

      //Expect death watch to note that it died
      probe.expectMsgPF(3 seconds) {
        case Terminated(theActor) => {
          theActor should equal(actor) //make sure we got a death from our actor
        }
      }

      //Coincidentally, this ends up nuking the actor system
      otherSystem.isTerminated shouldBe true
    }

  }
}
