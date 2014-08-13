package org.openrepose.servo.actors

import akka.actor.{Terminated, ActorSystem}
import akka.testkit.{EventFilter, TestProbe, TestKit}
import com.typesafe.config.ConfigFactory
import org.junit.runner.RunWith
import org.openrepose.servo.actors.NodeStoreMessages.Initialize
import org.scalatest.junit.JUnitRunner
import org.scalatest.{FunSpecLike, Matchers, BeforeAndAfterAll}

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
    it("will execute the command and remain running until the command exits"){

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
    it("will log standard error out to warn") {
      val probe = TestProbe()
      val props = ReposeLauncher.props(Seq("bash", "-c", "echo >&2 'standardError'"))

      val actor = system.actorOf(props)

      EventFilter.warning("standardError", occurrences = 1) intercept {
        actor ! Initialize("testCluster", "testNode")
      }
    }
    it("terminates the command and closes the streams when dying") {
      pending
    }
    it("logs an error and terminates if the command executes abnormally") {
      pending
    }
  }
}
