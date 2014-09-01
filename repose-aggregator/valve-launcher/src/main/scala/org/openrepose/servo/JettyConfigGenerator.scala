package org.openrepose.servo

import scala.util.{Success, Try}
import scala.xml.Node


case class MissingKeystoreException(message:String, cause:Throwable = null) extends Exception(message, cause)

class JettyConfigGenerator(configRoot: String, node: ReposeNode, keystoreConfig: Option[KeystoreConfig]) {

  val jettyConfig: String = {

    val httpConnector = node.httpPort.map { port =>
      <Call name="addConnector">
        <Arg>
          <New class="org.eclipse.jetty.server.ServerConnector">
            <Arg name="server">
              <Ref refid="ValveServer"/>
            </Arg>
            <Set name="host">
              {node.host}
            </Set>
            <Set name="port">
              {port}
            </Set>
          </New>
        </Arg>
      </Call>
    } getOrElse {
      scala.xml.Null
    }

    val httpsConnector = node.httpsPort.map { port =>
      <Call name="addConnector">
        <Arg>
          <New class="org.eclipse.jetty.server.ServerConnector">
            <Arg name="server">
              <Ref refid="ValveServer"/>
            </Arg>
            <Arg name="sslContextFactory">
              <Ref refid="sslContextFactory"/>
            </Arg>
            <Set name="host">{node.host}</Set>
            <Set name="port">{port}</Set>
          </New>
        </Arg>
      </Call>
    } getOrElse {
      scala.xml.Null
    }

    val xml =
      <Configure id="ValveServer" class="org.eclipse.jetty.server.Server">
        {httpConnector}
        {httpsConnector}
      </Configure>

    xml.toString()
  }

  val sslConfig: Option[String] = {

    keystoreConfig.map { config =>
      val configPath = configRoot + "/" + config.filename

      val xml = <Configure id="sslContextFactory" class="org.eclipse.jetty.util.ssl.SslContextFactory">
        <Set name="KeyStorePath">
          {configPath}
        </Set>
        <Set name="KeyStorePassword">
          {config.keystorePassword}
        </Set>
        <Set name="KeyManagerPassword">
          {config.keyPassword}
        </Set>
        <!-- Our config doesn't set any trust store or anything else -->
        <!-- our config doesn't exclude any ciphers -->
      </Configure>

      Some(xml.toString())
    } getOrElse {
      None
    }
  }

}
