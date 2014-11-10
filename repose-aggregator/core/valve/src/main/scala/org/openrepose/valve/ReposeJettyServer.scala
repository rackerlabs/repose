package org.openrepose.valve

import java.io.File
import java.util
import javax.servlet.DispatcherType

import com.typesafe.config.ConfigFactory
import org.eclipse.jetty.server.{ServerConnector, Connector, Server}
import org.eclipse.jetty.servlet.{FilterHolder, ServletContextHandler}
import org.eclipse.jetty.util.ssl.SslContextFactory
import org.openrepose.core.container.config.SslConfiguration
import org.openrepose.core.spring.{ReposeSpringProperties, CoreSpringProvider}
import org.openrepose.powerfilter.EmptyServlet
import org.springframework.core.env.MapPropertySource
import org.springframework.web.context.ContextLoaderListener
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext
import org.springframework.web.filter.DelegatingFilterProxy

case class ServerInitializationException(message: String, cause: Throwable = null) extends Exception(message, cause)

/**
 * Each jetty server starts here. These are the unique things that identify a jetty.
 * A single jetty can listen on both an HTTP port and an HTTPS port. In theory, a single jetty could listen on many
 * ports, and just have many connectors. A clusterID and nodeID is all that is needed to figure out what jetty it is.
 * It will fail to build if there's no SSL configuration
 * @param configRoot
 * @param clusterId
 * @param nodeId
 * @param httpPort
 * @param httpsPort
 * @param sslConfig
 * @param insecure
 */
class ReposeJettyServer(configRoot: String,
                        clusterId: String,
                        nodeId: String,
                        httpPort: Option[Integer],
                        httpsPort: Option[Integer],
                        sslConfig: Option[SslConfiguration],
                        insecure: Boolean) {


  /**
   * Create the jetty server for this guy
   */
  val server: Server = {
    val s = new Server()

    val config = ConfigFactory.load("springConfiguration.conf")

    //Set up connectors
    val httpConnector: Option[Connector] = httpPort.map { port =>
      val conn = new ServerConnector(s)
      conn.setPort(port)
      conn
    }

    val httpsConnector: Option[Connector] = httpsPort.map { port =>
      val cf = new SslContextFactory()

      //TODO: do we make this a URL for realsies?
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

    val connectors = List(httpConnector, httpsConnector).filter(_.isDefined).map(_.get).toArray

    if(connectors.isEmpty) {
      throw new ServerInitializationException("At least one HTTP or HTTPS port must be specified")
    }

    //Hook up the port connectors!
    s.setConnectors(connectors)

    //Build us some springs
    val serverSpringContext = new AnnotationConfigWebApplicationContext()
    //TODO: might have to pass in the core context?
    serverSpringContext.setParent(CoreSpringProvider.getInstance().getCoreContext)

    //TODO: need to know what we need to be able to fire up stuff, might only be the powerfilter stuff. I think
    serverSpringContext.scan(config.getString("powerFilterSpringContextPath"))

    //create properties based on what we know
    val props: Map[String, AnyRef] = Map(
      ReposeSpringProperties.NODE_ID -> nodeId,
      ReposeSpringProperties.CLUSTER_ID -> clusterId,
      ReposeSpringProperties.CONFIG_ROOT -> configRoot,
      ReposeSpringProperties.INSECURE -> new java.lang.Boolean(insecure.booleanValue)
    )

    import scala.collection.JavaConversions._
    val myProps = new MapPropertySource(s"node-$nodeId-props", props)
    serverSpringContext.getEnvironment.getPropertySources.addFirst(myProps)

    val contextHandler = new ServletContextHandler()
    contextHandler.setContextPath("/")
    contextHandler.addServlet(classOf[EmptyServlet], "/*")

    val cll = new ContextLoaderListener(serverSpringContext)
    contextHandler.addEventListener(cll)

    val filterHolder = new FilterHolder()

    //Don't use our bean directly, but use the Delegating Filter Bean from spring, so that sanity happens
    // See: http://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/web/filter/DelegatingFilterProxy.html
    //Create a spring delegating proxy for a repose filter bean
    // THe name must match the Named bean name (powerFilter)
    val delegatingProxy = new DelegatingFilterProxy("powerFilter")
    filterHolder.setFilter(delegatingProxy)
    filterHolder.setDisplayName("SpringDelegatingFilter")

    contextHandler.addFilter(filterHolder, "/*", util.EnumSet.of(DispatcherType.REQUEST))

    s.setHandler(contextHandler)

    s
  }

}
