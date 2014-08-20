package org.openrepose.servo

import java.io.{StringWriter, ByteArrayOutputStream, File, PrintStream}
import java.nio.charset.StandardCharsets
import java.nio.file.{Files, StandardOpenOption}

import com.typesafe.config.{Config, ConfigFactory}
import org.apache.log4j._
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.{BeforeAndAfterEach, FunSpec, Matchers}

import scala.concurrent.{ExecutionContext, Future}
import scala.io.Source
import scala.util.{Failure, Success}

@RunWith(classOf[JUnitRunner])
class ServoTest extends FunSpec with Matchers with TestUtils with BeforeAndAfterEach {

    val defaultConfig = ConfigFactory.load()

    val servoVersion = defaultConfig.getString("version")

    //Need an execution context for my futures

    import scala.concurrent.ExecutionContext.Implicits.global

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

        def servoConfig(systemModelResource: String)(testFunc: (String, File, Config) => Unit) = {
            //Create the 1-node system model
            val configRoot = tempDir("servo").toString
            val systemModelContent = resourceContent(systemModelResource)
            writeSystemModel(configRoot, systemModelContent)

            //Create a bash script that outputs test data and just runs
            val fakeRepose = resourceContent("/servoTesting/fakeRepose.sh")
            val tmpBash = tempFile("fakeRepose", ".sh")
            tmpBash.setExecutable(true)

            val tmpOutput = tempFile("fakeRepose", ".out")
            Files.write(tmpBash.toPath, fakeRepose.getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE)

            //Create a config object to merge in
            val config = ConfigFactory.parseString(
                s"""
            |executionCommand = [${tmpBash.getAbsolutePath}, ${tmpOutput.getAbsolutePath}]
          """.stripMargin).withFallback(defaultConfig)

            testFunc(configRoot, tmpOutput, config)
        }

        def singleNodeServoConfig(testFunc: (String, File, Config) => Unit) = {
            servoConfig("/servoTesting/system-model-1.cfg.xml")(testFunc)
        }

        def failConfig(testFunc: (String, File, Config) => Unit) = {
            servoConfig("/servoTesting/system-model-fail.cfg.xml")(testFunc)
        }

        describe("For a good single node configuration") {
            it("executes the command to start a Jetty with the Repose War on the specified port") {
                singleNodeServoConfig { (configRoot, tmpOutput, config) =>
                    //Have to throw my executable into a thread so I can asynchronously do stuff
                    //Just somewhere to do work
                    val servo = new Servo()
                    val exitValue = Future {
                        servo.execute(Array("--config-file", configRoot.toString), System.in, System.out, System.err, config)
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

                    Thread.sleep(500)
                    servo.shutdown()

                    val lines = Source.fromFile(tmpOutput).getLines().toList
                    lines shouldNot be(empty)
                    //Check the arguments for what we need
                    lines.filter(_.startsWith("ARGS:")).map { l =>
                        //This should be the ARGS line
                        note(s"ARGS: $l")
                        l should include("--port 8080")
                        l should include(config.getString("reposeWarLocation"))
                    }

                    //Check the environment variables for our cluster ID/node ID
                    val clusterId = lines.filter(_.startsWith("CLUSTER_ID"))
                    clusterId.size shouldBe 1
                    clusterId.head shouldBe "CLUSTER_ID=repose"

                    val nodeId = lines.filter(_.startsWith("NODE_ID"))
                    nodeId.size shouldBe 1
                    nodeId.head shouldBe "NODE_ID=repose_node1"
                }
            }
            it("outputs to stdout the settings it's going to use to start valves") {
                singleNodeServoConfig { (configRoot, tmpOutput, config) =>
                    val stdoutContent = new ByteArrayOutputStream()
                    val stdout = new PrintStream(stdoutContent)

                    val servo = new Servo()
                    val exitValue = Future {
                        servo.execute(Array("--config-file", configRoot.toString), System.in, stdout, System.err, config)
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

                    //After it's been started
                    //Run for a very short bit to get some information into my files
                    Thread.sleep(250)
                    servo.shutdown()

                    val content = new String(stdoutContent.toByteArray)
                    content should include(s"Using ${configRoot.toString} as configuration root")
                    content should include(s"Launching with SSL validation")
                    servo.shutdown()
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

                    //After it's been started
                    Thread.sleep(250)
                    servo.shutdown()

                    //Get output lines
                    val lines = Source.fromFile(tmpOutput).getLines().toList
                    lines shouldNot be(empty)
                    //Check the environment variables for our cluster ID/node ID
                    val clusterId = lines.filter(_.startsWith("JVM_OPTS"))
                    clusterId.size shouldBe 1
                    clusterId.head shouldBe "JVM_OPTS=some repose jvm options would be here"
                }
            }
            it("warns when JVM_OPTS are set") {
                singleNodeServoConfig { (configRoot, tmpOutput, config) =>
                    val newConfig = ConfigFactory.parseString(
                        """
                          |jvmOpts = "some jvm options would be here ALERT!"
                        """.stripMargin).withFallback(config)

                    //Get logging output and capture the named logger for servo
                    // There should be a warning log message regarding JVM opts!


                    //HOLY CRAP log4j1.2 is hard to find out information for
                    //This combination of stuff captures the log stuff for me in a very simple format proving that it does work
                    val sw = new StringWriter()
                    val writerAppender = new WriterAppender(new SimpleLayout(), sw)
                    BasicConfigurator.configure(writerAppender)

                    val servo = new Servo()
                    val exitValue = Future {
                        servo.execute(Array("--config-file", configRoot.toString), System.in, System.out, System.err, newConfig)
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

                    //After it's been started
                    Thread.sleep(250)
                    servo.shutdown()

                    //Should have a log message telling me about how I set JVM opts!
                    val logOutput = sw.getBuffer.toString

                    logOutput shouldNot be("")
                    logOutput should include("WARN - JVM_OPTS set! Those apply to Servo! Use REPOSE_OPTS instead!")
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
                    }.onComplete { t =>
                        info("Completion of servo!")
                        t match {
                            case Success(x) => {
                                fail("Servo should not have started up correctly!")
                            }
                            case Failure(x) => {
                                //I don't know if I care about the actual exception here
                            }
                        }
                    }

                    //After it's been started
                    Thread.sleep(250)
                    servo.shutdown()

                    val output = new String(stdoutContent.toByteArray)
                    output should include("No local node(s) found!")
                }
            }
            it("outputs a failure message and exits 1 if it cannot find the config file") {
                failConfig { (configRoot, tmpOutput, config) =>
                    val stdoutContent = new ByteArrayOutputStream()
                    val stderr = new PrintStream(stdoutContent)

                    val servo = new Servo()
                    val exitValue = Future {
                        servo.execute(Array("--config-file", configRoot.toString), System.in, System.out, stderr, config)
                    }.onComplete { t =>
                        info("Completion of servo!")
                        t match {
                            case Success(x) => {
                                fail("Servo should not have started up correctly!")
                            }
                            case Failure(x) => {
                                //I don't know if I care about the actual exception here
                            }
                        }
                    }

                    //After it's been started
                    Thread.sleep(250)
                    servo.shutdown()

                    val output = new String(stdoutContent.toByteArray)
                    //TODO: maybe this should be a better error message, but I think it's good enough
                    output should include("No local node(s) found!")
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
                    }.onComplete { t =>
                        info("Completion of servo!")
                        t match {
                            case Success(x) => {
                                //Servo should have started up correctly
                                x shouldBe 0
                            }
                            case Failure(x) => {
                                fail("Servo should not have failed!")
                            }
                        }
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
                }
            }
        }
    }

}
