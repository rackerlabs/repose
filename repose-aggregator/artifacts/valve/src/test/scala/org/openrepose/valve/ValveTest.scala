/*
 * _=_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_=
 * Repose
 * _-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-
 * Copyright (C) 2010 - 2015 Rackspace US, Inc.
 * _-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * =_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_=_
 */
package org.openrepose.valve

import java.io.{ByteArrayOutputStream, File, PrintStream}
import java.util.concurrent.ConcurrentSkipListSet

import com.typesafe.config.ConfigFactory
import org.apache.http.HttpResponse
import org.apache.http.client.methods.HttpGet
import org.apache.http.impl.client.HttpClients
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.{BeforeAndAfterAll, FunSpec, Matchers}
import org.slf4j.LoggerFactory

import scala.concurrent.{Await, Future}
import scala.util.{Failure, Success}

@RunWith(classOf[JUnitRunner])
class ValveTest extends FunSpec with Matchers with TestUtils with BeforeAndAfterAll {

  val LOG = LoggerFactory.getLogger(this.getClass)

  import scala.concurrent.ExecutionContext.Implicits.global
  import scala.concurrent.duration._

  val defaultConfig = ConfigFactory.load("valve-config.conf")
  val myVersion = defaultConfig.getString("myVersion")
  val jettyVersion = defaultConfig.getString("jettyVersion")

  val cleanUpDirs = new ConcurrentSkipListSet[File]()

  def postExecution(args: Array[String] = Array.empty[String], callback: (String, String, Int) => Unit) = {
    val stdout = new ByteArrayOutputStream()
    val stderr = new ByteArrayOutputStream()

    //Valve doesn't care about stdin, so we'll just hook that up
    val valve = new Valve()

    val exitStatus = Console.withIn(System.in)(Console.withOut(new PrintStream(stdout))(Console.withErr(new PrintStream(stderr))(
      valve.execute(args, defaultConfig))))

    val error = new String(stderr.toByteArray)
    val output = new String(stdout.toByteArray)

    callback(output, error, exitStatus)

    valve.shutdown()
  }

  def valveConfig(systemModelResource: String,
                  containerConfigResource: String = "/valveTesting/without-keystore.cfg.xml")(testFunc: (String, File) => Unit) = {
    val configRoot = autoCleanTempDir("valve").toString
    val systemModelContent = resourceContent(systemModelResource)
    //I have to parse the Container Config Content to make sure it knows about the path to the log4j file
    val containerConfigContent = resourceContent(containerConfigResource).replaceAll("\\$\\{configRootPath\\}", configRoot)

    val log4jContent = resourceContent("/valveTesting/log4j2.xml")

    writeSystemModel(configRoot, systemModelContent)
    writeContainerConfig(configRoot, containerConfigContent)
    writeFileContent(new File(configRoot, "log4j2.xml"), log4jContent)

    val tmpOutput = tempFile("fakeRepose", ".out")

    //There's no config to override, or anything, we're just going to hold on to our butts

    //call the test function ...
    testFunc(configRoot, tmpOutput)
  }

  def autoCleanTempDir(prefix: String): String = {
    val dir = tempDir(prefix).toFile
    cleanUpDirs.add(dir)
    dir.getAbsolutePath
  }

  describe("Command line argument parsing short circuits") {
    it("prints out a usage message when given --help and exits 1") {
      postExecution(Array("--help"), (output, error, exitStatus) => {
        output should include("Usage: java -jar repose.jar [options]")
        exitStatus shouldBe 1
      })
    }
    it(s"prints out the current version when given --version and exits 1") {
      postExecution(Array("--version"), (output, error, exitStatus) => {
        output should include(s"Repose Valve: $myVersion on Jetty $jettyVersion")
        exitStatus shouldBe 1
      })
    }
    it("prints out the available SSL protocols and ciphers when given --show-ssl-params and exits 1") {
      postExecution(Array("--show-ssl-params"), (output, error, exitStatus) => {
        output should include("Default enabled SSL Protocols:")
        output should include("Default enabled SSL Ciphers:")
        output should include("All available SSL Protocols:")
        output should include("All available SSL Ciphers:")
        //NOTE: can't actually verify the ciphers and protocols, because we don't know what they'll be... JVM dependent
        exitStatus shouldBe 1
      })
    }
  }

  /**
   * Need to keep track of stuff to clean up, so this is used to clean up the mess
   */
  override protected def afterAll() = {
    import scala.collection.JavaConverters._
    cleanUpDirs.asScala.foreach { f =>
      deleteRecursive(f.toPath)
    }
  }

  ignore("for a good single node configuration") {
    it("starts listening on the configured port") {
      //Starts up a real jetty
      //verify that I don't get a connection failed on that port, it should listen regardless (I hope?)
      valveConfig("/valveTesting/1node/system-model-1.cfg.xml") { (configRoot, tmpOutput) =>
        //TODO
        val valve = new Valve()
        val exitValue = Future {
          Console.withIn(System.in)(Console.withOut(System.out)(Console.withErr(System.err)(
            valve.execute(Array("--config-file", configRoot.toString), defaultConfig))))
        }

        exitValue onComplete {
          case Failure(cause) =>
            throw cause
          case Success(exitCode) =>
            LOG.info(s"Valve exited with code: $exitCode")
        }

        //Verify that the thing is listening on the configured port!
        //TODO: something on 8080
        val httpClient = HttpClients.createDefault()

        val get = new HttpGet("http://localhost:8080")

        var response: HttpResponse = null
        while (response == null && !exitValue.isCompleted) {
          try {
            LOG.info(s"Trying an httpRequest: $get")
            response = httpClient.execute(get)
          } catch {
            case e: Exception => {
              Thread.sleep(500)
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
