package org.openrepose.servo

import java.io._
import java.util.concurrent.ConcurrentSkipListSet

import com.typesafe.config.{Config, ConfigFactory}
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.{BeforeAndAfterAll, FunSpec, Matchers}

import scala.concurrent.{Await, Future}
import scala.io.Source

@RunWith(classOf[JUnitRunner])
class ServoTest extends FunSpec with Matchers with TestUtils with BeforeAndAfterAll {

  val defaultConfig = ConfigFactory.load()

  val servoVersion = defaultConfig.getString("version")

  //Keep a set of the things I need to clean up
  val cleanUpDirs = new ConcurrentSkipListSet[File]()

  //Need an execution context for my futures

  import scala.concurrent.ExecutionContext.Implicits.global
  import scala.concurrent.duration._

  override protected def afterAll() = {
    import scala.collection.JavaConverters._
    cleanUpDirs.asScala.foreach { f =>
      deleteRecursive(f.toPath)
    }
  }

  def autoCleanTempDir(prefix:String):String = {
    val dir = tempDir(prefix).toFile
    cleanUpDirs.add(dir)
    dir.getAbsolutePath
  }

  def afterExecution(args: Array[String] = Array.empty[String], callback: (String, String, Int) => Unit) = {
    val stdout = new ByteArrayOutputStream()
    val stderr = new ByteArrayOutputStream()
    //Servo doesn't yet do anything with stdin, so we can ignore that here
    val stdin = System.in

    val servo = new Servo()

    val exitStatus = servo.execute(args, stdin, new PrintStream(stdout), new PrintStream(stderr), defaultConfig)

    val error = new String(stderr.toByteArray)
    val output = new String(stdout.toByteArray)

    callback(output, error, exitStatus)

    servo.shutdown()
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

    def servoConfig(systemModelResource: String, containerConfigResource:String = "/servoTesting/with-keystore.xml")(testFunc: (String, File, Config) => Unit) = {
      //Create the 1-node system model
      val configRoot = autoCleanTempDir("servo").toString
      val systemModelContent = resourceContent(systemModelResource)
      val containerConfigContent = resourceContent(containerConfigResource)
      val log4jContent = resourceContent("/servoTesting/log4j.properties")
      writeSystemModel(configRoot, systemModelContent)
      writeContainerConfig(configRoot, containerConfigContent)
      writeFileContent(new File(configRoot, "log4j.properties"), log4jContent)

      //Create a bash script that outputs test data and just runs
      val fakeRepose = resourceContent("/servoTesting/fakeRepose.sh")
      val tmpBash = tempFile("fakeRepose", ".sh")
      tmpBash.setExecutable(true)
      writeFileContent(tmpBash, fakeRepose)

      val tmpOutput = tempFile("fakeRepose", ".out")

      //Create a config object to merge in
      //TODO: NONE OF THIS WORKS!
      val config = ConfigFactory.parseString(
        s"""
            |baseCommand = [${tmpBash.getAbsolutePath}, ${tmpOutput.getAbsolutePath}]
          """.stripMargin).withFallback(defaultConfig)

      testFunc(configRoot, tmpOutput, config)
    }

    def singleNodeServoConfig(testFunc: (String, File, Config) => Unit) = {
      servoConfig("/servoTesting/system-model-1.cfg.xml")(testFunc)
    }

    def failConfig(testFunc: (String, File, Config) => Unit) = {
      servoConfig("/servoTesting/system-model-fail.cfg.xml")(testFunc)
    }

    it("uses the log4j.properties from the configRoot") {
      singleNodeServoConfig { (configRoot, tmpOutput, config) =>
        //Create a log4j.properties in the config root
        val logFile = tempFile("log4jlogging", ".log")
        val log4jFile = new File(configRoot, "log4j.properties")
        log4jFile.deleteOnExit()
        val log4jContent = resourceContent("/servoTesting/targetedlog4j.properties").replace("${LOG_FILE}", logFile.getAbsolutePath)
        writeFileContent(log4jFile, log4jContent)

        val servo = new Servo()
        val exitValue = Future {
          servo.execute(Array("--config-file", configRoot.toString), System.in, System.out, System.err, config)
        }

        Thread.sleep(500)
        servo.shutdown()

        val logLines = Source.fromFile(logFile).getLines().toList

        info("LOG LINES")
        info(logLines mkString "\n")

        logLines shouldNot be(empty)

        Await.result(exitValue, 1 second) shouldBe 0
      }
    }
    describe("For a good single node configuration") {
      it("executes the command to start a Jetty with the Repose War on the specified port") {
        singleNodeServoConfig { (configRoot, tmpOutput, config) =>
          //Have to throw my executable into a thread so I can asynchronously do stuff
          //Just somewhere to do work
          val servo = new Servo()
          val exitValue = Future {
            servo.execute(Array("--config-file", configRoot.toString), System.in, System.out, System.err, config)
          }

          Thread.sleep(500)
          servo.shutdown()

          val lines = Source.fromFile(tmpOutput).getLines().toList
          lines shouldNot be(empty)
          //Check the arguments for what we need
          lines.filter(_.startsWith("ARGS:")).map { l =>
            //This should be the ARGS line
            note(s"ARGS: $l")
            l should include(config.getString("launcherPath"))
            l should include(config.getString("reposeWarLocation"))

            //the clusterID and nodeID are set in system properties
            l should include("-Drepose-node-id=repose_node1")
            l should include("-Drepose-cluster-id=repose")
          }

          Await.result(exitValue, 1 second) shouldBe 0
        }
      }
      it("outputs to stdout the settings it's going to use to start valves") {
        singleNodeServoConfig { (configRoot, tmpOutput, config) =>
          val stdoutContent = new ByteArrayOutputStream()
          val stdout = new PrintStream(stdoutContent)

          val servo = new Servo()
          val exitValue = Future {
            servo.execute(Array("--config-file", configRoot.toString), System.in, stdout, System.err, config)
          }

          //After it's been started
          //Run for a very short bit to get some information into my files
          Thread.sleep(250)
          servo.shutdown()

          val content = new String(stdoutContent.toByteArray)
          content should include(s"Using ${configRoot.toString} as configuration root")
          content should include(s"Launching with SSL validation")

          Await.result(exitValue, 1 second) shouldBe 0
        }
      }
      it("passes through REPOSE_JVM_OPTS through as JVM_OPTS") {
        singleNodeServoConfig { (configRoot, tmpOutput, config) =>
          val newConfig = ConfigFactory.parseString(
            """
              |reposeOpts = "some repose jvm options would be here"
            """.stripMargin).withFallback(config)

          val servo = new Servo()
          val exitValue = Future {
            servo.execute(Array("--config-file", configRoot.toString), System.in, System.out, System.err, newConfig)
          }

          //After it's been started
          Thread.sleep(250)
          servo.shutdown()

          //Get output lines
          val lines = Source.fromFile(tmpOutput).getLines().toList
          lines shouldNot be(empty)
          //Check the environment variables for our cluster ID/node ID
          val clusterId = lines.filter(_.startsWith("JAVA_OPTS"))
          clusterId.size shouldBe 1
          clusterId.head shouldBe "JAVA_OPTS=some repose jvm options would be here"

          Await.result(exitValue, 1 second) shouldBe 0
        }
      }
      it("warns when JAVA_OPTS are set") {
        singleNodeServoConfig { (configRoot, tmpOutput, config) =>
          val newConfig = ConfigFactory.parseString(
            """
              |javaOpts = "some jvm options would be here ALERT!"
            """.stripMargin).withFallback(config)

          //Not logging the JVM_OPTS warning, it's just going to system err
          val stdErr = new ByteArrayOutputStream()
          val stdErrStream = new PrintStream(stdErr)

          val servo = new Servo()
          val exitValue = Future {
            servo.execute(Array("--config-file", configRoot.toString), System.in, System.out, stdErrStream, newConfig)
          }

          //After it's been started
          Thread.sleep(250)
          servo.shutdown()

          //Should have a log message telling me about how I set JVM opts!
          val stdErrOutput = new String(stdErr.toByteArray)

          stdErrOutput shouldNot be("")
          stdErrOutput should include("WARNING: JAVA_OPTS set! Those apply to Servo! Use REPOSE_OPTS instead!")

          Await.result(exitValue, 1 second) shouldBe 0
        }
      }
      it("warns when XX_CONFIGURATION_OVERRIDE_FILE_XX override is set") {
        singleNodeServoConfig { (configRoot, tmpOutput, config) =>
          //Create a log4j.properties in the config root
          val logFile = tempFile("log4jlogging", ".log")
          val log4jFile = new File(configRoot, "log4j.properties")
          log4jFile.deleteOnExit()
          val log4jContent = resourceContent("/servoTesting/targetedlog4j.properties").replace("${LOG_FILE}", logFile.getAbsolutePath)
          writeFileContent(log4jFile, log4jContent)

          //Config file to pass on the command line
          val overridePath = "/some/path/to/the/jetty-runner-version.jar"
          val overrideLocation = "/some/path/to/the/repose-version.war"
          val overrideFile = tempFile("overrideFile", ".conf")
          writeFileContent(overrideFile,
          "launcherPath = "+overridePath+"\n"+
          "reposeWarLocation = "+overrideLocation)

          //The override warning is sent to system err and logged
          val stdErr = new ByteArrayOutputStream()
          val stdErrStream = new PrintStream(stdErr)

          val servo = new Servo()
          val exitValue = Future {
            servo.execute(Array("--config-file", configRoot.toString, "--XX_CONFIGURATION_OVERRIDE_FILE_XX", overrideFile.getAbsolutePath),
              System.in, System.out, stdErrStream, config)
          }

          //After it's been started
          Thread.sleep(500)
          servo.shutdown()

          //Should have an error message about the overridden configuration!
          val stdErrOutput = new String(stdErr.toByteArray)

          info("STD ERROR")
          info(stdErrOutput)

          stdErrOutput shouldNot be("")
          stdErrOutput should include("WARNING: XX_CONFIGURATION_OVERRIDE_FILE_XX set! It is usually bad to override this!!!")

          //Should have a log message about the overridden configuration!
          val logLines = Source.fromFile(logFile).getLines().toList

          info("LOG LINES")
          info(logLines mkString "\n")

          logLines shouldNot be(empty)
          logLines.exists(s=>s.contains("WARNING: XX_CONFIGURATION_OVERRIDE_FILE_XX set! It is usually bad to override this!!!")) shouldBe true

          val lines = Source.fromFile(tmpOutput).getLines().toList
          lines shouldNot be(empty)
          //Should have the overridden values in the arguments
          lines.filter(_.startsWith("ARGS:")).map { l =>
            info(s"ARGS: $l")
            l should include("-jar "+overridePath)
            l should include(overrideLocation)
          }

          Await.result(exitValue, 1 second) shouldBe 0
        }
      }
    }
    describe("Failing to find nodes in the config file") {
      it("outputs a failure message and exits 1 if it cannot find any local nodes to start") {
        failConfig { (configRoot, tmpOutput, config) =>
          val stdoutContent = new ByteArrayOutputStream()
          val stderr = new PrintStream(stdoutContent)

          val servo = new Servo()
          val exitValue = Future {
            servo.execute(Array("--config-file", configRoot.toString), System.in, System.out, stderr, config)
          }

          //After it's been started
          Thread.sleep(250)
          servo.shutdown()

          val output = new String(stdoutContent.toByteArray)
          output should include("No local node(s) found!")
          Await.result(exitValue, 1 second) shouldNot be(0)
        }
      }
      it("outputs a failure message and exits 1 if it cannot find the config file") {
        failConfig { (configRoot, tmpOutput, config) =>
          val stdoutContent = new ByteArrayOutputStream()
          val stderr = new PrintStream(stdoutContent)

          val servo = new Servo()
          val exitValue: Future[Int] = Future {
            servo.execute(Array("--config-file", configRoot.toString), System.in, System.out, stderr, config)
          }

          //After it's been started
          Thread.sleep(250)
          servo.shutdown()

          val output = new String(stdoutContent.toByteArray)
          //TODO: maybe this should be a better error message, but I think it's good enough
          output should include("No local node(s) found!")
          //Ensure our exit value is valid
          Await.result(exitValue, 1 second) shouldNot be(0)
        }
      }
    }
    describe("for a good multi-local-node configuration") {
      it("starts those multiple nodes") {
        servoConfig("/servoTesting/system-model-2.cfg.xml") { (configRoot, tmpOutput, config) =>
          val servo = new Servo()

          val stdoutContent = new ByteArrayOutputStream()
          val stdout = new PrintStream(stdoutContent)

          val exitValue = Future {
            servo.execute(Array("--config-file", configRoot.toString), System.in, stdout, System.err, config)
          }

          //After it's been started
          Thread.sleep(250)
          servo.shutdown()

          //Looking at stdout to report on the local nodes it started!
          stdout.flush()

          val string = new String(stdoutContent.toByteArray)
          string should include("Starting 2 local nodes!")
          string should include("Starting local node repose_node1 in cluster repose")
          string should include("Starting local node repose_node2 in cluster repose")

          Await.result(exitValue, 1 second) shouldBe 0
        }
      }
    }
  }
}
