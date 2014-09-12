package org.openrepose.servo

import org.junit.runner.RunWith
import org.scalatest.{BeforeAndAfterAll, Matchers, FunSpec}
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class CommandGeneratorTest extends FunSpec with Matchers with TestUtils with BeforeAndAfterAll {

  val tempDirFile = tempDir("commandGenerator").toFile
  val tempdir = tempDirFile.getAbsolutePath

  override def afterAll() = {
    deleteRecursive(tempDirFile.toPath)
  }

  val configurationRoot = tempdir
  val warPath = "/path/to/war"
  val launcherPath = "/path/to/launcher"
  val baseCommand = Seq("java")
  val node = ReposeNode("clusterId", "nodeId", "hostname", Some(8080), None)
  val httpsNode = ReposeNode("clusterId", "nodeId", "hostname", None, Some(8081))
  val bothNode = ReposeNode("clusterId", "nodeId", "hostname", Some(8080), Some(8081))
  val keystoreConfig = KeystoreConfig("keystoreFile", "keystorePass", "keyPass")


  //TODO: this will probably not put the port in the thing any longer at all, and use config files the whole time
  describe("Command line generator") {
    it("generates a proper command line from a base command, a node, a configurationRoot, a war path, and a launcher path") {
      val cg = new CommandGenerator(baseCommand, configurationRoot, launcherPath, warPath)

      cg.commandLine(node) shouldBe Seq("java", "-Drepose-cluster-id=clusterId", "-Drepose-node-id=nodeId", s"-Dpowerapi-config-directory=$tempdir",
        "-jar", "/path/to/launcher", "--config", s"$tempdir/clusterId_nodeId_jetty.xml", "/path/to/war")
    }
    it("generates a proper command line when given a container config with a keystore") {
      //This would include the configuration file generated.
      val cg = new CommandGenerator(baseCommand, configurationRoot, launcherPath, warPath, Some(keystoreConfig))

      cg.commandLine(node) shouldBe Seq("java", "-Drepose-cluster-id=clusterId", "-Drepose-node-id=nodeId", s"-Dpowerapi-config-directory=$tempdir",
        "-jar", "/path/to/launcher","--config", s"$tempdir/clusterId_nodeId_jetty_ssl.xml", "--config", s"$tempdir/clusterId_nodeId_jetty.xml", "/path/to/war")
    }

    it("generates a proper command line when told to operate insecurely") {
      val cg = new CommandGenerator(baseCommand, configurationRoot, launcherPath, warPath, insecure = true)

      cg.commandLine(node) shouldBe Seq("java",
        "-Drepose-cluster-id=clusterId", "-Drepose-node-id=nodeId", s"-Dpowerapi-config-directory=$tempdir","-Dinsecure=true",
        "-jar", "/path/to/launcher", "--config", s"$tempdir/clusterId_nodeId_jetty.xml", "/path/to/war")

    }
    it("generates a proper command line for a node with an HTTPS port") {
      val cg = new CommandGenerator(baseCommand, configurationRoot, launcherPath, warPath, Some(keystoreConfig))

      cg.commandLine(httpsNode) shouldBe Seq("java",
        "-Drepose-cluster-id=clusterId", "-Drepose-node-id=nodeId", s"-Dpowerapi-config-directory=$tempdir",
        "-jar", "/path/to/launcher", "--config", s"$tempdir/clusterId_nodeId_jetty_ssl.xml" , "--config", s"$tempdir/clusterId_nodeId_jetty.xml", "/path/to/war")

    }
    it("generates a proper command line for a node with both an http port and an https port") {
      val cg = new CommandGenerator(baseCommand, configurationRoot, launcherPath, warPath, Some(keystoreConfig))

      cg.commandLine(bothNode) shouldBe Seq("java",
        "-Drepose-cluster-id=clusterId", "-Drepose-node-id=nodeId", s"-Dpowerapi-config-directory=$tempdir",
        "-jar", "/path/to/launcher", "--config", s"$tempdir/clusterId_nodeId_jetty_ssl.xml" , "--config", s"$tempdir/clusterId_nodeId_jetty.xml", "/path/to/war")


    }
  }
}
