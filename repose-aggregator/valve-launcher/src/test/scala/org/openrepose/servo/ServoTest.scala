package org.openrepose.servo

import java.io.{ByteArrayOutputStream, File, PrintStream}
import java.nio.file.Files

import com.typesafe.config.ConfigFactory
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.{FunSpec, Matchers}

@RunWith(classOf[JUnitRunner])
class ServoTest extends FunSpec with Matchers {

  val config = ConfigFactory.load()

  val servoVersion = config.getString("version")

  def afterExecution(args: Array[String] = Array.empty[String], callback: (String, String, Int) => Unit) = {
    val stdout = new ByteArrayOutputStream()
    val stderr = new ByteArrayOutputStream()
    //Servo doesn't yet do anything with stdin, so we can ignore that here
    val stdin = System.in

    val exitStatus = Servo.execute(args, stdin, new PrintStream(stdout), new PrintStream(stderr))

    val error = new String(stderr.toByteArray)
    val output = new String(stdout.toByteArray)

    callback(output, error, exitStatus)
  }

  describe("Servo, the Repose Valve Launcher") {
    it("prints out a usage message when given unknown parameters") {
      afterExecution(Array("--help"), (output, error, exitStatus) => {
        output should include("Usage: java -jar servo.jar [options]")
        exitStatus should be(1)
      })
    }
    it(s"prints out the version ($servoVersion) when given --version and exits 1") {
      afterExecution(Array("--version"), (output, error, exitStatus) => {
        output should include(s"Servo: ${config.getString("version")}")
        exitStatus should be(1)
      })
    }
    it("outputs to stdout the settings it's going to use to start valves") {
      //Needs a solid start up
      pending
    }
    describe("Failing to find nodes in the config file") {
      it("outputs a failure message and exits 1if it cannot find any local nodes to start") {
        pending
      }
      it("outputs a failure message and exits 1 if it cannot find the config file") {
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
