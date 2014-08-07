package org.openrepose.servo.actors

import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestKit}
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.{FunSpecLike, Matchers}

@RunWith(classOf[JUnitRunner])
class SystemModelWatcherTest(_system:ActorSystem) extends TestKit(_system) with ImplicitSender with FunSpecLike with Matchers {

  def this() = this(ActorSystem("SystemModelWatcherSpec"))

  describe("System Model Watcher Actor watches the configured directory for changes") {
    it("will notify when something has changed with a changed message") {
      pending
    }
    it("will not notify if there are no changes") {
      pending
    }
  }
}
