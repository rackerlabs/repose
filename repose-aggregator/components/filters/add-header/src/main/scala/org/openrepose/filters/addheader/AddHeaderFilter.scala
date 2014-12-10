package org.openrepose.filters.addheader

import javax.servlet._

import com.typesafe.scalalogging.slf4j.LazyLogging
import org.openrepose.commons.config.manager.UpdateListener
import org.openrepose.core.filter.FilterConfigHelper
import org.openrepose.core.filter.logic.impl.FilterLogicHandlerDelegate
import org.openrepose.core.services.config.ConfigurationService
import org.openrepose.core.services.context.ServletContextHelper
import org.openrepose.filters.addheader.config.AddHeaderType


/**
 * Created by dimi5963 on 12/4/14.
 */
class AddHeaderFilter  extends Filter with LazyLogging {
  private final val DEFAULT_CONFIG = "add-header.cfg.xml"

  private var config: String = _
  private var handlerFactory: AddHeaderHandlerFactory = _
  private var configurationService: ConfigurationService = _


  override def init(filterConfig: FilterConfig): Unit = {
    config = new FilterConfigHelper(filterConfig).getFilterConfig(DEFAULT_CONFIG)
    logger.info(s"Initializing AddHeaderFilter using config $config")

    //Spring hack -- as copied from RackspaceAuthUserFilter
    val powerApiContext = ServletContextHelper.getInstance(filterConfig.getServletContext).getPowerApiContext
    configurationService = powerApiContext.configurationService()
    handlerFactory = new AddHeaderHandlerFactory()

    val xsdURL = getClass.getResource("/META-INF/config/schema/add-header-configuration.xsd")
    configurationService.subscribeTo(filterConfig.getFilterName,
      config,
      xsdURL,
      handlerFactory.asInstanceOf[UpdateListener[AddHeaderType]],
      classOf[AddHeaderType])

  }

  override def doFilter(request: ServletRequest, response: ServletResponse, chain: FilterChain): Unit = {
    new FilterLogicHandlerDelegate(request, response, chain).doFilter(handlerFactory.newHandler)

  }

  override def destroy(): Unit = {
    configurationService.unsubscribeFrom(config, handlerFactory)
  }
}
