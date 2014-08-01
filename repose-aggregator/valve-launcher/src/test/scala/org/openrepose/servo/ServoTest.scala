package org.openrepose.servo

import java.io.{PrintStream, ByteArrayOutputStream}

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.{FunSpec, Matchers}

@RunWith(classOf[JUnitRunner])
class ServoTest extends FunSpec with Matchers {

  describe("Servo, the Repose Valve Launcher") {
    it("prints out a usage message when given unknown parameters") {
      val stdout = new ByteArrayOutputStream()
      val stderr = new ByteArrayOutputStream()
      val stdin = System.in //I think this is okay for testing

      Servo.execute(Array("--help"), stdin, new PrintStream(stdout), new PrintStream(stderr))

      val error = new String(stderr.toByteArray)

      error should include("Usage: servo [options]")
    }
    it("outputs to stdout the settings it's going to use to start valves") {
      val stdout = new ByteArrayOutputStream()
      val stderr = new ByteArrayOutputStream()
      val stdin = System.in //I think this is okay for testing

      Servo.execute(Array.empty[String], stdin, new PrintStream(stdout), new PrintStream(stderr))

      val output = new String(stdout.toByteArray)

      output should include("Using /etc/repose as configuration root")
      output should include("Launching with SSL validation")
    }
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
