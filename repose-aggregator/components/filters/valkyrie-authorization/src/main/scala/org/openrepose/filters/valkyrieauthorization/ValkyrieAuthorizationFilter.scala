package org.openrepose.filters.valkyrieauthorization

import java.io.InputStream
import java.net.URL
import javax.inject.{Inject, Named}
import javax.servlet._
import javax.servlet.http.{HttpServletRequest, HttpServletResponse}

import com.rackspace.httpdelegation.HttpDelegationManager
import com.typesafe.scalalogging.slf4j.LazyLogging
import org.openrepose.commons.config.manager.UpdateListener
import org.openrepose.commons.utils.http.{ServiceClientResponse, OpenStackServiceHeader}
import org.openrepose.commons.utils.servlet.http.{MutableHttpServletRequest, MutableHttpServletResponse}
import org.openrepose.core.filter.FilterConfigHelper
import org.openrepose.core.filter.logic.common.AbstractFilterLogicHandler
import org.openrepose.core.services.config.ConfigurationService
import org.openrepose.core.services.serviceclient.akka.AkkaServiceClient
import org.openrepose.filters.valkyrieauthorization.config.{ValkyrieServer, ValkyrieAuthorizationConfig}

import scala.io.Source
import scala.util.{Success, Failure, Try}

@Named
class ValkyrieAuthorizationFilter @Inject()(configurationService: ConfigurationService, akkaServiceClient: AkkaServiceClient)
  extends AbstractFilterLogicHandler
  with Filter
  with UpdateListener[ValkyrieAuthorizationConfig]
  with HttpDelegationManager
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
    val mutableHttpRequest = MutableHttpServletRequest.wrap(servletRequest.asInstanceOf[HttpServletRequest])
    val mutableHttpResponse = MutableHttpServletResponse.wrap(mutableHttpRequest, servletResponse.asInstanceOf[HttpServletResponse])

    case class ValkyrieResult(statusCode: Int, message: String = "")
    def headerResult(headerName: String, valkyrieResult: ValkyrieResult): Either[String, ValkyrieResult] = {
      Option(mutableHttpRequest.getHeader(headerName)).map {
        Left(_)
      } getOrElse {
        Right(valkyrieResult)
      }
    }

    val requestedTenantId = headerResult(OpenStackServiceHeader.TENANT_ID.toString, ValkyrieResult(502, "No tenant ID specified"))
    val requestedDeviceId = headerResult("X-Device-Id", ValkyrieResult(502, "No device ID specified"))
    val requestedContactId = headerResult("X-Contact-Id", ValkyrieResult(403, "No contact ID specified"))
    val valkyrieServer = configuration.getValkyrieServer

    val clientResponse: ValkyrieResult = (requestedTenantId, requestedContactId, requestedDeviceId) match {
      case (_, _, Right(x)) => x
      case (_, Right(x), _) => x
      case (Right(x), _, _) => x
      case (Left(tenantId), Left(contactId), Left(deviceId)) =>
        tryValkyrieCall(valkyrieServer, tenantId, contactId) match {
          case Success(response) => {
            if (response.getStatus == 200) {
              parseDevices(response.getData) match {
                case Success(deviceList) => {
                  deviceList.find(_.device.toString == deviceId).map { deviceToPermission =>
                    deviceToPermission.permission match {
                      case "view_product" if List("GET", "HEAD").contains(mutableHttpRequest.getMethod) => ValkyrieResult(200)
                      case "edit_product" => ValkyrieResult(200)
                      case "admin_product" => ValkyrieResult(200)
                      case _ => ValkyrieResult(403, "Not Authorized")
                    }
                  } getOrElse {
                    ValkyrieResult(403, "Not Authorized")
                  }
                }
                case Failure(x) => {
                  //JSON Parsing failure
                  ValkyrieResult(502, x.getMessage)
                }
              }
            } else {
              //Didn't get a 200 from valkyrie
              ValkyrieResult(502, s"Valkyrie returned a ${response.getStatus}")
            }
          }
          case Failure(exception) => {
            ValkyrieResult(502, s"Unable to communicate with Valkyrie: ${exception.getMessage}")
          }
        }

    }

    clientResponse match {
      case ValkyrieResult(200, _) =>
        filterChain.doFilter(mutableHttpRequest, mutableHttpResponse)
      case ValkyrieResult(code, message) if Option(configuration.getDelegating).isDefined =>
        buildDelegationHeaders(code, "valkyrie-authorization", message, configuration.getDelegating.getQuality).foreach { case (key, values) =>
          values.foreach { value => mutableHttpRequest.addHeader(key, value) }
        }
        filterChain.doFilter(mutableHttpRequest, mutableHttpResponse)
      case ValkyrieResult(code, message) =>
        mutableHttpResponse.sendError(code, message)
    }
  }

  def tryValkyrieCall(valkyrieServer: ValkyrieServer, tenantId: String, contactId: String): Try[ServiceClientResponse] = {
    import collection.JavaConversions._

    val transformedTenant = tenantId.substring(tenantId.indexOf(":") + 1, tenantId.length)
    val uri = valkyrieServer.getUri + s"/account/$transformedTenant/permissions/contacts/devices/by_contact/$contactId/effective"
    Try(akkaServiceClient.get(transformedTenant + contactId,
      uri,
      Map("X-Auth-User" -> valkyrieServer.getUsername, "X-Auth-Token" -> valkyrieServer.getPassword))
    )

  }

  case class DeviceToPermission(device: Int, permission: String)

  def parseDevices(is: InputStream): Try[Seq[DeviceToPermission]] = {
    import play.api.libs.json.Reads._
    import play.api.libs.json._
    import play.api.libs.functional.syntax._

    implicit val deviceToPermissions = (
      (JsPath \ "item_id").read[Int] and
        (JsPath \ "permission_name").read[String]
      )(DeviceToPermission.apply _)

    val input: String = Source.fromInputStream(is).getLines() mkString ""
    try {
      val json = Json.parse(input)
      (json \ "contact_permissions").validate[Seq[DeviceToPermission]] match {
        case s: JsSuccess[Seq[DeviceToPermission]] =>
          Success(s.get)
        case f: JsError =>
          logger.error(s"Valkyrie Response did not match expected contract: ${
            JsError.toFlatJson(f)
          }")
          Failure(new Exception("Valkyrie Response did not match expected contract"))
      }
    } catch {
      case e: Exception =>
        logger.error(s"Invalid Json response from Valkyrie: $input", e)
        Failure(new Exception("Invalid Json response from Valkyrie"))
    }
  }

  override def configurationUpdated(configurationObject: ValkyrieAuthorizationConfig): Unit = {
    configuration = configurationObject
    initialized = true
  }

  override def isInitialized: Boolean = initialized
}
