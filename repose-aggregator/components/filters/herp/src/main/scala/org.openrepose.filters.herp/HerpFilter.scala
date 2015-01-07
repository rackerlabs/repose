package org.openrepose.filters.herp

import java.io.{StringReader, StringWriter}
import java.net.URL
import javax.servlet._
import javax.servlet.http.{HttpServletRequest, HttpServletResponse}

import com.github.mustachejava.{DefaultMustacheFactory, Mustache}
import com.rackspace.httpdelegation._
import com.typesafe.scalalogging.slf4j.LazyLogging
import org.openrepose.commons.config.manager.UpdateListener
import org.openrepose.commons.utils.http.{CommonHttpHeader, HttpStatusCode, OpenStackServiceHeader}
import org.openrepose.core.filter.FilterConfigHelper
import org.openrepose.core.services.config.ConfigurationService
import org.openrepose.core.services.context.ServletContextHelper
import org.openrepose.filters.herp.config.HerpConfig
import org.slf4j.{Logger, LoggerFactory}

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
  private var mustacheTemplate: Mustache = _

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
    def translateParameters(): Map[String, Array[Parameter]] = {
      Option(httpServletRequest.getAttribute("http://openrepose.org/queryParams")) match {
        case Some(parameters) => {
          val parametersMap = parameters.asInstanceOf[java.util.Map[String, Array[String]]].asScala
          parametersMap.mapValues((values :Array[String]) => values.map(Parameter(_))).toMap
        }
        case None => Map[String, Array[Parameter]]()
      }
    }

    val templateValues = Map(
      "userName" -> httpServletRequest.getHeader(OpenStackServiceHeader.USER_NAME.toString),
      "impersonatorName" -> httpServletRequest.getHeader(OpenStackServiceHeader.IMPERSONATOR_NAME.toString),
      "tenantID" -> httpServletRequest.getHeader(OpenStackServiceHeader.TENANT_ID.toString),
      "roles" -> httpServletRequest.getHeaders(OpenStackServiceHeader.ROLES.toString).asScala.map(Role(_)).toArray,
      "userAgent" -> httpServletRequest.getHeader(CommonHttpHeader.USER_AGENT.toString),
      "requestMethod" -> httpServletRequest.getMethod,
      "requestURL" -> Option(httpServletRequest.getAttribute("http://openrepose.org/requestUrl")).map(_.toString).orNull,
      "parameters" -> translateParameters().asJava.entrySet(),
      "timestamp" -> System.currentTimeMillis(),
      "responseCode" -> httpServletResponse.getStatus,
      "responseMessage" -> HttpStatusCode.fromInt(httpServletResponse.getStatus).name(),
      "guid" -> java.util.UUID.randomUUID.toString,
      "serviceCode" -> serviceCode,
      "region" -> region,
      "dataCenter" -> dataCenter
    )

    val templateOutput: StringWriter = new StringWriter
    mustacheTemplate.execute(templateOutput, templateValues.asJava)

    herpLogger.info(templateOutput.toString)
  }

  override def destroy(): Unit = {
    logger.trace("HERP filter destroying ...")
    configurationService.unsubscribeFrom(config, this.asInstanceOf[UpdateListener[_]])

    logger.trace("HERP filter destroyed.")
  }

  override def configurationUpdated(config: HerpConfig): Unit = {
    def template: StringReader = {
      var templateText = config.getTemplate.getValue.trim
      if(config.getTemplate.isCrush) {
        templateText = templateText.replaceAll("(?m)[ \\t]*(\\r\\n|\\r|\\n)[ \\t]*", " ")
      }
      new StringReader(templateText)
    }

    herpLogger = LoggerFactory.getLogger(config.getLoggerName)
    serviceCode = config.getServiceCode
    region = config.getRegion
    dataCenter = config.getDataCenter
    def mustacheFactory = new DefaultMustacheFactory()
    mustacheTemplate = mustacheFactory.compile(template, "herp template")
    initialized = true
  }

  override def isInitialized: Boolean = {
    initialized
  }
}

case class Role(name: String) {}
case class Parameter(value: String) {}
