package org.openrepose.filters.valkyrieauthorization

import java.io.InputStream
import java.net.URL
import javax.inject.{Inject, Named}
import javax.servlet._
import javax.servlet.http.{HttpServletRequest, HttpServletResponse}

import com.rackspace.httpdelegation.HttpDelegationManager
import com.typesafe.scalalogging.slf4j.LazyLogging
import org.openrepose.commons.config.manager.UpdateListener
import org.openrepose.commons.utils.http.{OpenStackServiceHeader, ServiceClientResponse}
import org.openrepose.commons.utils.servlet.http.{MutableHttpServletRequest, MutableHttpServletResponse}
import org.openrepose.core.filter.FilterConfigHelper
import org.openrepose.core.filter.logic.common.AbstractFilterLogicHandler
import org.openrepose.core.services.config.ConfigurationService
import org.openrepose.core.services.datastore.DatastoreService
import org.openrepose.core.services.serviceclient.akka.AkkaServiceClient
import org.openrepose.filters.valkyrieauthorization.config.{ValkyrieAuthorizationConfig, ValkyrieServer}

import scala.io.Source
import scala.util.{Failure, Success, Try}

@Named
class ValkyrieAuthorizationFilter @Inject()(configurationService: ConfigurationService, akkaServiceClient: AkkaServiceClient, datastoreService: DatastoreService)
  extends AbstractFilterLogicHandler
  with Filter
  with UpdateListener[ValkyrieAuthorizationConfig]
  with HttpDelegationManager
  with LazyLogging {

  private final val DEFAULT_CONFIG = "valkyrie-authorization.cfg.xml"
  val datastore = datastoreService.getDefaultDatastore

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

  trait ValkyrieResult
  case class DeviceToPermission(device: Int, permission: String)
  case class DeviceList(devices: Seq[DeviceToPermission]) extends ValkyrieResult
  case class ResponseResult(statusCode: Int, message: String = "") extends ValkyrieResult

  override def doFilter(servletRequest: ServletRequest, servletResponse: ServletResponse, filterChain: FilterChain): Unit = {
    val mutableHttpRequest = MutableHttpServletRequest.wrap(servletRequest.asInstanceOf[HttpServletRequest])
    val mutableHttpResponse = MutableHttpServletResponse.wrap(mutableHttpRequest, servletResponse.asInstanceOf[HttpServletResponse])

    def headerResult(headerName: String, valkyrieResult: ResponseResult): Either[String, ResponseResult] = {
      Option(mutableHttpRequest.getHeader(headerName)).map {
        Left(_)
      } getOrElse {
        Right(valkyrieResult)
      }
    }

    val requestedTenantId = headerResult(OpenStackServiceHeader.TENANT_ID.toString, ResponseResult(502, "No tenant ID specified"))
    val requestedDeviceId = headerResult("X-Device-Id", ResponseResult(502, "No device ID specified"))
    val requestedContactId = headerResult("X-Contact-Id", ResponseResult(403, "No contact ID specified"))
    val clientResponse: ResponseResult = (requestedTenantId, requestedContactId, requestedDeviceId) match {
      case (_, _, Right(x)) => x
      case (_, Right(x), _) => x
      case (Right(x), _, _) => x
      case (Left(tenantId), Left(contactId), Left(deviceId)) =>
        tenantId.replaceAll(".*:", "")
        val transformedTenant = tenantId.substring(tenantId.indexOf(":") + 1, tenantId.length)
        datastoreValue(transformedTenant, contactId, deviceId, configuration.getValkyrieServer, mutableHttpRequest.getMethod) match {
          case x:DeviceList => authorize (deviceId, x.devices, mutableHttpRequest.getMethod)
          case x:ResponseResult => x
        }
    }

    clientResponse match {
      case ResponseResult(200, _) =>
        filterChain.doFilter(mutableHttpRequest, mutableHttpResponse)
      case ResponseResult(code, message) if Option(configuration.getDelegating).isDefined =>
        buildDelegationHeaders(code, "valkyrie-authorization", message, configuration.getDelegating.getQuality).foreach { case (key, values) =>
          values.foreach { value => mutableHttpRequest.addHeader(key, value) }
        }
        filterChain.doFilter(mutableHttpRequest, mutableHttpResponse)
      case ResponseResult(code, message) =>
        mutableHttpResponse.sendError(code, message)
    }
  }

  def cacheKey(transformedTenant: String, contactId: String): String = {
    transformedTenant + contactId
  }

  def datastoreValue(transformedTenant: String, contactId: String, deviceId: String,  valkyrieServer: ValkyrieServer, method: String): ValkyrieResult = {
    Option(datastore.get(cacheKey(transformedTenant, contactId))) match {
      case Some(x: Seq[DeviceToPermission]) => DeviceList(x)
      case None => valkyrieAuthorize(valkyrieServer, transformedTenant, contactId, deviceId, method)
    }
  }

  def valkyrieAuthorize(valkyrieServer: ValkyrieServer, tenantId: String, contactId: String, deviceId: String, method: String): ValkyrieResult = {
    tryValkyrieCall(valkyrieServer, tenantId, contactId) match {
      case Success(response) => {
        if (response.getStatus == 200) {
          parseDevices(response.getData) match {
            case Success(deviceList) => DeviceList(deviceList)
            case Failure(x) => ResponseResult(502, x.getMessage) //JSON Parsing failure
          }
        } else {
          ResponseResult(502, s"Valkyrie returned a ${response.getStatus}") //Didn't get a 200 from valkyrie
        }
      }
      case Failure(exception) => {
        ResponseResult(502, s"Unable to communicate with Valkyrie: ${exception.getMessage}")
      }
    }
  }

  def authorize(deviceId: String, deviceList: Seq[DeviceToPermission], method: String): ResponseResult = {
    deviceList.find(_.device.toString == deviceId).map { deviceToPermission =>
      deviceToPermission.permission match {
        case "view_product" if List("GET", "HEAD").contains(method) => ResponseResult(200)
        case "edit_product" => ResponseResult(200)
        case "admin_product" => ResponseResult(200)
        case _ => ResponseResult(403, "Not Authorized")
      }
    } getOrElse {
      ResponseResult(403, "Not Authorized")
    }
  }

  def tryValkyrieCall(valkyrieServer: ValkyrieServer, tenantId: String, contactId: String): Try[ServiceClientResponse] = {
    import collection.JavaConversions._

    val uri = valkyrieServer.getUri + s"/account/$tenantId/permissions/contacts/devices/by_contact/$contactId/effective"
    Try(akkaServiceClient.get(cacheKey(tenantId, contactId),
      uri,
      Map("X-Auth-User" -> valkyrieServer.getUsername, "X-Auth-Token" -> valkyrieServer.getPassword))
    )

  }

  def parseDevices(is: InputStream): Try[Seq[DeviceToPermission]] = {
    import play.api.libs.functional.syntax._
    import play.api.libs.json.Reads._
    import play.api.libs.json._

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
