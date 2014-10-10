package com.rackspace.papi.components.rackspace.identity.basicauth

import java.net.URL
import javax.servlet._

import org.openrepose.commons.config.manager.UpdateListener
import com.rackspace.papi.components.rackspace.identity.basicauth.config.RackspaceIdentityBasicAuthConfig
import org.openrepose.core.filter.FilterConfigHelper
import org.openrepose.core.filter.logic.impl.FilterLogicHandlerDelegate
import org.openrepose.core.services.config.ConfigurationService
import org.openrepose.core.services.context.ServletContextHelper
import org.slf4j.LoggerFactory

class RackspaceIdentityBasicAuthFilter extends Filter {
  private final val LOG = LoggerFactory.getLogger(classOf[RackspaceIdentityBasicAuthFilter])
  private final val DEFAULT_CONFIG = "rackspace-identity-basic-auth.cfg.xml"

  private var config: String = _
  private var handlerFactory: RackspaceIdentityBasicAuthHandlerFactory = _
  private var configurationService: ConfigurationService = _

  override def init(filterConfig: FilterConfig) {
    config = new FilterConfigHelper(filterConfig).getFilterConfig(DEFAULT_CONFIG)
    LOG.info("Initializing filter using config " + config)
    val powerApiContext = ServletContextHelper.getInstance(filterConfig.getServletContext).getPowerApiContext
    configurationService = powerApiContext.configurationService
    handlerFactory = new RackspaceIdentityBasicAuthHandlerFactory(powerApiContext.akkaServiceClientService, powerApiContext.datastoreService)
    val xsdURL: URL = getClass.getResource("/META-INF/config/schema/rackspace-identity-basic-auth.xsd")
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
