package org.openrepose.servo

import org.junit.runner.RunWith
import org.scalatest.{Matchers, FunSpec}
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class CommandGeneratorTest extends FunSpec with Matchers with TestUtils {

  describe("Command line generator") {
    it("generates a proper command line from a node, a configurationRoot, a war path, and a launcher path") {
      val configurationRoot = "/config/root"
      val warPath = "/path/to/war"
      val launcherPath = "/path/to/launcher"
      val cg = new CommandGenerator(configurationRoot, launcherPath, warPath)

      val node = ReposeNode("clusterId", "nodeId", "hostname", Some(8080), None)

      cg.commandLine(node) shouldBe Seq("java", "-Drepose-cluster-id=clusterId", "-Drepose-node-id=nodeId", "-Dpowerapi-config-directory=/config/root",
        "-jar", "/path/to/launcher", "--port", "8080", "/path/to/war")
    }
    it("generates a proper command line for a node with an HTTPS port") {
      pending
    }
    it("generates a proper command line for a node with both an http port and an https port") {
      pending
    }
  }
}
