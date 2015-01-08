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
                        sslConfig: Option[SslConfiguration]) {

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

  private var isShutdown = false


  /**
   * Create the jetty server for this guy
   */
  val server: Server = {
    val s = new Server()

    //Set up connectors
    val httpConnector: Option[Connector] = httpPort.map { port =>
      val conn = new ServerConnector(s)
      conn.setPort(port)
      conn
    }

    val httpsConnector: Option[Connector] = httpsPort.map { port =>
      val cf = new SslContextFactory()

      //TODO: do we make this a URL for realsies?
      //Get the configuration root from the core spring context, because we haven't fired up the app Context yet.
      val configRoot = coreSpringProvider.getCoreContext.getEnvironment.getProperty(ReposeSpringProperties.CORE.CONFIG_ROOT)
      sslConfig.map { ssl =>
        cf.setKeyStorePath(configRoot + File.separator + ssl.getKeystoreFilename)
        cf.setKeyStorePassword(ssl.getKeystorePassword)
        cf.setKeyManagerPassword(ssl.getKeyPassword)

        val sslConnector = new ServerConnector(s, cf)
        sslConnector.setPort(port)
        sslConnector
      } getOrElse {
        //If we didn't have an SSL config, BOMBZ0R
        throw new ServerInitializationException("HTTPS port was specified, but no SSL config provided")
      }
    }

    val connectors = List(httpConnector, httpsConnector).collect{ case Some(x) => x }.toArray

    if (connectors.isEmpty) {
      throw new ServerInitializationException("At least one HTTP or HTTPS port must be specified")
    }

    //Hook up the port connectors!
    s.setConnectors(connectors)

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

    s
  }

  def start() = {
    if (isShutdown) {
      throw new Exception("Cannot start again a shutdown ReposeJettyServer")
    }
    server.start()
  }

  def stop() = {
    server.stop()
  }

  /**
   * destroys everything, this class won't be usable again
   */
  def shutdown() = {
    isShutdown = true
    nodeContext.close()
    stop()
  }

  /**
   * Shuts this one down and returns a new one
   * @return
   */
  def restart(): ReposeJettyServer = {
    shutdown()
    new ReposeJettyServer(clusterId, nodeId, httpPort, httpsPort, sslConfig)
  }
}
