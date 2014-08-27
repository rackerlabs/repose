package org.openrepose.servo

import org.slf4j.LoggerFactory

class CommandGenerator(baseCommand: Seq[String],
                       configRoot: String,
                       launcherPath: String,
                       warPath: String,
                       keystoreConfig: Option[KeystoreConfig] = None,
                       insecure: Boolean = false) {

  val LOG = LoggerFactory.getLogger(classOf[CommandGenerator])

  def commandLine(node: ReposeNode): Seq[String] = {

    val reposeProps = Seq(
      s"-Drepose-cluster-id=${node.clusterId}",
      s"-Drepose-node-id=${node.nodeId}",
      s"-Dpowerapi-config-directory=$configRoot"
    )

    val systemProps = if(insecure) {
      reposeProps :+ "-Dinsecure=true"
    } else {
      reposeProps
    }


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

    val command = baseCommand ++ systemProps ++ Seq("-jar", launcherPath) ++ jettyParams ++ Seq(warPath)
    LOG.debug("Full generated command: {}", command)
    command
  }
}
