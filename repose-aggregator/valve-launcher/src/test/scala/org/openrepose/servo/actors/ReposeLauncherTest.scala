package org.openrepose.servo.actors

import akka.actor.ActorSystem
import akka.testkit.TestKit
import com.typesafe.config.ConfigFactory
import org.junit.runner.RunWith
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
      pending
    }
    it("sets environment variables that are passed to it") {
      pending
    }
    it("will log standard out to info") {
      pending
    }
    it("will log standard error out to warn") {
      pending
    }
    it("terminates the command and closes the streams when dying") {
      pending
    }
    it("logs an error and terminates if the command executes abnormally") {
      pending
    }
  }
}
