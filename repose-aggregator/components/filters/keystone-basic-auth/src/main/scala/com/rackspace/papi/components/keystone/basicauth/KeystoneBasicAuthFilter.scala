package com.rackspace.papi.components.keystone.basicauth

import java.net.URL
import javax.servlet._

import com.rackspace.papi.commons.config.manager.UpdateListener
import com.rackspace.papi.components.keystone.basicauth.config.KeystoneBasicAuthConfig
import com.rackspace.papi.filter.FilterConfigHelper
import com.rackspace.papi.filter.logic.impl.FilterLogicHandlerDelegate
import com.rackspace.papi.service.config.ConfigurationService
import com.rackspace.papi.service.context.ServletContextHelper
import org.slf4j.LoggerFactory

class KeystoneBasicAuthFilter extends Filter {
  private final val LOG = LoggerFactory.getLogger(classOf[KeystoneBasicAuthFilter])
  private final val DEFAULT_CONFIG = "keystone-basic-auth.cfg.xml"

  private var config: String = _
  private var handlerFactory: KeystoneBasicAuthHandlerFactory = _
  private var configurationService: ConfigurationService = _

  override def init(filterConfig: FilterConfig) {
    config = new FilterConfigHelper(filterConfig).getFilterConfig(DEFAULT_CONFIG)
    LOG.info("Initializing filter using config " + config)
    val powerApiContext = ServletContextHelper.getInstance(filterConfig.getServletContext).getPowerApiContext
    configurationService = powerApiContext.configurationService
    handlerFactory = new KeystoneBasicAuthHandlerFactory(powerApiContext.akkaServiceClientService, powerApiContext.datastoreService)
    val xsdURL: URL = getClass.getResource("/META-INF/config/schema/keystone-basic-auth.xsd")
    configurationService.subscribeTo(
      filterConfig.getFilterName,
      config,
      xsdURL,
      handlerFactory.asInstanceOf[UpdateListener[KeystoneBasicAuthConfig]],
      classOf[KeystoneBasicAuthConfig]
    )
  }

  override def doFilter(servletRequest: ServletRequest, servletResponse: ServletResponse, filterChain: FilterChain) {
    new FilterLogicHandlerDelegate(servletRequest, servletResponse, filterChain).doFilter(handlerFactory.newHandler)
  }

  override def destroy() {
    configurationService.unsubscribeFrom(config, handlerFactory)
  }
}
