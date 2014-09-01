package org.openrepose.servo.actors

import akka.actor.{ActorSystem, PoisonPill, Props, Terminated}
import akka.event.Logging.Info
import akka.testkit.{EventFilter, TestKit, TestProbe}
import com.typesafe.config.ConfigFactory
import org.junit.runner.RunWith
import org.openrepose.servo.{CommandGenerator, ReposeNode, TestUtils}
import org.scalatest.junit.JUnitRunner
import org.scalatest.{BeforeAndAfterAll, FunSpecLike, Matchers}

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
  val configRootFile = tempDir("reposeLauncherTest").toFile
  val configRoot = configRootFile.getAbsolutePath

  override def afterAll() = {
    TestKit.shutdownActorSystem(system)
    deleteRecursive(configRootFile.toPath)
  }

  def launcherProps(preOpts: Seq[String],
                    env: Map[String, String] = Map.empty[String, String]): Props = {
    val cg = new CommandGenerator(preOpts ++ Seq("--"), configRoot, "launcherPath", "warFile")
    ReposeLauncher.props(cg.commandLine(testNode), env)
  }

  describe("The Repose Launcher") {
    it("will execute the command and remain running until the command exits") {

      //Create a test probe to watch for DEATH
      val probe = TestProbe()

      //create an actor with that command
      val props = launcherProps(Seq("bash", "-c", "sleep 1"))

      //As soon as you create one of these, it's fired up!
      val actor = system.actorOf(props)
      probe.watch(actor)

      probe.expectMsgPF(3 seconds) {
        case Terminated(theActor) => {
          theActor should equal(actor) //make sure we got a death from our actor
        }
      }
    }
    it("will log standard out to info") {
      val probe = TestProbe()
      val props = launcherProps(Seq("bash", "-c", "echo 'lololol'"))

      EventFilter.info(pattern = ".*lololol.*", occurrences = 1) intercept {
        val actor = system.actorOf(props)
      }
    }
    it("always specifies a --config for jetty configs") {
      val probe = TestProbe()
      val props = launcherProps(Seq("bash", "-c", "echo $@"))

      EventFilter.info(pattern = "--config .*_jetty.xml", occurrences = 1) intercept {
        val actor = system.actorOf(props)
      }
    }
    it("has the war file path at the end") {
      val probe = TestProbe()
      val props = launcherProps(Seq("bash", "-c", "echo $@"))

      EventFilter.info(pattern = "warFile$", occurrences = 1) intercept {
        val actor = system.actorOf(props)
      }
    }
    it("sets passed in environment variables") {
      val probe = TestProbe()
      val props = launcherProps(List("bash", "-c", "echo $ENV_VAR"), Map("ENV_VAR" -> "LOLWUT"))

      EventFilter.info(message = "LOLWUT", occurrences = 1) intercept {
        val actor = system.actorOf(props)
      }
    }
    it("sets the system properties: repose-node-id, repose-cluster-id and powerapi-config-directory") {
      val probe = TestProbe()
      val props = launcherProps(Seq("bash", "-c", "echo $@", "--"))


      EventFilter.custom({
        case Info(ref, clazz, msg: String)
          if msg.contains("-Drepose-node-id=") &&
            msg.contains("-Drepose-cluster-id") &&
            msg.contains("-Dpowerapi-config-directory") => true
      }, 1) intercept {
        val actor = system.actorOf(props)
      }


    }
    it("will log standard error out to warn") {
      val probe = TestProbe()
      val props = launcherProps(Seq("bash", "-c", "echo >&2 'standardError'"))


      EventFilter.warning("standardError", occurrences = 1) intercept {
        val actor = system.actorOf(props)
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

      //This is a bit more gross when you can't use the implicit actor system
      val actor = EventFilter.error("Repose Node Execution terminated abnormally. Value: 1", occurrences = 1).
        intercept(probe.watch(otherSystem.actorOf(props)))(otherSystem)

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
