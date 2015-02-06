package org.openrepose.filters.herp

import java.io.StringWriter
import java.net.{URL, URLDecoder}
import java.nio.charset.StandardCharsets
import java.util
import javax.inject.{Inject, Named}
import javax.servlet._
import javax.servlet.http.{HttpServletRequest, HttpServletResponse}

import com.github.jknack.handlebars.{Handlebars, Template}
import com.rackspace.httpdelegation._
import com.typesafe.scalalogging.slf4j.LazyLogging
import org.openrepose.commons.config.manager.UpdateListener
import org.openrepose.commons.utils.http.{CommonHttpHeader, OpenStackServiceHeader}
import org.openrepose.core.filter.FilterConfigHelper
import org.openrepose.core.services.config.ConfigurationService
import org.openrepose.filters.herp.config.{FilterOut, HerpConfig}
import org.slf4j.{Logger, LoggerFactory}
import org.springframework.http.HttpStatus

import scala.collection.JavaConverters._
import scala.util.Try
import scala.util.matching.Regex

@Named
class HerpFilter @Inject()(configurationService: ConfigurationService) extends Filter with HttpDelegationManager with UpdateListener[HerpConfig] with LazyLogging {
  private final val DEFAULT_CONFIG = "highly-efficient-record-processor.cfg.xml"
  private final val X_PROJECT_ID = "X-Project-ID"

  private var config: String = _
  private var initialized = false
  private var preLogger: Logger = _
  private var postLogger: Logger = _
  private var serviceCode: String = _
  private var region: String = _
  private var dataCenter: String = _
  private var handlebarsTemplate: Template = _
  private var filtersOut: Iterable[Iterable[(String, Regex)]] = _

  override def init(filterConfig: FilterConfig): Unit = {
    logger.trace("HERP filter initializing ...")
    config = new FilterConfigHelper(filterConfig).getFilterConfig(DEFAULT_CONFIG)

    logger.info("Initializing filter using config " + config)
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
    trait Value {
      def toJava: Any
    }
    case class StringValue(value: Option[String]) extends Value {
      override def toJava: Any = {
        value.orNull
      }
    }
    case class StringArrayValue(value: Option[Array[String]]) extends Value {
      override def toJava: Any = {
        value.orNull
      }
    }
    case class MapValue(value: Option[Map[String, Array[String]]]) extends Value {
      override def toJava: Any = {
        val x = value.orNull
        if (x == null) {
          null
        } else {
          x.asJava.entrySet()
        }
      }
    }
    case class LongValue(value: Option[Long]) extends Value {
      override def toJava: Any = {
        value.orNull
      }
    }

    def translateParameters(): Map[String, Array[String]] = {
      def decode(s: String) = URLDecoder.decode(s, StandardCharsets.UTF_8.name)

      Option(httpServletRequest.getAttribute("http://openrepose.org/queryParams")) match {
        case Some(parameters) =>
          val parametersMap = parameters.asInstanceOf[java.util.Map[String, Array[String]]].asScala
          parametersMap.map({ case (key, values) => decode(key) -> values.map(value => decode(value))}).toMap
        case None => Map[String, Array[String]]()
      }
    }

    def doFilterOut(valuesMap: Map[String, Value]): Boolean = {
      val hitFilters = filtersOut.filter { andFilter =>
        andFilter.forall { case (keyOrig, pattern) =>
          val keySplit = keyOrig.split('.')
          val foundMatch: Boolean = valuesMap(keySplit(0)) match {
            case LongValue(Some(x)) => pattern.findFirstIn(x.toString).isDefined
            case StringValue(Some(x)) => pattern.findFirstIn(x).isDefined
            case StringArrayValue(Some(x)) => x.filter { s => pattern.findFirstIn(s).isDefined}.nonEmpty
            case MapValue(Some(x)) =>
              // IF there is a sub key,
              // THEN try the map;
              // ELSE just bail.
              if (keySplit.size > 1) {
                x.getOrElse(keySplit(1), Array("")).filter { s => pattern.findFirstIn(s).isDefined}.nonEmpty
              } else {
                false
              }
          }
          foundMatch
        }
      }
      hitFilters.nonEmpty
    }

    val eventValues: Map[String, Value] = Map(
      "userName" -> StringValue(Option(httpServletRequest.getHeader(OpenStackServiceHeader.USER_NAME.toString))),
      "impersonatorName" -> StringValue(Option(httpServletRequest.getHeader(OpenStackServiceHeader.IMPERSONATOR_NAME.toString))),
      "projectID" -> StringArrayValue(Option(httpServletRequest.getHeaders(OpenStackServiceHeader.TENANT_ID.toString).asScala
        .++(httpServletRequest.getHeaders(X_PROJECT_ID).asScala).toArray)),
      "roles" -> StringArrayValue(Option(httpServletRequest.getHeaders(OpenStackServiceHeader.ROLES.toString).asScala.toArray)),
      "userAgent" -> StringValue(Option(httpServletRequest.getHeader(CommonHttpHeader.USER_AGENT.toString))),
      "requestMethod" -> StringValue(Option(httpServletRequest.getMethod)),
      "requestURL" -> StringValue(Option(httpServletRequest.getAttribute("http://openrepose.org/requestUrl")).map(_.toString)),
      "requestQueryString" -> StringValue(Option(httpServletRequest.getQueryString)),
      "parameters" -> MapValue(Option(translateParameters())),
      "timestamp" -> LongValue(Option(System.currentTimeMillis)),
      "responseCode" -> LongValue(Option(httpServletResponse.getStatus)),
      "responseMessage" -> StringValue(Option(Try(HttpStatus.valueOf(httpServletResponse.getStatus).name).getOrElse("UNKNOWN"))),
      "guid" -> StringValue(Option(java.util.UUID.randomUUID.toString)),
      "serviceCode" -> StringValue(Option(serviceCode)),
      "region" -> StringValue(Option(region)),
      "dataCenter" -> StringValue(Option(dataCenter))
    )

    val templateOutput: StringWriter = new StringWriter
    handlebarsTemplate.apply(eventValues.map { case (k, v) => k -> v.toJava}.asJava, templateOutput)

    preLogger.info(templateOutput.toString)
    if (!doFilterOut(eventValues)) {
      postLogger.info(templateOutput.toString)
    }
  }

  override def destroy(): Unit = {
    logger.trace("HERP filter destroying ...")
    configurationService.unsubscribeFrom(config, this.asInstanceOf[UpdateListener[_]])

    logger.trace("HERP filter destroyed.")
  }

  override def configurationUpdated(config: HerpConfig): Unit = {
    def templateString: String = {
      var templateText = config.getTemplate.getValue.trim
      if (config.getTemplate.isCrush) {
        templateText = templateText.replaceAll("(?m)[ \\t]*(\\r\\n|\\r|\\n)[ \\t]*", " ")
      }
      templateText
    }

    preLogger = LoggerFactory.getLogger(config.getPreFilterLoggerName)
    postLogger = LoggerFactory.getLogger(config.getPostFilterLoggerName)
    serviceCode = config.getServiceCode
    region = config.getRegion
    dataCenter = config.getDataCenter
    def handlebars = new Handlebars
    handlebarsTemplate = handlebars.compileInline(templateString)
    filtersOut = processFiltersOut(config.getFilterOut.asScala)
    initialized = true

    def processFiltersOut(filters: Iterable[FilterOut]): Iterable[Iterable[(String, Regex)]] = {
      filters.map { filter =>
        filter.getMatch.asScala.map { matcher =>
          (matcher.getField, matcher.getRegex.r)
        }
      }
    }
  }

  override def isInitialized: Boolean = {
    initialized
  }
}
