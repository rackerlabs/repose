package org.openrepose.filters.rackspaceidentitybasicauth

import java.net.URL
import javax.inject.{Inject, Named}
import javax.servlet._

import org.openrepose.commons.config.manager.UpdateListener
import org.openrepose.core.filter.FilterConfigHelper
import org.openrepose.core.filter.logic.impl.FilterLogicHandlerDelegate
import org.openrepose.core.services.config.ConfigurationService
import org.openrepose.filters.rackspaceidentitybasicauth.config.RackspaceIdentityBasicAuthConfig
import org.openrepose.services.datastore.DatastoreService
import org.openrepose.services.serviceclient.akka.AkkaServiceClient
import org.slf4j.LoggerFactory

@Named
class RackspaceIdentityBasicAuthFilter @Inject() (configurationService: ConfigurationService,
                                                  akkaServiceClient : AkkaServiceClient,
                                                  datastoreService : DatastoreService) extends Filter {
  private final val LOG = LoggerFactory.getLogger(classOf[RackspaceIdentityBasicAuthFilter])
  private final val DEFAULT_CONFIG = "rackspace-identity-basic-auth.cfg.xml"

  private var config: String = _
  private var handlerFactory: RackspaceIdentityBasicAuthHandlerFactory = _

  override def init(filterConfig: FilterConfig) {
    config = new FilterConfigHelper(filterConfig).getFilterConfig(DEFAULT_CONFIG)
    LOG.info("Initializing filter using config " + config)
    handlerFactory = new RackspaceIdentityBasicAuthHandlerFactory(akkaServiceClient, datastoreService)
    val xsdURL: URL = getClass.getResource("/META-INF/config/schema/rackspace-identity-basic-auth.xsd")
    // TODO: Clean up the asInstanceOf below, if possible?
    configurationService.subscribeTo(
      filterConfig.getFilterName,
      config,
      xsdURL,
      handlerFactory.asInstanceOf[UpdateListener[RackspaceIdentityBasicAuthConfig]],
      classOf[RackspaceIdentityBasicAuthConfig]
    )
    LOG.warn("WARNING: This filter cannot be used alone, it requires an AuthFilter after it.")
  }

  override def doFilter(servletRequest: ServletRequest, servletResponse: ServletResponse, filterChain: FilterChain) {
    new FilterLogicHandlerDelegate(servletRequest, servletResponse, filterChain).doFilter(handlerFactory.newHandler)
  }

  override def destroy() {
    configurationService.unsubscribeFrom(config, handlerFactory)
  }
}
