package com.rackspace.identity.repose.authidentity

import javax.servlet._

import com.rackspace.papi.commons.config.manager.UpdateListener
import com.rackspace.papi.filter.FilterConfigHelper
import com.rackspace.papi.filter.logic.impl.FilterLogicHandlerDelegate
import com.rackspace.papi.service.config.ConfigurationService
import com.rackspace.papi.service.context.ServletContextHelper
import com.typesafe.scalalogging.slf4j.LazyLogging

class RackspaceAuthIdentityFilter extends Filter with LazyLogging {

  private final val DEFAULT_CONFIG = "rackspace-auth-identity.cfg.xml"

  private var config: String = _
  private var handlerFactory: RackspaceAuthIdentityHandlerFactory = _
  private var configurationService: ConfigurationService = _


  override def init(filterConfig: FilterConfig): Unit = {
    config = new FilterConfigHelper(filterConfig).getFilterConfig(DEFAULT_CONFIG)
    logger.info(s"Initializing RackspaceAuthIdentityFilter using config $config")

    //Ew, nasty spring hax
    val powerApiContext = ServletContextHelper.getInstance(filterConfig.getServletContext).getPowerApiContext
    configurationService = powerApiContext.configurationService()
    handlerFactory = new RackspaceAuthIdentityHandlerFactory()

    val xsdURL = getClass.getResource("/META-INF/config/schema/rackspace-auth-identity-configuration.xsd")
    configurationService.subscribeTo(filterConfig.getFilterName,
      config,
      xsdURL,
      handlerFactory.asInstanceOf[UpdateListener[RackspaceAuthIdentityConfig]],
      classOf[RackspaceAuthIdentityConfig])

  }

  override def doFilter(request: ServletRequest, response: ServletResponse, chain: FilterChain): Unit = {
    new FilterLogicHandlerDelegate(request, response, chain).doFilter(handlerFactory.newHandler)

  }

  override def destroy(): Unit = {
    configurationService.unsubscribeFrom(config, handlerFactory)
  }
}
