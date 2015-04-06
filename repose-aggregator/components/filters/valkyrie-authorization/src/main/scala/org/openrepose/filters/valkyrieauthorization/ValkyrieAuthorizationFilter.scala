package org.openrepose.filters.valkyrieauthorization

import java.io.InputStream
import java.net.URL
import javax.inject.{Inject, Named}
import javax.servlet._
import javax.servlet.http.{HttpServletRequest, HttpServletResponse}

import com.typesafe.scalalogging.slf4j.LazyLogging
import org.openrepose.commons.config.manager.UpdateListener
import org.openrepose.commons.utils.http.OpenStackServiceHeader
import org.openrepose.commons.utils.servlet.http.{MutableHttpServletRequest, MutableHttpServletResponse}
import org.openrepose.core.filter.FilterConfigHelper
import org.openrepose.core.filter.logic.common.AbstractFilterLogicHandler
import org.openrepose.core.services.config.ConfigurationService
import org.openrepose.core.services.serviceclient.akka.AkkaServiceClient
import org.openrepose.filters.valkyrieauthorization.config.ValkyrieAuthorizationConfig

import scala.io.Source

@Named
class ValkyrieAuthorizationFilter @Inject()(configurationService: ConfigurationService, akkaServiceClient: AkkaServiceClient)
  extends AbstractFilterLogicHandler
  with Filter
  with UpdateListener[ValkyrieAuthorizationConfig]
  with LazyLogging {

  private final val DEFAULT_CONFIG = "valkyrie-authorization.cfg.xml"

  var configurationFile: String = DEFAULT_CONFIG
  var configuration: ValkyrieAuthorizationConfig = _
  var initialized = false

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

  override def doFilter(servletRequest: ServletRequest, servletResponse: ServletResponse, filterChain: FilterChain): Unit = {
    import collection.JavaConversions._

    val mutableHttpRequest = MutableHttpServletRequest.wrap(servletRequest.asInstanceOf[HttpServletRequest])
    val mutableHttpResponse = MutableHttpServletResponse.wrap(mutableHttpRequest, servletResponse.asInstanceOf[HttpServletResponse])

    val tenant = Option(mutableHttpRequest.getHeader(OpenStackServiceHeader.TENANT_ID.toString))
    val device = mutableHttpRequest.getHeader("X-Device-Id")
    val contact = Option(mutableHttpRequest.getHeader("X-Contact-Id"))
    val valkyrieServer = configuration.getValkyrieServer

    val clientResponse = (tenant, contact) match {
      case (Some(tenantId), Some(contactId)) =>
        val transformedTenant = tenantId.substring(tenantId.indexOf(":")+1, tenantId.length)
        val uri =  valkyrieServer.getUri + s"/account/$transformedTenant/permissions/contacts/devices/by_contact/$contactId/effective"
        Option(akkaServiceClient.get(transformedTenant + contactId, uri, Map("X-Auth-User" -> valkyrieServer.getUsername, "X-Auth-Token" -> valkyrieServer.getPassword)))
      case (_,_) => None
    }

    val valkyrieResponse = clientResponse.map { response =>
      if(response.getStatus == 200) {
        parseDevices(response.getData)
      } else {
        Seq.empty[DeviceToPermission]
      }
    }.getOrElse(Seq.empty[DeviceToPermission])

    val accept = valkyrieResponse.find(_.device.toString == device).map { deviceToPermission =>
      deviceToPermission.permission match {
        case "view_product" if List("GET", "HEAD").contains(mutableHttpRequest.getMethod) => Some
        case "edit_product" => Some
        case "admin_product" => Some
        case _ => None
      }
    }

    if (accept.isDefined) {
      filterChain.doFilter(mutableHttpRequest, mutableHttpResponse)
    } else if (clientResponse.isEmpty || clientResponse.get.getStatus != 200 || device == null) {
      mutableHttpResponse.sendError(HttpServletResponse.SC_BAD_GATEWAY)
    } else {
      mutableHttpResponse.sendError(HttpServletResponse.SC_FORBIDDEN)
    }
  }

  case class DeviceToPermission(device: Int, permission: String)

  def parseDevices(is: InputStream): Seq[DeviceToPermission] = {
    import play.api.libs.json.Reads._
    import play.api.libs.json._
    import play.api.libs.functional.syntax._

    implicit val deviceToPermissions = (
      (JsPath \ "item_id").read[Int] and
      (JsPath \ "permission_name").read[String]
    )(DeviceToPermission.apply _)

    val json = Json.parse(Source.fromInputStream(is).getLines() mkString)
    (json \ "contact_permissions").validate[Seq[DeviceToPermission]] match {
      case s: JsSuccess[Seq[DeviceToPermission]] =>
        s.get
      case f: JsError =>
        logger.debug(s"Failure Parsing Valkyrie Response: ${
          JsError.toFlatJson(f)
        }")
        Seq.empty[DeviceToPermission]
    }
  }

  override def configurationUpdated(configurationObject: ValkyrieAuthorizationConfig): Unit = {
    configuration = configurationObject
    initialized = true
  }

  override def isInitialized: Boolean = initialized
}
