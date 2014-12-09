package org.openrepose.filters.rackspaceauthuser

import javax.inject.{Inject, Named}
import javax.servlet._

import com.typesafe.scalalogging.slf4j.LazyLogging
import org.openrepose.commons.config.manager.UpdateListener
import org.openrepose.core.filter.FilterConfigHelper
import org.openrepose.core.filter.logic.impl.FilterLogicHandlerDelegate
import org.openrepose.core.services.config.ConfigurationService

@Named
class RackspaceAuthUserFilter @Inject() (configurationService: ConfigurationService) extends Filter with LazyLogging {
  private final val DEFAULT_CONFIG = "rackspace-auth-user.cfg.xml"

  private var config: String = _
  private var handlerFactory: RackspaceAuthUserHandlerFactory = _

  override def init(filterConfig: FilterConfig): Unit = {
    config = new FilterConfigHelper(filterConfig).getFilterConfig(DEFAULT_CONFIG)
    logger.info(s"Initializing RackspaceAuthUserFilter using config $config")
    handlerFactory = new RackspaceAuthUserHandlerFactory()
    val xsdURL = getClass.getResource("/META-INF/config/schema/rackspace-auth-user-configuration.xsd")
    // TODO: Clean up the asInstanceOf below, if possible?
    configurationService.subscribeTo(filterConfig.getFilterName,
      config,
      xsdURL,
      handlerFactory.asInstanceOf[UpdateListener[RackspaceAuthUserConfig]],
      classOf[RackspaceAuthUserConfig])
  }

  override def doFilter(servletRequest: ServletRequest, servletResponse: ServletResponse, filterChain: FilterChain): Unit = {
    new FilterLogicHandlerDelegate(servletRequest, servletResponse, filterChain).doFilter(handlerFactory.newHandler)
  }

  override def destroy(): Unit = {
    configurationService.unsubscribeFrom(config, handlerFactory)
  }
}
