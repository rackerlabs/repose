package org.openrepose.servo.actors

import java.io.File

import akka.actor.{PoisonPill, Terminated, ActorSystem}
import akka.testkit.{EventFilter, TestProbe, TestKit}
import com.typesafe.config.ConfigFactory
import org.junit.runner.RunWith
import org.openrepose.servo.actors.NodeStoreMessages.Initialize
import org.scalatest.junit.JUnitRunner
import org.scalatest.{FunSpecLike, Matchers, BeforeAndAfterAll}

import scala.io.Source

@RunWith(classOf[JUnitRunner])
class ReposeLauncherTest(_system: ActorSystem) extends TestKit(_system)
with FunSpecLike with Matchers with BeforeAndAfterAll {

  import scala.concurrent.duration._

  def this() = this(ActorSystem("ReposeLauncherSpec", ConfigFactory.parseString(
    """
      |akka.loggers = [akka.testkit.TestEventListener]
    """.stripMargin)))


  override def afterAll() = {
    TestKit.shutdownActorSystem(system)
  }

  describe("The Repose Launcher") {
    it("will execute the command and remain running until the command exits") {

      //Create a test probe to watch for DEATH
      val probe = TestProbe()

      //create an actor with that command
      val props = ReposeLauncher.props(List("bash", "-c", "sleep 1"))

      //Won't actually start until the initialize is sent
      val actor = system.actorOf(props)
      probe.watch(actor)

      actor ! Initialize("testCluster", "testNode")

      probe.expectMsgPF(3 seconds) {
        case Terminated(theActor) => {
          theActor should equal(actor) //make sure we got a death from our actor
        }
      }
    }
    it("will log standard out to info") {
      val probe = TestProbe()
      val props = ReposeLauncher.props(Seq("bash", "-c", "echo 'lololol'"))

      val actor = system.actorOf(props)

      EventFilter.info(pattern = ".*lololol.*", occurrences = 1) intercept {
        actor ! Initialize("testCluster", "testNode")
      }
    }
    it("sets passed in environment variables") {
      val probe = TestProbe()
      val props = ReposeLauncher.props(List("bash", "-c", "echo $CONFIG_ROOT"), Map("CONFIG_ROOT" -> "/etc/repose"))

      val actor = system.actorOf(props)

      EventFilter.info(message = "/etc/repose", occurrences = 1) intercept {
        actor ! Initialize("testCluster", "testNode")
      }
    }
    it("sets CLUSTER_ID and NODE_ID as environment variables") {
      val probe = TestProbe()
      val props = ReposeLauncher.props(Seq("bash", "-c", "echo ${CLUSTER_ID}:${NODE_ID}"))

      val actor = system.actorOf(props)

      EventFilter.info(message = "testCluster:testNode", occurrences = 1) intercept {
        actor ! Initialize("testCluster", "testNode")
      }

    }
    it("will log standard error out to warn") {
      val probe = TestProbe()
      val props = ReposeLauncher.props(Seq("bash", "-c", "echo >&2 'standardError'"))

      val actor = system.actorOf(props)

      EventFilter.warning("standardError", occurrences = 1) intercept {
        actor ! Initialize("testCluster", "testNode")
      }
    }
    it("terminates the command and closes the streams when dying") {
      val probe = TestProbe()
      //Std out constant noise!

      //Create a temp file
      val f = File.createTempFile("testing", "txt")

      val fileName = f.getAbsolutePath
      val props = ReposeLauncher.props(Seq("bash", "-c", "while true; do echo 'test' >> " + fileName + "; sleep 0.1; done"))

      val actor = system.actorOf(props)

      //Turn it on, it should start doing stuff
      actor ! Initialize("testCluster", "testNode")

      Thread.sleep(500)

      //TERMINATE IT
      actor ! PoisonPill //Kill the actor before the process dies

      //Within a second or so, the file should stop receiving "tests"
      def fileLines():Int =  Source.fromFile(f).getLines().count(_ => true)

      var lineCount = 0
      var sames = 0
      val duration = 2.seconds
      val maxTime = System.currentTimeMillis() + duration.toMillis
      while(sames < 4 && maxTime > System.currentTimeMillis()) {
        val newLineCount = fileLines()
        //println(s"old: $lineCount, new: $newLineCount")
        if(newLineCount == lineCount) {
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

    it("logs an error and terminates if the command executes abnormally") {
      val probe = TestProbe()

      val props = ReposeLauncher.props(Seq("bash", "-c", "exit 1"))

      val actor = system.actorOf(props)

      EventFilter.error("Command terminated abnormally. Value: 1", occurrences = 1) intercept {
        actor ! Initialize("testCluster", "testNode")
      }
    }
  }
}
