/*
 * _=_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_=
 * Repose
 * _-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-
 * Copyright (C) 2010 - 2015 Rackspace US, Inc.
 * _-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * =_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_=_
 */
package org.openrepose.valve

import java.io.File
import java.util
import javax.servlet.DispatcherType

import com.typesafe.config.ConfigFactory
import org.eclipse.jetty.server.{Connector, Server, ServerConnector}
import org.eclipse.jetty.servlet.{FilterHolder, ServletContextHandler}
import org.eclipse.jetty.util.ssl.SslContextFactory
import org.openrepose.core.container.config.SslConfiguration
import org.openrepose.core.spring.{CoreSpringProvider, ReposeSpringProperties}
import org.openrepose.powerfilter.EmptyServlet
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer
import org.springframework.web.context.ContextLoaderListener
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext
import org.springframework.web.filter.DelegatingFilterProxy

case class ServerInitializationException(message: String, cause: Throwable = null) extends Exception(message, cause)

/**
 * Each jetty server starts here. These are the unique things that identify a jetty.
 * A single jetty can listen on both an HTTP port and an HTTPS port. In theory, a single jetty could listen on many
 * ports, and just have many connectors. A clusterID and nodeID is all that is needed to figure out what jetty it is.
 * It will fail to build if there's no SSL configuration
 * @param clusterId
 * @param nodeId
 * @param httpPort
 * @param httpsPort
 * @param sslConfig
 */
class ReposeJettyServer(val clusterId: String,
                        val nodeId: String,
                        val httpPort: Option[Int],
                        val httpsPort: Option[Int],
                        sslConfig: Option[SslConfiguration],
                        testMode: Boolean = false) {

  val config = ConfigFactory.load("springConfiguration.conf")

  val appContext = new AnnotationConfigWebApplicationContext()

  val coreSpringProvider = CoreSpringProvider.getInstance()
  //Safe to use here, it's been initialized earlier
  val nodeContext = coreSpringProvider.getNodeContext(clusterId, nodeId)

  //NOTE: have to add this manually each time we fire up a spring context so that we can ensure that @Value works
  val propConfig = new PropertySourcesPlaceholderConfigurer()
  propConfig.setEnvironment(appContext.getEnvironment)
  appContext.addBeanFactoryPostProcessor(propConfig)

  appContext.setParent(nodeContext) //Use the local node context, not the core context
  appContext.scan(config.getString("powerFilterSpringContextPath"))
  /**
   * Create the jetty server for this guy,
   * and get back the connectors so I can ask for ports for them :D
   */
  val (httpConnector: Option[ServerConnector], httpsConnector: Option[ServerConnector], server: Server) = {
    val s = new Server()

    //If I'm in test mode, then randomly select ports and register that port somehow with JMX...

    //Set up connectors
    val httpConnector: Option[ServerConnector] = httpPort.map { port =>
      val conn = new ServerConnector(s)
      if (testMode) {
        conn.setPort(0)
      } else {
        conn.setPort(port)
      }
      conn
    }

    val httpsConnector: Option[ServerConnector] = httpsPort.map { port =>
      val cf = new SslContextFactory()

      //TODO: do we make this a URL for realsies?
      //Get the configuration root from the core spring context, because we haven't fired up the app Context yet.
      val configRoot = coreSpringProvider.getCoreContext.getEnvironment.getProperty(ReposeSpringProperties.stripSpringValueStupidity(ReposeSpringProperties.CORE.CONFIG_ROOT))
      sslConfig.map { ssl =>
        cf.setKeyStorePath(configRoot + File.separator + ssl.getKeystoreFilename)
        cf.setKeyStorePassword(ssl.getKeystorePassword)
        cf.setKeyManagerPassword(ssl.getKeyPassword)

        val sslConnector = new ServerConnector(s, cf)
        if (testMode) {
          sslConnector.setPort(0)
        } else {
          sslConnector.setPort(port)
        }

        //Handle the Protocols and Ciphers
        //varargs are annoying, so lets deal with this using the scala methods
        import scala.collection.JavaConversions._
        Option(ssl.getExcludedProtocols).foreach(xp => cf.addExcludeProtocols(xp.getProtocol.toList: _*))
        Option(ssl.getIncludedProtocols).foreach { ip =>
          if (ip.getProtocol.nonEmpty) {
            cf.setIncludeProtocols(ip.getProtocol.toList: _*)
          }

        }
        Option(ssl.getExcludedCiphers).foreach { xc =>
          cf.addExcludeCipherSuites(xc.getCipher.toList: _*)
        }

        Option(ssl.getIncludedCiphers).foreach { ic =>
          if (ic.getCipher.nonEmpty) {
            cf.setIncludeCipherSuites(ic.getCipher.toList: _*)
          }
        }

        Option(ssl.isTlsRenegotiationAllowed).foreach { tls =>
          cf.setRenegotiationAllowed(tls)
        }

        sslConnector
      } getOrElse {
        //If we didn't have an SSL config, BOMBZ0R
        throw new ServerInitializationException("HTTPS port was specified, but no SSL config provided")
      }
    }

    val connectors = List(httpConnector, httpsConnector).collect { case Some(x) => x }.toArray

    if (connectors.isEmpty) {
      throw new ServerInitializationException("At least one HTTP or HTTPS port must be specified")
    }

    //Hook up the port connectors!
    //Have to coerce the stuff here, because it makes it happier
    s.setConnectors(connectors.asInstanceOf[Array[Connector]])

    val contextHandler = new ServletContextHandler()
    contextHandler.setContextPath("/")
    contextHandler.addServlet(classOf[EmptyServlet], "/*")

    val cll = new ContextLoaderListener(appContext)
    contextHandler.addEventListener(cll)

    val filterHolder = new FilterHolder()

    //Don't use our bean directly, but use the Delegating Filter Bean from spring, so that sanity happens
    // See: http://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/web/filter/DelegatingFilterProxy.html
    //Create a spring delegating proxy for a repose filter bean
    // THe name must match the Named bean name (powerFilter)
    val delegatingProxy = new DelegatingFilterProxy("powerFilter")
    filterHolder.setFilter(delegatingProxy)
    filterHolder.setDisplayName("SpringDelegatingFilter")


    //All the dispatch types...
    val dispatchTypes = util.EnumSet.allOf(classOf[DispatcherType]) //Using what was in the old repose
    contextHandler.addFilter(filterHolder, "/*", dispatchTypes)

    s.setHandler(contextHandler)

    (httpConnector, httpsConnector, s)
  }
  private var isShutdown = false

  def runningHttpPort: Int = {
    httpConnector.map(_.getLocalPort).getOrElse(0)
  }

  def runningHttpsPort: Int = {
    httpsConnector.map(_.getLocalPort).getOrElse(0)
  }

  def start() = {
    if (isShutdown) {
      throw new Exception("Cannot start again a shutdown ReposeJettyServer")
    }
    server.start()
  }

  /**
   * Shuts this one down and returns a new one
   * @return
   */
  def restart(): ReposeJettyServer = {
    shutdown()
    new ReposeJettyServer(clusterId, nodeId, httpPort, httpsPort, sslConfig, testMode)
  }

  /**
   * destroys everything, this class won't be usable again
   */
  def shutdown() = {
    isShutdown = true
    nodeContext.close()
    stop()
  }

  def stop() = {
    server.stop()
  }
}
