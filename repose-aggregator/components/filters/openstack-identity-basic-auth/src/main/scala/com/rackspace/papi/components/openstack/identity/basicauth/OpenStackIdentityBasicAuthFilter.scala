package com.rackspace.papi.components.openstack.identity.basicauth

import java.net.URL
import javax.servlet._

import com.rackspace.papi.commons.config.manager.UpdateListener
import com.rackspace.papi.components.openstack.identity.basicauth.config.OpenStackIdentityBasicAuthConfig
import com.rackspace.papi.filter.FilterConfigHelper
import com.rackspace.papi.filter.logic.impl.FilterLogicHandlerDelegate
import com.rackspace.papi.service.config.ConfigurationService
import com.rackspace.papi.service.context.ServletContextHelper
import org.slf4j.LoggerFactory

class OpenStackIdentityBasicAuthFilter extends Filter {
  private final val LOG = LoggerFactory.getLogger(classOf[OpenStackIdentityBasicAuthFilter])
  private final val DEFAULT_CONFIG = "openstack-identity-basic-auth.cfg.xml"

  private var config: String = _
  private var handlerFactory: OpenStackIdentityBasicAuthHandlerFactory = _
  private var configurationService: ConfigurationService = _

  override def init(filterConfig: FilterConfig) {
    config = new FilterConfigHelper(filterConfig).getFilterConfig(DEFAULT_CONFIG)
    LOG.info("Initializing filter using config " + config)
    val powerApiContext = ServletContextHelper.getInstance(filterConfig.getServletContext).getPowerApiContext
    configurationService = powerApiContext.configurationService
    handlerFactory = new OpenStackIdentityBasicAuthHandlerFactory(powerApiContext.akkaServiceClientService, powerApiContext.datastoreService)
    val xsdURL: URL = getClass.getResource("/META-INF/config/schema/openstack-identity-basic-auth.xsd")
    configurationService.subscribeTo(
      filterConfig.getFilterName,
      config,
      xsdURL,
      handlerFactory.asInstanceOf[UpdateListener[OpenStackIdentityBasicAuthConfig]],
      classOf[OpenStackIdentityBasicAuthConfig]
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
