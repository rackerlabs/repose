package org.openrepose.servo

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.{FunSpec, Matchers}

@RunWith(classOf[JUnitRunner])
class ServoTest extends FunSpec with Matchers {

  describe("Servo, the Repose Valve Launcher") {
    it("executes the command to start a Jetty with the Repose War on the specified port"){
      pending
    }
    it("executes several commands if there are several local instances configured") {
      pending
    }
    it("passes through REPOSE_JVM_OPTS through as JVM_OPTS") {
      pending
    }
    it("warns when JVM_OPTS are set") {
      pending
    }
    describe("watches the system-model.cfg.xml for changes to nodes") {
      it("starts new valves when they're added") {
        pending
      }
      it("stops orphaned valves when they're removed") {
        pending
      }
      it("does not restart valves if the ordering changed") {
        pending
      }
    }

  }
}
