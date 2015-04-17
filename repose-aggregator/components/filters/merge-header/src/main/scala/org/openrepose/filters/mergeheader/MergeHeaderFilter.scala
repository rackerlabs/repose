package org.openrepose.filters.mergeheader

import javax.inject.{Inject, Named}
import javax.servlet._

import com.typesafe.scalalogging.slf4j.LazyLogging
import org.openrepose.commons.config.manager.UpdateListener
import org.openrepose.core.filter.FilterConfigHelper
import org.openrepose.core.filter.logic.impl.FilterLogicHandlerDelegate
import org.openrepose.core.services.config.ConfigurationService

@Named
class MergeHeaderFilter @Inject()(configurationService: ConfigurationService) extends Filter with LazyLogging {
  private final val DEFAULT_CONFIG = "merge-header.cfg.xml"

  private var config: String = _
  private var handlerFactory: MergeHeaderHandlerFactory = _

  override def init(filterConfig: FilterConfig): Unit = {
    config = new FilterConfigHelper(filterConfig).getFilterConfig(DEFAULT_CONFIG)
    logger.info(s"Initializing MergeHeaderFilter using config $config")
    handlerFactory = new MergeHeaderHandlerFactory()
    val xsdURL = getClass.getResource("/META-INF/config/schema/merge-header.xsd")
    // TODO: Clean up the asInstanceOf below, if possible?
    configurationService.subscribeTo(filterConfig.getFilterName,
      config,
      xsdURL,
      handlerFactory.asInstanceOf[UpdateListener[MergeHeaderConfig]],
      classOf[MergeHeaderConfig])
  }

  override def doFilter(servletRequest: ServletRequest, servletResponse: ServletResponse, filterChain: FilterChain): Unit = {
    new FilterLogicHandlerDelegate(servletRequest, servletResponse, filterChain).doFilter(handlerFactory.newHandler)
  }

  override def destroy(): Unit = {
    configurationService.unsubscribeFrom(config, handlerFactory)
  }
}
