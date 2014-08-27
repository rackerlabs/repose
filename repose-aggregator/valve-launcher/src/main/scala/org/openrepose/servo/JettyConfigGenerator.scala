package org.openrepose.servo

class JettyConfigGenerator(configRoot: String, node: ReposeNode, keystoreConfig: Option[KeystoreConfig]) {

  val jettyConfig: String = {

    val httpPort = node.httpPort.get

    val xml =
      <Configure id="ValveServer" class="org.eclipse.jetty.server.Server">
        <Call name="addConnector">
          <Arg>
            <New class="org.eclipse.jetty.server.ServerConnector">
              <Arg name="server">
                <Ref refid="ValveServer"/>
              </Arg>
              <Set name="host">{node.host}</Set>
              <Set name="port">{httpPort}</Set>
            </New>
          </Arg>
        </Call>
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
