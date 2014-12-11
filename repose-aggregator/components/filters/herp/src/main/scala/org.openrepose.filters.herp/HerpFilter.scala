package org.openrepose.filters.herp

import java.net.URL
import javax.servlet._
import javax.servlet.http.{HttpServletRequest, HttpServletResponse}

import com.rackspace.httpdelegation._
import com.typesafe.scalalogging.slf4j.LazyLogging
import org.openrepose.commons.config.manager.UpdateListener
import org.openrepose.commons.utils.servlet.http.{MutableHttpServletRequest, MutableHttpServletResponse}
import org.openrepose.core.filter.FilterConfigHelper
import org.openrepose.core.filter.logic.impl.FilterDirectorImpl
import org.openrepose.core.filter.logic.{FilterAction, FilterDirector}
import org.openrepose.core.services.config.ConfigurationService
import org.openrepose.core.services.context.ServletContextHelper
import org.openrepose.filters.herp.config.HerpConfig
import org.slf4j.{Logger, LoggerFactory}
import play.api.libs.json.Json

class HerpFilter extends Filter with HttpDelegationManager with UpdateListener[HerpConfig] with LazyLogging {
  private final val DEFAULT_CONFIG = "highly-efficient-record-processor.cfg.xml"

  private var configurationService: ConfigurationService = _
  private var config: String = _
  private var initialized = false
  private var herpLogger: Option[Logger] = None

  override def init(filterConfig: FilterConfig) = {
    logger.trace("HERP filter initializing ...")
    config = new FilterConfigHelper(filterConfig).getFilterConfig(DEFAULT_CONFIG)
    logger.info("Initializing filter using config " + config)
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
    logger.trace("HERP filter initialized.")
  }

  override def doFilter(servletRequest: ServletRequest, servletResponse: ServletResponse, filterChain: FilterChain) = {
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
    logger.trace("HERP filter handling Request ...")
    filterDirector.setFilterAction(FilterAction.PROCESS_RESPONSE)
    logger.trace("HERP filter handled Request.")
  }

  private def handleResponse(httpServletRequest: HttpServletRequest,
                             httpServletResponse: HttpServletResponse,
                             filterDirector: FilterDirector) = {
    logger.trace("HERP filter handling Response ...")
    if (initialized) {
      val headers =
      val userName = "A - User Name"
      val impersonatorName = "B - Impersonator Name"
      val tenantID = "C - Tenant ID"
      val rbacRoles = "D - RBAC Roles"
      val userAgent = "E - User Agent"
      val requestMethod = "POST"
      val requestURL = "http://www.example.com/derp/derp?herp=derp"
      val eventName = "H - Event Name"
      val serviceCode = "I - Service Code"
      val region = "J - Region"
      val dataCenter = "K - Data Center"
      val parameters = paramsToMapStringString(httpServletRequest.getParameterMap)
      val timestamp = System.currentTimeMillis()
      val responseCode = httpServletResponse.getStatus
      val responseMessage = "O - Response Message"
      val guid = java.util.UUID.randomUUID

      val jsonObject = Json.toJson(
        Map(
          "UserName" -> Json.toJson(userName),
          "ImpersonatorName" -> Json.toJson(impersonatorName),
          "TenantID" -> Json.toJson(tenantID),
          "RbacRoles" -> Json.toJson(rbacRoles),
          "Useragent" -> Json.toJson(userAgent),
          "RequestMethod" -> Json.toJson(requestMethod),
          "RequestURL" -> Json.toJson(requestURL),
          "EventName" -> Json.toJson(eventName),
          "ServiceCode" -> Json.toJson(serviceCode),
          "Region" -> Json.toJson(region),
          "DataCenter" -> Json.toJson(dataCenter),
          "Parameters" -> Json.toJson(parameters),
          "Timestamp" -> Json.toJson(timestamp),
          "Response" -> Map(
            "Code" -> Json.toJson(responseCode),
            "Message" -> Json.toJson(responseMessage)
          ),
          "GUID" -> Json.toJson(guid)
        )
      )
      herpLogger.get.info(Json.stringify(jsonObject))
    }
    logger.trace("HERP filter handled Response.")
  }

  private def paramsToMapStringString(params: Map[String, Array[String]]): Map[String, String] = {
    params.toList flatMap {x => x._2 map { y => x._1 -> y }}
  }

  override def destroy() = {
    logger.trace("HERP filter destroying ...")
    configurationService.unsubscribeFrom(config, this.asInstanceOf[UpdateListener[_]])
    logger.trace("HERP filter destroyed.")
  }

  override def configurationUpdated(config: HerpConfig) = {
    herpLogger = Some(LoggerFactory.getLogger(config.getId))
    initialized = herpLogger.isDefined
  }

  override def isInitialized = {
    initialized
  }
}
