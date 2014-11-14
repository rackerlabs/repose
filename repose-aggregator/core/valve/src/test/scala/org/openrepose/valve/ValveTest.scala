package org.openrepose.valve

import java.io.{File, PrintStream, ByteArrayOutputStream}
import java.util.concurrent.ConcurrentSkipListSet

import com.typesafe.config.{Config, ConfigFactory}
import org.apache.http.HttpResponse
import org.apache.http.client.methods.HttpGet
import org.apache.http.impl.client.DefaultHttpClient
import org.junit.runner.RunWith
import org.scalatest.{BeforeAndAfterAll, Matchers, FunSpec}
import org.scalatest.junit.JUnitRunner

import scala.concurrent.{Await, Future}

@RunWith(classOf[JUnitRunner])
class ValveTest extends FunSpec with Matchers with TestUtils with BeforeAndAfterAll {

  import scala.concurrent.ExecutionContext.Implicits.global
  import scala.concurrent.duration._

  val defaultConfig = ConfigFactory.load("valve-config.conf")
  val myVersion = defaultConfig.getString("myVersion")
  val jettyVersion = defaultConfig.getString("jettyVersion")

  val cleanUpDirs = new ConcurrentSkipListSet[File]()

  /**
   * Need to keep track of stuff to clean up, so this is used to clean up the mess
   */
  override protected def afterAll() = {
    import scala.collection.JavaConverters._
    cleanUpDirs.asScala.foreach { f =>
      deleteRecursive(f.toPath)
    }
  }

  def autoCleanTempDir(prefix: String): String = {
    val dir = tempDir(prefix).toFile
    cleanUpDirs.add(dir)
    dir.getAbsolutePath
  }


  def postExecution(args: Array[String] = Array.empty[String], callback: (String, String, Int) => Unit) = {
    val stdout = new ByteArrayOutputStream()
    val stderr = new ByteArrayOutputStream()

    //Valve doesn't care about stdin, so we'll just hook that up
    val stdin = System.in

    val valve = new Valve()

    val exitStatus = valve.execute(args, stdin, new PrintStream(stdout), new PrintStream(stderr), defaultConfig)

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
    it(s"prints out the current version when given --version and exits 1") {
      postExecution(Array("--version"), (output, error, exitStatus) => {
        output should include(s"Repose Valve: $myVersion on Jetty $jettyVersion")
        exitStatus shouldBe 1
      })
    }
  }

  def valveConfig(systemModelResource: String,
                  containerConfigResource: String = "/valveTesting/without-keystore.xml")(testFunc: (String, File) => Unit) = {
    val configRoot = autoCleanTempDir("valve").toString
    val systemModelContent = resourceContent(systemModelResource)
    val containerConfigContent = resourceContent(containerConfigResource)
    val log4jContent = resourceContent("/valveTesting/log4j2.xml")

    writeSystemModel(configRoot, systemModelContent)
    writeContainerConfig(configRoot, containerConfigContent)
    writeFileContent(new File(configRoot, "log4j.properties"), log4jContent)

    val tmpOutput = tempFile("fakeRepose", ".out")

    //There's no config to override, or anything, we're just going to hold on to our butts

    //call the test function ...
    testFunc(configRoot, tmpOutput)
  }

  describe("for a good single node configuration") {
    it("starts listening on the configured port") {
      //Starts up a real jetty
      //verify that I don't get a connection failed on that port, it should listen regardless (I hope?)
      valveConfig("/valveTesting/1node/system-model-1.cfg.xml") { (configRoot, tmpOutput) =>
        //TODO
        val valve = new Valve()
        val exitValue = Future {
          valve.execute(Array("--config-file", configRoot.toString), System.in, System.out, System.err, defaultConfig)
        }

        //Verify that the thing is listening on the configured port!
        //TODO: something on 8080
        val httpClient = new DefaultHttpClient()

        val get = new HttpGet("http://localhost:8080")

        var response: HttpResponse = null
        while (response == null) {
          try {
            response = httpClient.execute(get)
          } catch {
            case e: Exception => {
              Thread.sleep(100)
            }
          }
        }

        valve.shutdown() //Terminate it!

        Await.result(exitValue, 1 second) shouldBe 0
      }
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

  describe("failing to start up, outputs a failure message and exits 1 ") {
    it("if no local nodes found") {
      pending
    }
    it("if it cannot find the config file") {
      pending
    }
    it("if ssl is specified, but no keystore is given") {
      pending
    }
  }

}
