package org.openrepose.servo

import java.io.{PrintStream, ByteArrayOutputStream}

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.{FunSpec, Matchers}

@RunWith(classOf[JUnitRunner])
class ServoTest extends FunSpec with Matchers {

  def afterExecution(args: Array[String] = Array.empty[String], callback: (String, String) => Unit) = {
    val stdout = new ByteArrayOutputStream()
    val stderr = new ByteArrayOutputStream()
    //Servo doesn't yet do anything with stdin, so we can ignore that here
    val stdin = System.in

    Servo.execute(args, stdin, new PrintStream(stdout), new PrintStream(stderr))

    val error = new String(stderr.toByteArray)
    val output = new String(stdout.toByteArray)

    callback(output, error)
  }

  describe("Servo, the Repose Valve Launcher") {
    it("prints out a usage message when given unknown parameters") {
      afterExecution(Array("--help"), (output, error) => {
        error should include("Usage: java -jar servo.jar [options]")
      })
    }
    it("outputs to stdout the settings it's going to use to start valves") {
      afterExecution(callback = (output, error) => {
        output should include("Using /etc/repose as configuration root")
        output should include("Launching with SSL validation")
      })
    }
    describe("Failing to find nodes in the config file") {
      it("outputs a failure message if it cannot find any local nodes to start") {
        pending
      }
      it("outputs a failure message if it cannot find the config file") {
        pending
      }
    }
    it("executes the command to start a Jetty with the Repose War on the specified port") {
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
