package org.openrepose.filters.valkyrieauthorization

import java.net.URL
import javax.inject.{Named, Inject}
import javax.servlet._

import com.typesafe.scalalogging.slf4j.LazyLogging
import org.openrepose.commons.config.manager.UpdateListener
import org.openrepose.core.filter.FilterConfigHelper
import org.openrepose.core.filter.logic.common.AbstractFilterLogicHandler
import org.openrepose.core.services.config.ConfigurationService
import org.openrepose.core.services.serviceclient.akka.AkkaServiceClient
import org.openrepose.filters.valkyrieauthorization.config.ValkyrieAuthorizationConfig

@Named
class ValkyrieAuthorizationFilter @Inject() (configurationService: ConfigurationService, akkaServiceClient: AkkaServiceClient)
  extends AbstractFilterLogicHandler
  with Filter
  with UpdateListener[ValkyrieAuthorizationConfig]
  with LazyLogging {

  private final val DEFAULT_CONFIG = "valkyrie-authorization.cfg.xml"

  var configurationFile: String = _

  override def init(filterConfig: FilterConfig): Unit = {
    configurationFile = Option(filterConfig.getInitParameter(FilterConfigHelper.FILTER_CONFIG)).getOrElse(DEFAULT_CONFIG)
    logger.info("Initializing filter using config " + configurationFile)
    val xsdURL: URL = getClass.getResource("/META-INF/schema/config/valkyrie-authorization.xsd")
    configurationService.subscribeTo(
      filterConfig.getFilterName,
      configurationFile,
      xsdURL,
      this,
      classOf[ValkyrieAuthorizationConfig]
    )
  }

  override def destroy(): Unit = {
    configurationService.unsubscribeFrom(configurationFile, this)
  }

  override def doFilter(servletRequest: ServletRequest, servletResponse: ServletResponse, filterChain: FilterChain): Unit = ???

  override def configurationUpdated(configurationObject: ValkyrieAuthorizationConfig): Unit = ???

  override def isInitialized: Boolean = ???
}
