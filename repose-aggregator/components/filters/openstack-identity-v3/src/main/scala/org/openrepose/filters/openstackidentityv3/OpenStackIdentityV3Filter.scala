package org.openrepose.filters.openstackidentityv3

import java.net.URL
import javax.servlet._

import com.rackspace.papi.components.openstack.identity.v3.config.OpenstackIdentityV3Config
import com.rackspace.papi.filter.FilterConfigHelper
import com.rackspace.papi.filter.logic.impl.FilterLogicHandlerDelegate
import com.rackspace.papi.service.config.ConfigurationService
import com.rackspace.papi.service.context.ServletContextHelper
import org.openrepose.commons.config.manager.UpdateListener
import org.slf4j.LoggerFactory

class OpenStackIdentityV3Filter extends Filter {

  private final val LOG = LoggerFactory.getLogger(classOf[OpenStackIdentityV3Filter])
  private final val DEFAULT_CONFIG = "openstack-identity-v3.cfg.xml"

  private var config: String = _
  private var handlerFactory: OpenStackIdentityV3HandlerFactory = _
  private var configurationService: ConfigurationService = _

  override def init(filterConfig: FilterConfig) {
    config = new FilterConfigHelper(filterConfig).getFilterConfig(DEFAULT_CONFIG)
    LOG.info("Initializing filter using config " + config)
    val powerApiContext = ServletContextHelper.getInstance(filterConfig.getServletContext).getPowerApiContext
    configurationService = powerApiContext.configurationService
    handlerFactory = new OpenStackIdentityV3HandlerFactory(powerApiContext.akkaServiceClientService, powerApiContext.datastoreService)
    val xsdURL: URL = getClass.getResource("/META-INF/config/schema/openstack-identity-v3.xsd")
    // TODO: Clean up the asInstanceOf below, if possible?
    configurationService.subscribeTo(filterConfig.getFilterName, config, xsdURL, handlerFactory.asInstanceOf[UpdateListener[OpenstackIdentityV3Config]], classOf[OpenstackIdentityV3Config])
  }

  override def doFilter(servletRequest: ServletRequest, servletResponse: ServletResponse, filterChain: FilterChain) {
    new FilterLogicHandlerDelegate(servletRequest, servletResponse, filterChain).doFilter(handlerFactory.newHandler)
  }

  override def destroy() {
    configurationService.unsubscribeFrom(config, handlerFactory)
  }
}
