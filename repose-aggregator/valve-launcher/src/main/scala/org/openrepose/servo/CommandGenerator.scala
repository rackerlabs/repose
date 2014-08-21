package org.openrepose.servo

class CommandGenerator(configRoot: String, launcherPath: String, warPath: String) {

  def commandLine(node: ReposeNode): Seq[String] = {
    val systemProps = Seq(
      s"-Drepose-cluster-id=${node.clusterId}",
      s"-Drepose-node-id=${node.nodeId}",
      s"-Dpowerapi-config-directory=$configRoot"
    )

    val jettyParams: Seq[String] = node match {
      case ReposeNode(_, _, _, Some(port), None) => {
        Seq("--port", port.toString)
      }
      case ReposeNode(_, _, _, None, Some(httpsPort)) => {
        ???
      }
      case ReposeNode(_, _, _, Some(httpPort), Some(httpsPort)) => {
        ???
      }
    }

    Seq("java") ++ systemProps ++ Seq("-jar", launcherPath) ++ jettyParams ++ Seq(warPath)
  }
}
