package org.openrepose.servo

import java.io.File
import java.nio.file.attribute.{PosixFilePermissions, PosixFilePermission}
import java.nio.file.{StandardOpenOption, OpenOption, Files}

import org.slf4j.LoggerFactory

class CommandGenerator(baseCommand: Seq[String],
                       configRoot: String,
                       launcherPath: String,
                       warPath: String,
                       keystoreConfig: Option[KeystoreConfig] = None,
                       insecure: Boolean = false) {

  val LOG = LoggerFactory.getLogger(classOf[CommandGenerator])

  def commandLine(node: ReposeNode): Seq[String] = {

    val jettyConfig = new JettyConfigGenerator(configRoot, node, keystoreConfig)

    val reposeProps = Seq(
      s"-Drepose-cluster-id=${node.clusterId}",
      s"-Drepose-node-id=${node.nodeId}",
      s"-Dpowerapi-config-directory=$configRoot"
    )

    val systemProps = if (insecure) {
      reposeProps :+ "-Dinsecure=true"
    } else {
      reposeProps
    }

    //Need to generate a few --config /config/root/jetty.xmls
    val configFileName = s"${node.clusterId}_${node.nodeId}_jetty.xml"
    val sslConfigName = s"${node.clusterId}_${node.nodeId}_jetty_ssl.xml"

    val config = new File(configRoot, configFileName)
    val configPermissions = PosixFilePermissions.fromString("rw-rw----")

    /**
     * Use the Files stuff to make sure that we set the permissions on the jetty config files properly
     * This will handle it if the file exists, or if we're creating new ones, without doing unnecessary deletes
     * @param f
     */
    def prepConfigFile(f:File):Unit = {
      if(!Files.exists(f.toPath)) {
        Files.createFile(f.toPath, PosixFilePermissions.asFileAttribute(configPermissions))
      } else {
        Files.setPosixFilePermissions(f.toPath, configPermissions)
      }
    }

    prepConfigFile(config)
    Files.write(config.toPath, jettyConfig.jettyConfig.getBytes, StandardOpenOption.CREATE)
    val jettyConfigParams = Seq("--config", config.getAbsolutePath)

    val jettySslParams = jettyConfig.sslConfig match {
      case Some(sslConfig) =>
        val sslConfigFile = new File(configRoot, sslConfigName)
        prepConfigFile(sslConfigFile)
        Files.write(sslConfigFile.toPath, sslConfig.getBytes, StandardOpenOption.CREATE)
        Seq("--config", sslConfigFile.getAbsolutePath)
      case None =>
        Seq()
    }

    val command = baseCommand ++ systemProps ++ Seq("-jar", launcherPath) ++ jettySslParams ++ jettyConfigParams ++ Seq(warPath)
    LOG.debug("Full generated command: {}", command mkString " ")
    command
  }
}
