package com.rackspace.papi.components.keystone.v3

import java.net.URL
import javax.servlet._

import com.rackspace.papi.commons.config.manager.UpdateListener
import com.rackspace.papi.components.keystone.v3.config.KeystoneV3Config
import com.rackspace.papi.filter.FilterConfigHelper
import com.rackspace.papi.filter.logic.impl.FilterLogicHandlerDelegate
import com.rackspace.papi.service.config.ConfigurationService
import com.rackspace.papi.service.context.ServletContextHelper
import org.slf4j.LoggerFactory

class KeystoneV3Filter extends Filter {

  private final val LOG = LoggerFactory.getLogger(classOf[KeystoneV3Filter])
  private final val DEFAULT_CONFIG = "keystone-v3.cfg.xml"

  private var config: String = _
  private var handlerFactory: KeystoneV3HandlerFactory = _
  private var configurationService: ConfigurationService = _

  override def init(filterConfig: FilterConfig) {
    config = new FilterConfigHelper(filterConfig).getFilterConfig(DEFAULT_CONFIG)
    LOG.info("Initializing filter using config " + config)
    val powerApiContext = ServletContextHelper.getInstance(filterConfig.getServletContext).getPowerApiContext
    configurationService = powerApiContext.configurationService
    // TODO: These services are passed in to support asynchronous requests, caching, and connection pooling (in the future!)
    handlerFactory = new KeystoneV3HandlerFactory(powerApiContext.akkaServiceClientService, powerApiContext.datastoreService)
    val xsdURL: URL = getClass.getResource("/META-INF/config/schema/keystone-v3.xsd")
    // TODO: Clean up the asInstanceOf below, if possible?
    configurationService.subscribeTo(filterConfig.getFilterName, config, xsdURL, handlerFactory.asInstanceOf[UpdateListener[KeystoneV3Config]], classOf[KeystoneV3Config])
  }

  override def doFilter(servletRequest: ServletRequest, servletResponse: ServletResponse, filterChain: FilterChain) {
    new FilterLogicHandlerDelegate(servletRequest, servletResponse, filterChain).doFilter(handlerFactory.newHandler)
  }

  override def destroy() {
    configurationService.unsubscribeFrom(config, handlerFactory)
  }
}
