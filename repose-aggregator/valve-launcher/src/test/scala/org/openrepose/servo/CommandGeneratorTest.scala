package org.openrepose.servo

import org.junit.runner.RunWith
import org.scalatest.{Matchers, FunSpec}
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class CommandGeneratorTest extends FunSpec with Matchers with TestUtils {

  val tempdir = tempDir("commandGenerator").toFile.getAbsolutePath

  val configurationRoot = tempdir
  val warPath = "/path/to/war"
  val launcherPath = "/path/to/launcher"
  val baseCommand = Seq("java")
  val node = ReposeNode("clusterId", "nodeId", "hostname", Some(8080), None)
  val keystoreConfig = KeystoreConfig("keystoreFile", "keystorePass", "keyPass")


  describe("Command line generator") {
    it("generates a proper command line from a base command, a node, a configurationRoot, a war path, and a launcher path") {
      val cg = new CommandGenerator(baseCommand, configurationRoot, launcherPath, warPath)


      cg.commandLine(node) shouldBe Seq("java", "-Drepose-cluster-id=clusterId", "-Drepose-node-id=nodeId", s"-Dpowerapi-config-directory=$tempdir",
        "-jar", "/path/to/launcher", "--port", "8080", "/path/to/war")
    }
    it("generates a proper command line when given a container config with a keystore") {
      //This would include the configuration file generated.
      val cg = new CommandGenerator(baseCommand, configurationRoot, launcherPath, warPath, Some(keystoreConfig))

      //TODO: also need to verify the file that's written? This means that /config/root has to be real
      //This has information on configuring the xmls for SSL and for a keystore
      //http://www.eclipse.org/jetty/documentation/current/configuring-ssl.html

      cg.commandLine(node) shouldBe Seq("java", "-Drepose-cluster-id=clusterId", "-Drepose-node-id=nodeId", s"-Dpowerapi-config-directory=$tempdir",
        "-jar", "/path/to/launcher", "--config", s"$tempdir/clusterId_nodeId_jetty.xml", "--port", "8080", "/path/to/war")
    }

    it("generates a proper command line when told to operate insecurely") {
      pending
    }
    it("generates a proper command line for a node with an HTTPS port") {
      pending
    }
    it("generates a proper command line for a node with both an http port and an https port") {
      pending
    }
  }
}
