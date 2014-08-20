package org.openrepose.servo.actors

import java.io.File

import akka.actor.{PoisonPill, Terminated, ActorSystem}
import akka.testkit.{EventFilter, TestProbe, TestKit}
import com.typesafe.config.ConfigFactory
import org.junit.runner.RunWith
import org.openrepose.servo.{TestUtils, ReposeNode}
import org.openrepose.servo.actors.NodeStoreMessages.Initialize
import org.scalatest.junit.JUnitRunner
import org.scalatest.{FunSpecLike, Matchers, BeforeAndAfterAll}

import scala.io.Source

@RunWith(classOf[JUnitRunner])
class ReposeLauncherTest(_system: ActorSystem) extends TestKit(_system)
with FunSpecLike with Matchers with BeforeAndAfterAll with TestUtils {

    import scala.concurrent.duration._

    def this() = this(ActorSystem("ReposeLauncherSpec", ConfigFactory.parseString(
        """
          |akka.loggers = [akka.testkit.TestEventListener]
        """.stripMargin)))


    val testNode = ReposeNode("testCluster", "testNode", "localhost", Some(8080), None)

    val fakeWarPath = "/path/to/war/file"

    override def afterAll() = {
        TestKit.shutdownActorSystem(system)
    }

    describe("The Repose Launcher") {
        it("will execute the command and remain running until the command exits") {

            //Create a test probe to watch for DEATH
            val probe = TestProbe()

            //create an actor with that command
            val props = ReposeLauncher.props(Seq("bash", "-c", "sleep 1"), warFilePath = fakeWarPath)

            //Won't actually start until the initialize is sent
            val actor = system.actorOf(props)
            probe.watch(actor)

            actor ! Initialize(testNode)

            probe.expectMsgPF(3 seconds) {
                case Terminated(theActor) => {
                    theActor should equal(actor) //make sure we got a death from our actor
                }
            }
        }
        it("will log standard out to info") {
            val probe = TestProbe()
            val props = ReposeLauncher.props(Seq("bash", "-c", "echo 'lololol'"), warFilePath = fakeWarPath)

            val actor = system.actorOf(props)

            EventFilter.info(pattern = ".*lololol.*", occurrences = 1) intercept {
                actor ! Initialize(testNode)
            }
        }
        it("sets the command line parameter --port when given an HTTP port (not https)") {
            val probe = TestProbe()
            val props = ReposeLauncher.props(Seq("bash", "-c", "echo $@", "--"), warFilePath = fakeWarPath)

            val actor = system.actorOf(props)
            EventFilter.info(pattern = "--port 8080", occurrences = 1) intercept {
                actor ! Initialize(testNode)
            }
        }
        it("appends the war file path to any command line args") {
            val probe = TestProbe()
            val props = ReposeLauncher.props(Seq("bash", "-c", "echo $@", "--"), warFilePath = fakeWarPath)

            val actor = system.actorOf(props)
            EventFilter.info(pattern = fakeWarPath, occurrences = 1) intercept {
                actor ! Initialize(testNode)
            }
        }
        it("Generates the necessary jetty configuration when given an HTTPS port (not http)") {
            //TODO: how does this even work?
            // I have to generate a jetty configuration file and pass it to the launcher for this part
            // But that also means I have to find a SSL cert and SSL key, no clue how existing repose got those...
            pending
        }
        it("Generates the necessary jetty configuration when both HTTP and HTTPS ports are specified") {
            pending
        }
        it("sets passed in environment variables") {
            val probe = TestProbe()
            val props = ReposeLauncher.props(List("bash", "-c", "echo $CONFIG_ROOT"), Map("CONFIG_ROOT" -> "/etc/repose"), warFilePath = fakeWarPath)

            val actor = system.actorOf(props)

            EventFilter.info(message = "/etc/repose", occurrences = 1) intercept {
                actor ! Initialize(testNode)
            }
        }
        it("sets CLUSTER_ID and NODE_ID as environment variables") {
            val probe = TestProbe()
            val props = ReposeLauncher.props(Seq("bash", "-c", "echo ${CLUSTER_ID}:${NODE_ID}"), warFilePath = fakeWarPath)

            val actor = system.actorOf(props)

            EventFilter.info(message = "testCluster:testNode", occurrences = 1) intercept {
                actor ! Initialize(testNode)
            }

        }
        it("will log standard error out to warn") {
            val probe = TestProbe()
            val props = ReposeLauncher.props(Seq("bash", "-c", "echo >&2 'standardError'"), warFilePath = fakeWarPath)

            val actor = system.actorOf(props)

            EventFilter.warning("standardError", occurrences = 1) intercept {
                actor ! Initialize(testNode)
            }
        }
        it("terminates the command and closes the streams when dying") {
            val probe = TestProbe()
            //Std out constant noise!

            //Create a temp file
            val f = tempFile("testing", ".txt")

            val fileName = f.getAbsolutePath
            val props = ReposeLauncher.props(Seq("bash", "-c", "while true; do echo 'test' >> " + fileName + "; sleep 0.1; done"), warFilePath = fakeWarPath)

            val actor = system.actorOf(props)

            //Turn it on, it should start doing stuff
            actor ! Initialize(testNode)

            //Give it a bit of time to do a couple things
            Thread.sleep(500)

            //TERMINATE IT
            actor ! PoisonPill //Kill the actor before the process dies

            //Within a second or so, the file should stop receiving "tests"
            def fileLines(): Int = Source.fromFile(f).getLines().count(_ => true)

            var lineCount = 0
            var sames = 0
            val duration = 2.seconds
            val maxTime = System.currentTimeMillis() + duration.toMillis
            while (sames < 4 && maxTime > System.currentTimeMillis()) {
                val newLineCount = fileLines()
                //println(s"old: $lineCount, new: $newLineCount")
                if (newLineCount == lineCount) {
                    //yay -- it was the same
                    sames = sames + 1
                } else {
                    lineCount = newLineCount
                    sames = 0
                    //Boo
                }
                Thread.sleep(200)
            }

            sames should be >= 4
        }

        it("logs an error and terminates if the command executes abnormally") {
            val probe = TestProbe()

            val props = ReposeLauncher.props(Seq("bash", "-c", "exit 1"), warFilePath = fakeWarPath)

            val actor = system.actorOf(props)

            EventFilter.error("Command terminated abnormally. Value: 1", occurrences = 1) intercept {
                actor ! Initialize(testNode)
            }
        }

    }
}
