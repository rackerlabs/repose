package org.openrepose.filters.valkyrieauthorization

import java.io.InputStream
import java.net.URL
import java.util.concurrent.TimeUnit
import javax.inject.{Inject, Named}
import javax.servlet._
import javax.servlet.http.{HttpServletRequest, HttpServletResponse}

import com.rackspace.httpdelegation.HttpDelegationManager
import com.typesafe.scalalogging.slf4j.LazyLogging
import org.openrepose.commons.config.manager.UpdateListener
import org.openrepose.commons.utils.http.{CommonHttpHeader, OpenStackServiceHeader, ServiceClientResponse}
import org.openrepose.commons.utils.servlet.http.{MutableHttpServletRequest, MutableHttpServletResponse}
import org.openrepose.core.filter.FilterConfigHelper
import org.openrepose.core.services.config.ConfigurationService
import org.openrepose.core.services.datastore.DatastoreService
import org.openrepose.core.services.serviceclient.akka.AkkaServiceClient
import org.openrepose.filters.valkyrieauthorization.config.{ValkyrieAuthorizationConfig, ValkyrieServer}

import scala.collection.JavaConverters._
import scala.io.Source
import scala.util.{Failure, Success, Try}

@Named
class ValkyrieAuthorizationFilter @Inject()(configurationService: ConfigurationService, akkaServiceClient: AkkaServiceClient, datastoreService: DatastoreService)
  extends Filter
  with UpdateListener[ValkyrieAuthorizationConfig]
  with HttpDelegationManager
  with LazyLogging {

  private final val DEFAULT_CONFIG = "valkyrie-authorization.cfg.xml"
  val datastore = datastoreService.getDefaultDatastore

  var configurationFile: String = DEFAULT_CONFIG
  var configuration: ValkyrieAuthorizationConfig = _
  var initialized = false

  override def init(filterConfig: FilterConfig): Unit = {
    configurationFile = new FilterConfigHelper(filterConfig).getFilterConfig(DEFAULT_CONFIG)
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
  case class DeviceList(devices: Vector[DeviceToPermission]) extends ValkyrieResult //Vector because List isnt serializable until Scala 2.11
  case class ResponseResult(statusCode: Int, message: String = "") extends ValkyrieResult

  override def doFilter(servletRequest: ServletRequest, servletResponse: ServletResponse, filterChain: FilterChain): Unit = {
    val mutableHttpRequest = MutableHttpServletRequest.wrap(servletRequest.asInstanceOf[HttpServletRequest])
    val mutableHttpResponse = MutableHttpServletResponse.wrap(mutableHttpRequest, servletResponse.asInstanceOf[HttpServletResponse])
    var devicePermissions: DeviceList = null

    def nullOrWhitespace(str: Option[String]): Option[String] = str.map { _.trim }.filter { !"".equals(_) }

    val requestedTenantId = nullOrWhitespace(Option(mutableHttpRequest.getHeader(OpenStackServiceHeader.TENANT_ID.toString)))
    val requestedDeviceId = nullOrWhitespace(Option(mutableHttpRequest.getHeader("X-Device-Id")))
    val requestedContactId = nullOrWhitespace(Option(mutableHttpRequest.getHeader("X-Contact-Id")))
    val requestGuid = nullOrWhitespace(Option(mutableHttpRequest.getHeader(CommonHttpHeader.TRACE_GUID.toString)))

    val clientResponse = ((requestedTenantId, requestedContactId, requestedDeviceId) match {
      case (None, _, _) => ResponseResult(403, "No tenant ID specified")
      case (Some(tenant), _, _) if "(hybrid:.*)".r.findFirstIn(tenant).isEmpty => ResponseResult(403, "Not Authorized")
      case (_, None, _) => ResponseResult(403, "No contact ID specified")
      case (_, _, None) if !nonAuthorizedPath(mutableHttpRequest.getRequestURL.toString) => ResponseResult(403, "No device ID specified")
      case (Some(tenant), Some(contact), device) =>
        val transformedTenant = tenant.substring(tenant.indexOf(":") + 1, tenant.length)

        datastoreValue(transformedTenant, contact, configuration.getValkyrieServer, requestGuid) match {
          case deviceList: DeviceList =>
            devicePermissions = deviceList
            device match {
              case Some(deviceId) => authorize(deviceId, deviceList.devices, mutableHttpRequest.getMethod)
              case None => ResponseResult(200)
            }
          case result: ResponseResult => result
        }
    }) match {
      case ResponseResult(403,_) if configuration.isEnableMasking403S => ResponseResult(404, "Not Found")
      case result => result
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

  def nonAuthorizedPath(url: => String): Boolean = {
    val path: String = new URL(url).getPath
    lazy val onResourceList: Boolean = configuration.getCollectionResources.getResource.asScala.exists { resource =>
      resource.getPathRegex.r.findFirstIn(path).isDefined
    }
    lazy val onWhitelist: Boolean = configuration.getOtherWhitelistedResources.getPathRegex.asScala.exists { pathRegex =>
      pathRegex.r.findFirstIn(path).isDefined
    }
    onResourceList || onWhitelist
  }

  def datastoreValue(transformedTenant: String, contactId: String, valkyrieServer: ValkyrieServer, requestGuid: Option[String] = None): ValkyrieResult = {
    def tryValkyrieCall(): Try[ServiceClientResponse] = {
      import collection.JavaConversions._

      val requestGuidHeader = requestGuid.map(guid => Map(CommonHttpHeader.TRACE_GUID.toString -> guid)).getOrElse(Map())
      val uri = valkyrieServer.getUri + s"/account/$transformedTenant/permissions/contacts/devices/by_contact/$contactId/effective"
      Try(akkaServiceClient.get(cacheKey(transformedTenant, contactId),
        uri,
        Map("X-Auth-User" -> valkyrieServer.getUsername, "X-Auth-Token" -> valkyrieServer.getPassword) ++ requestGuidHeader)
      )
    }

    def valkyrieAuthorize(): ValkyrieResult = {
      tryValkyrieCall() match {
        case Success(response) =>
          if (response.getStatus == 200) {
            parseDevices(response.getData) match {
              case Success(deviceList) =>
                datastore.put(cacheKey(transformedTenant, contactId), deviceList, configuration.getCacheTimeoutMillis, TimeUnit.MILLISECONDS)
                DeviceList(deviceList)
              case Failure(x) => ResponseResult(502, x.getMessage) //JSON Parsing failure
            }
          } else {
            ResponseResult(502, s"Valkyrie returned a ${response.getStatus}") //Didn't get a 200 from valkyrie
          }
        case Failure(exception) =>
          ResponseResult(502, s"Unable to communicate with Valkyrie: ${exception.getMessage}")
      }
    }

    Option(datastore.get(cacheKey(transformedTenant, contactId))) match {
      case Some(x) => DeviceList(x.asInstanceOf[Vector[DeviceToPermission]])
      case None => valkyrieAuthorize()
    }
  }

  def authorize(deviceId: String, deviceList: Vector[DeviceToPermission], method: String): ResponseResult = {
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

  def parseDevices(is: InputStream): Try[Vector[DeviceToPermission]] = {
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
      (json \ "contact_permissions").validate[Vector[DeviceToPermission]] match {
        case s: JsSuccess[Vector[DeviceToPermission]] =>
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
