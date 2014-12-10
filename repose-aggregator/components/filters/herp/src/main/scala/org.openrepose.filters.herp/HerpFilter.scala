package org.openrepose.filters.herp

import java.net.URL
import javax.servlet._
import javax.servlet.http.{HttpServletRequest, HttpServletResponse}

import com.rackspace.httpdelegation._
import org.openrepose.commons.config.manager.UpdateListener
import org.openrepose.commons.utils.servlet.http.{HttpServletHelper, MutableHttpServletRequest, MutableHttpServletResponse}
import org.openrepose.core.filter.FilterConfigHelper
import org.openrepose.core.filter.logic.impl.FilterDirectorImpl
import org.openrepose.core.filter.logic.{FilterAction, FilterDirector}
import org.openrepose.core.services.config.ConfigurationService
import org.openrepose.core.services.context.ServletContextHelper
import org.openrepose.filters.herp.config.HerpConfig
import org.slf4j.{Logger, LoggerFactory}

class HerpFilter extends Filter with HttpDelegationManager with UpdateListener[HerpConfig] {

  private final val LOG = LoggerFactory.getLogger(classOf[HerpFilter])
  private final val DEFAULT_CONFIG = "highly-efficient-record-processor.cfg.xml"

  private var configurationService: ConfigurationService = _
  private var config: String = _
  private var initialized = false
  private var herpLogger: Option[Logger] = None

  override def init(filterConfig: FilterConfig) = {
    LOG.trace("HERP filter initializing ...")
    config = new FilterConfigHelper(filterConfig).getFilterConfig(DEFAULT_CONFIG)
    LOG.info("Initializing filter using config " + config)
    val powerApiContext = ServletContextHelper.getInstance(filterConfig.getServletContext).getPowerApiContext
    configurationService = powerApiContext.configurationService
    val xsdURL: URL = getClass.getResource("/META-INF/schema/config/highly-efficient-record-processor.xsd")
    configurationService.subscribeTo(
      filterConfig.getFilterName,
      config,
      xsdURL,
      this,
      classOf[HerpConfig]
    )
    LOG.trace("HERP filter initialized.")
  }

  override def doFilter(servletRequest: ServletRequest, servletResponse: ServletResponse, filterChain: FilterChain) = {
    HttpServletHelper.verifyRequestAndResponse(LOG, servletRequest, servletResponse)
    val filterDirector: FilterDirector = new FilterDirectorImpl()
    val mutableHttpRequest: MutableHttpServletRequest = MutableHttpServletRequest.wrap(servletRequest.asInstanceOf[HttpServletRequest])
    val mutableHttpResponse: MutableHttpServletResponse = MutableHttpServletResponse.wrap(mutableHttpRequest, servletResponse.asInstanceOf[HttpServletResponse])

    handleRequest(mutableHttpRequest, mutableHttpResponse, filterDirector)
    filterDirector.applyTo(mutableHttpResponse)
    filterDirector.getFilterAction match {
      case FilterAction.NOT_SET =>
        filterChain.doFilter(mutableHttpRequest, mutableHttpResponse)
      case FilterAction.PASS =>
        filterChain.doFilter(mutableHttpRequest, mutableHttpResponse)
      case FilterAction.PROCESS_RESPONSE =>
        filterChain.doFilter(mutableHttpRequest, mutableHttpResponse)
        handleResponse(mutableHttpRequest, mutableHttpResponse, filterDirector)
        filterDirector.applyTo(mutableHttpResponse)
      case FilterAction.RETURN | _ =>
      // Just Return
    }
  }

  private def handleRequest(httpServletRequest: HttpServletRequest,
                            httpServletResponse: HttpServletResponse,
                            filterDirector: FilterDirector) = {
    LOG.trace("HERP filter handling Request ...")
    filterDirector.setFilterAction(FilterAction.PROCESS_RESPONSE)
    if (initialized) {
      herpLogger.get.error("This is a message from the HERP filter.")
    }
    LOG.trace("HERP filter handled Request.")
  }

  private def handleResponse(httpServletRequest: HttpServletRequest,
                             httpServletResponse: HttpServletResponse,
                             filterDirector: FilterDirector) = {
    LOG.trace("HERP filter handling Response ...")
    LOG.trace("HERP filter handled Response.")
  }

  override def destroy() = {
    LOG.trace("HERP filter destroying ...")
    configurationService.unsubscribeFrom(config, this.asInstanceOf[UpdateListener[_]])
    LOG.trace("HERP filter destroyed.")
  }

  override def configurationUpdated(config: HerpConfig) = {
    herpLogger = Some(LoggerFactory.getLogger(config.getId))
    initialized = herpLogger.isDefined
  }

  override def isInitialized = {
    initialized
  }
}
