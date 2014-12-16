package org.openrepose.filters.herp

import java.net.URL
import javax.servlet._
import javax.servlet.http.{HttpServletRequest, HttpServletResponse}

import com.rackspace.httpdelegation._
import com.typesafe.scalalogging.slf4j.LazyLogging
import org.openrepose.commons.config.manager.UpdateListener
import org.openrepose.commons.utils.http.{CommonHttpHeader, OpenStackServiceHeader}
import org.openrepose.core.filter.FilterConfigHelper
import org.openrepose.core.services.config.ConfigurationService
import org.openrepose.core.services.context.ServletContextHelper
import org.openrepose.filters.herp.config.HerpConfig
import org.slf4j.{Logger, LoggerFactory}
import play.api.libs.json.Json

import scala.collection.JavaConverters._

class HerpFilter extends Filter with HttpDelegationManager with UpdateListener[HerpConfig] with LazyLogging {
  private final val DEFAULT_CONFIG = "highly-efficient-record-processor.cfg.xml"

  private var configurationService: ConfigurationService = _
  private var config: String = _
  private var initialized = false
  private var herpLogger: Logger = _
  private var serviceCode: String = _
  private var region: String = _
  private var dataCenter: String = _

  override def init(filterConfig: FilterConfig): Unit = {
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

  override def doFilter(servletRequest: ServletRequest, servletResponse: ServletResponse, filterChain: FilterChain): Unit = {
    if (!initialized) {
      logger.error("HERP filter has not yet initialized...")
      servletResponse.asInstanceOf[HttpServletResponse].sendError(500)
    } else {
      logger.trace("HERP filter passing request...")
      filterChain.doFilter(servletRequest, servletResponse)

      logger.trace("HERP filter handling response...")
      handleResponse(servletRequest.asInstanceOf[HttpServletRequest], servletResponse.asInstanceOf[HttpServletResponse])

      logger.trace("HERP filter returning response...")
    }
  }

  private def handleResponse(httpServletRequest: HttpServletRequest,
                             httpServletResponse: HttpServletResponse) = {
    val userName = httpServletRequest.getHeader(OpenStackServiceHeader.USER_NAME.toString)
    val impersonatorName = httpServletRequest.getHeader(OpenStackServiceHeader.IMPERSONATOR_NAME.toString)
    val tenantID = httpServletRequest.getHeader(OpenStackServiceHeader.TENANT_ID.toString)
    val rbacRoles = httpServletRequest.getHeaders(OpenStackServiceHeader.ROLES.toString).asScala.toArray
    val userAgent = httpServletRequest.getHeader(CommonHttpHeader.USER_AGENT.toString)
    val requestMethod = httpServletRequest.getMethod
    val requestURL = Option(httpServletRequest.getAttribute("http://openrepose.org/requestUrl")).map(_.asInstanceOf[String]).orNull
    val parameters = Option(httpServletRequest.getAttribute("http://openrepose.org/queryParams")).map(_.asInstanceOf[java.util.Map[String, Array[String]]].asScala.toMap).getOrElse(Map[String, Array[String]]())
    val timestamp = System.currentTimeMillis()
    val responseCode = httpServletResponse.getStatus
    val responseMessage = "response message"
    val guid = java.util.UUID.randomUUID.toString

    val jsonObject = Json.obj(
      "GUID" -> Json.toJson(guid),
      "ServiceCode" -> Json.toJson(serviceCode),
      "Region" -> Json.toJson(region),
      "DataCenter" -> Json.toJson(dataCenter),
      "Timestamp" -> Json.toJson(timestamp),
      "Request" -> Json.obj(
        "Method" -> Json.toJson(requestMethod),
        "URL" -> Json.toJson(requestURL),
        "Parameters" -> Json.toJson(parameters),
        "UserName" -> Json.toJson(userName),
        "ImpersonatorName" -> Json.toJson(impersonatorName),
        "TenantID" -> Json.toJson(tenantID),
        "RbacRoles" -> Json.toJson(rbacRoles),
        "UserAgent" -> Json.toJson(userAgent)
      ),
      "Response" -> Json.obj(
        "Code" -> Json.toJson(responseCode),
        "Message" -> Json.toJson(responseMessage)
      )
    )

    herpLogger.info(Json.stringify(jsonObject))
  }

  override def destroy(): Unit = {
    logger.trace("HERP filter destroying ...")
    configurationService.unsubscribeFrom(config, this.asInstanceOf[UpdateListener[_]])

    logger.trace("HERP filter destroyed.")
  }

  override def configurationUpdated(config: HerpConfig): Unit = {
    herpLogger = LoggerFactory.getLogger(config.getId)
    serviceCode = config.getServiceCode
    region = config.getRegion
    dataCenter = config.getDataCenter
    initialized = true
  }

  override def isInitialized: Boolean = {
    initialized
  }
}
