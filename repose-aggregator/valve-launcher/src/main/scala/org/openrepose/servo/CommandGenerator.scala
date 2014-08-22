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
        //TODO: deal with creating the HTTPS config
        //TODO: probably need keystore magic the whole time no matter what...
        ???
      }
      case ReposeNode(_, _, _, Some(httpPort), Some(httpsPort)) => {
        //TODO: deal with creating a config that does both ports
        ???
      }
    }

    Seq("java") ++ systemProps ++ Seq("-jar", launcherPath) ++ jettyParams ++ Seq(warPath)
  }
}
