package org.openrepose.servo

import java.io.{ByteArrayOutputStream, File, PrintStream}
import java.nio.charset.StandardCharsets
import java.nio.file.{StandardOpenOption, Paths, Files}

import com.typesafe.config.ConfigFactory
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.{FunSpec, Matchers}

import scala.concurrent.{ExecutionContext, Future}
import scala.io.Source
import scala.util.{Failure, Success}

@RunWith(classOf[JUnitRunner])
class ServoTest extends FunSpec with Matchers with TestUtils {

  val defaultConfig = ConfigFactory.load()

  val servoVersion = defaultConfig.getString("version")

  def afterExecution(args: Array[String] = Array.empty[String], callback: (String, String, Int) => Unit) = {
    val stdout = new ByteArrayOutputStream()
    val stderr = new ByteArrayOutputStream()
    //Servo doesn't yet do anything with stdin, so we can ignore that here
    val stdin = System.in

    val exitStatus = Servo.execute(args, stdin, new PrintStream(stdout), new PrintStream(stderr), defaultConfig)

    val error = new String(stderr.toByteArray)
    val output = new String(stdout.toByteArray)

    callback(output, error, exitStatus)
  }

  describe("Servo, the Repose Valve Launcher") {
    it("prints out a usage message when given --help and exits 1") {
      afterExecution(Array("--help"), (output, error, exitStatus) => {
        output should include("Usage: java -jar servo.jar [options]")
        exitStatus should be(1)
      })
    }
    it(s"prints out the version ($servoVersion) when given --version and exits 1") {
      afterExecution(Array("--version"), (output, error, exitStatus) => {
        output should include(s"Servo: ${defaultConfig.getString("version")}")
        exitStatus should be(1)
      })
    }

    describe("For a good single node configuration") {
      it("executes the command to start a Jetty with the Repose War on the specified port") {
        //Create the 1-node system model
        val configRoot = Files.createTempDirectory("servo").toString
        val systemModel = resourceContent("/servoTesting/system-model-1.cfg.xml")
        writeSystemModel(configRoot, systemModel)

        //Create a bash script that outputs test data and just runs
        val fakeRepose = resourceContent("/servoTesting/fakeRepose.sh")
        val tmpBash = File.createTempFile("fakeRepose", ".sh").toPath
        val tmpOutput = File.createTempFile("fakeRepose", ".out")
        Files.write(tmpBash, fakeRepose.getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE)

        //Create a config object to merge in
        val config = ConfigFactory.parseString(
          s"""
            |executionCommand = [${tmpBash.toFile.getAbsolutePath}, ${tmpOutput.getAbsolutePath}]
          """.stripMargin).withFallback(defaultConfig)

        info(s"Execution is: ${config.getStringList("executionCommand")}")

        //Have to throw my executable into a thread so I can asynchronously do stuff
        import ExecutionContext.Implicits.global
        //Just somewhere to do work
        val exitValue = Future {
          //    val exitStatus = Servo.execute(args, stdin, new PrintStream(stdout), new PrintStream(stderr), defaultConfig)
          Servo.execute(Array("--config-file", configRoot.toString), System.in, System.out, System.err, config)
        }.onComplete { t =>
          info("Completion of servo!")
          t match {
            case Success(x) => {
              x shouldBe 0
            }
            case Failure(x) => {
              fail("Servo didn't start up correctly", x)
            }
          }
        }

        val lines = Source.fromFile(tmpOutput).getLines().toList
        lines shouldNot be(empty)
        //Check the arguments for what we need
        lines.filter(_.startsWith("ARGS:")).map { l =>
          //This should be the ARGS line
          l should include("--port 8080")
        }

        //Check the environment variables for our cluster ID/node ID
        lines.filter(_.startsWith("CLUSTER_ID")).map { l =>
          l should be("CLUSTER_ID=repose")
        }

        lines.filter(_.startsWith("NODE_ID")).map { l =>
          l should be("NODE_ID=repose_node1")
        }

        //tell servo to stop.
        Servo.shutdown()


      }
      it("outputs to stdout the settings it's going to use to start valves") {
        pending
      }
      it("passes through REPOSE_JVM_OPTS through as JVM_OPTS") {
        pending
      }
      it("warns when JVM_OPTS are set") {
        pending
      }
    }
    describe("Failing to find nodes in the config file") {
      it("outputs a failure message and exits 1if it cannot find any local nodes to start") {
        pending
      }
      it("outputs a failure message and exits 1 if it cannot find the config file") {
        pending
      }
    }
    describe("for a good multi-local-node configuration") {
      it("starts those multiple nodes") {
        pending
      }
    }
  }
}
