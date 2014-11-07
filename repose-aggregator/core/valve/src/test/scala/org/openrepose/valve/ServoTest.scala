package org.openrepose.valve

import java.io.{PrintStream, ByteArrayOutputStream}

import org.junit.runner.RunWith
import org.scalatest.{Matchers, FunSpec}
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class ServoTest extends FunSpec with Matchers {

  def postExecution(args: Array[String] = Array.empty[String], callback: (String, String, Int) => Unit) = {
    val stdout = new ByteArrayOutputStream()
    val stderr = new ByteArrayOutputStream()

    //Valve doesn't care about stdin, so we'll just hook that up
    val stdin = System.in

    val valve = new Valve()

    val exitStatus = valve.execute(args, stdin, new PrintStream(stdout), new PrintStream(stderr))

    val error = new String(stderr.toByteArray)
    val output = new String(stdout.toByteArray)

    callback(output, error, exitStatus)

    valve.shutdown()
  }

  describe("Command line argument parsing short circuits") {
    it("prints out a usage message when given --help and exits 1") {
      postExecution(Array("--help"), (output, error, exitStatus) => {
        output should include("Usage: java -jar repose-valve.jar [options]")
        exitStatus shouldBe 1
      })
    }
    it("prints out the version when given --version and exits 1") {
      pending
    }
  }

  describe("for a good single node configuration") {
    it("starts listening on the configured port") {
      pending
    }
    it("outputs to stdout the settings it's going to hand to the individual jetties") {
      pending
    }
  }

  describe("for a good multi-local-node configuration") {
    it("starts listening on the configured ports") {
      pending
    }
  }

  describe("failing to start up") {
    it("outputs a failure message and exits 1 if no local nodes found") {
      pending
    }
    it("outputs a failure message and exits 1 if it cannot find the config file") {
      pending
    }
  }

}
