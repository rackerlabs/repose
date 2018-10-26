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

import java.io.Writer
import java.util
import javax.servlet.DispatcherType
import javax.servlet.http.{HttpServletRequest, HttpServletResponse}

import com.typesafe.config.ConfigFactory
import org.eclipse.jetty.http.HttpHeader
import org.eclipse.jetty.server.handler.ErrorHandler
import org.eclipse.jetty.server._
import org.eclipse.jetty.servlet.{FilterHolder, ServletContextHandler}
import org.eclipse.jetty.util.ssl.SslContextFactory
import org.openrepose.commons.utils.io.FileUtilities
import org.openrepose.core.container.config.SslConfiguration
import org.openrepose.core.spring.{CoreSpringProvider, ReposeSpringProperties}
import org.openrepose.powerfilter.EmptyServlet
import org.springframework.context.support.{AbstractApplicationContext, PropertySourcesPlaceholderConfigurer}
import org.springframework.web.context.ContextLoaderListener
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext
import org.springframework.web.filter.DelegatingFilterProxy

/**
  * Each jetty server starts here. These are the unique things that identify a jetty.
  * A single jetty can listen on both an HTTP port and an HTTPS port. In theory, a single jetty could listen on many
  * ports, and just have many connectors. A clusterID and nodeID is all that is needed to figure out what jetty it is.
  * It will fail to build if there's no SSL configuration
  *
  * @param clusterId    Repose Cluster ID
  * @param nodeId       Repose Node ID
  * @param httpPort     The port to listen on for HTTP traffic
  * @param httpsPort    The port to listen on for HTTPS traffic
  * @param idleTimeout  The time in milliseconds that the connection can be idle before it is closed.
  * @param soLingerTime A value >=0 sets the socket SO_LINGER value in milliseconds.
  * @param sslConfig    The SSL Configuration to use
  */
class ReposeJettyServer(val nodeContext: AbstractApplicationContext,
                        val clusterId: String,
                        val nodeId: String,
                        val httpPort: Option[Int],
                        val httpsPort: Option[Int],
                        sslConfig: Option[SslConfiguration],
                        idleTimeout: Option[Long],
                        soLingerTime: Option[Int],
                        testMode: Boolean = false) {

  import ReposeJettyServer._

  val config = ConfigFactory.load("springConfiguration.conf")

  val appContext = new AnnotationConfigWebApplicationContext()

  val coreSpringProvider = CoreSpringProvider.getInstance()

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
      conn.getConnectionFactory(classOf[HttpConnectionFactory])
        .getHttpConfiguration
        .setSendServerVersion(false)

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
      // See: http://www.eclipse.org/jetty/documentation/current/configuring-ssl.html
      val configRoot = coreSpringProvider.getCoreContext.getEnvironment.getProperty(ReposeSpringProperties.stripSpringValueStupidity(ReposeSpringProperties.CORE.CONFIG_ROOT))
      sslConfig.map { ssl =>
        cf.setKeyStorePath(FileUtilities.guardedAbsoluteFile(configRoot, ssl.getKeystoreFilename).getAbsolutePath)
        cf.setKeyStorePassword(ssl.getKeystorePassword)
        cf.setKeyManagerPassword(ssl.getKeyPassword)
        if (ssl.isNeedClientAuth) {
          cf.setNeedClientAuth(true)
          Option(ssl.getTruststoreFilename).foreach { truststoreFilename =>
            cf.setTrustStorePath(FileUtilities.guardedAbsoluteFile(configRoot, truststoreFilename).getAbsolutePath)
            cf.setTrustStorePassword(ssl.getTruststorePassword)
          }
        }

        val sslConnector = new ServerConnector(s, cf)
        if (testMode) {
          sslConnector.setPort(0)
        } else {
          sslConnector.setPort(port)
        }

        //Handle the Protocols and Ciphers
        //varargs are annoying, so lets deal with this using the scala methods
        // From https://www.eclipse.org/jetty/documentation/9.4.x/configuring-ssl.html
        // "When working with Includes / Excludes, it is important to know that Excludes will always win."
        import scala.collection.JavaConversions._
        // Clear Jetty defaults that were added in later v9.2.x releases.
        cf.setIncludeProtocols()
        cf.setExcludeProtocols()
        cf.setIncludeCipherSuites()
        cf.setExcludeCipherSuites()
        Option(ssl.getIncludedProtocols) foreach { ip => cf.setIncludeProtocols(ip.getProtocol.toList: _*) }
        Option(ssl.getExcludedProtocols) foreach { ep => cf.setExcludeProtocols(ep.getProtocol.toList: _*) }
        Option(ssl.getIncludedCiphers) foreach { ic => cf.setIncludeCipherSuites(ic.getCipher.toList: _*) }
        Option(ssl.getExcludedCiphers) foreach { ec => cf.setExcludeCipherSuites(ec.getCipher.toList: _*) }

        cf.setRenegotiationAllowed(Option(ssl.isTlsRenegotiationAllowed).getOrElse(false).asInstanceOf[Boolean])

        sslConnector
      } getOrElse {
        //If we didn't have an SSL config, BOMBZ0R
        throw ServerInitializationException("HTTPS port was specified, but no SSL config provided")
      }
    }

    val connectors = List(httpConnector, httpsConnector).collect { case Some(x) => x }.toArray

    if (connectors.isEmpty) {
      throw ServerInitializationException("At least one HTTP or HTTPS port must be specified")
    } else {
      connectors foreach { connector =>
        idleTimeout foreach connector.setIdleTimeout
        soLingerTime foreach connector.setSoLingerTime
      }
    }

    //Hook up the port connectors!
    //Have to coerce the stuff here, because it makes it happier
    s.setConnectors(connectors.asInstanceOf[Array[Connector]])

    val contextHandler = new ServletContextHandler()
    contextHandler.setContextPath("/")
    contextHandler.addServlet(classOf[EmptyServlet], "/*")

    // todo: when switching to the new ReposeFilterChain, replace the previous line with the following line
    // todo: to wire in the new routing servlet
    // contextHandler.addServlet(new ServletHolder(appContext.getBean(classOf[ReposeRoutingServlet])), "/*")

    val cll = new ContextLoaderListener(appContext)
    contextHandler.addEventListener(cll)

    val filterHolder = new FilterHolder()

    //Don't use our bean directly, but use the Delegating Filter Bean from spring, so that sanity happens
    // See: http://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/web/filter/DelegatingFilterProxy.html
    //Create a spring delegating proxy for a repose filter bean
    // The name must match the Named bean name (powerFilter)
    val delegatingProxy = new DelegatingFilterProxy("powerFilter")
    filterHolder.setFilter(delegatingProxy)
    filterHolder.setDisplayName("SpringDelegatingFilter")

    // todo: when switching to the new ReposeFilterChain, replace the previous line with the following line
    // todo: to wire in the new ReposeFilter.
    //filterHolder.setFilter(appContext.getBean("reposeFilter")
    //filterHolder.setDisplayName("ReposeFilter")

    //All the dispatch types...
    val dispatchTypes = util.EnumSet.allOf(classOf[DispatcherType]) //Using what was in the old repose
    contextHandler.addFilter(filterHolder, "/*", dispatchTypes)

    s.setHandler(contextHandler)

    // Prevents Jetty from writing HTML error pages when sendError is called on the response.
    // This is for backwards compatibility, but should eventually be removed.
    s.setErrorHandler(new ErrorHandler() {
      override def doError(target: String, baseRequest: Request, request: HttpServletRequest, response: HttpServletResponse): Unit = {
        if (getCacheControl != null) response.setHeader(HttpHeader.CACHE_CONTROL.asString, getCacheControl)
        baseRequest.setHandled(true)
      }
    })

    (httpConnector, httpsConnector, s)
  }
  private var isShutdown = false

  def runningHttpPort: Int = {
    httpConnector.map(_.getLocalPort).getOrElse(0)
  }

  def runningHttpsPort: Int = {
    httpsConnector.map(_.getLocalPort).getOrElse(0)
  }

  def start(): Unit = {
    if (isShutdown) {
      throw new Exception("Cannot start again a shutdown ReposeJettyServer")
    }
    server.start()
  }

  /**
    * Shuts this one down and returns a new one
    *
    * @return
    */
  def restart(): ReposeJettyServer = {
    shutdown()
    new ReposeJettyServer(nodeContext, clusterId, nodeId, httpPort, httpsPort, sslConfig, idleTimeout, soLingerTime, testMode)
  }

  /**
    * destroys everything, this class won't be usable again
    */
  def shutdown(): Unit = {
    isShutdown = true
    nodeContext.close()
    stop()
  }

  def stop(): Unit = {
    server.stop()
  }
}

object ReposeJettyServer {
  case class ServerInitializationException(message: String, cause: Throwable = null) extends Exception(message, cause)
}
