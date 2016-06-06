/*
 * _=_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_=
 * Repose
 * _-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-
 * Copyright (C) 2010 - 2015 Rackspace US, Inc.
 * _-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * =_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_=_
 */
package org.openrepose.filters.valkyrieauthorization

import java.io.{ByteArrayInputStream, InputStream}
import java.net.URL
import java.util.concurrent.TimeUnit
import java.util.regex.PatternSyntaxException
import javax.inject.{Inject, Named}
import javax.servlet._
import javax.servlet.http.HttpServletResponse._
import javax.servlet.http.{HttpServletRequest, HttpServletResponse}
import javax.ws.rs.core.MediaType

import com.fasterxml.jackson.core.JsonParseException
import com.josephpconley.jsonpath.JSONPath
import com.rackspace.httpdelegation.HttpDelegationManager
import com.typesafe.scalalogging.slf4j.LazyLogging
import io.gatling.jsonpath.AST.{Field, PathToken, RootNode}
import io.gatling.jsonpath.Parser
import org.apache.http.HttpHeaders.RETRY_AFTER
import org.openrepose.commons.config.manager.UpdateListener
import org.openrepose.commons.utils.http.normal.ExtendedStatusCodes.SC_TOO_MANY_REQUESTS
import org.openrepose.commons.utils.http.{CommonHttpHeader, OpenStackServiceHeader, ServiceClientResponse}
import org.openrepose.commons.utils.servlet.http.{HttpServletRequestWrapper, HttpServletResponseWrapper, ResponseMode}
import org.openrepose.core.filter.FilterConfigHelper
import org.openrepose.core.services.config.ConfigurationService
import org.openrepose.core.services.datastore.DatastoreService
import org.openrepose.core.services.serviceclient.akka.{AkkaServiceClient, AkkaServiceClientFactory}
import org.openrepose.filters.valkyrieauthorization.config._
import play.api.libs.json._

import scala.annotation.tailrec
import scala.collection.JavaConverters._
import scala.io.Source
import scala.language.postfixOps
import scala.util.{Failure, Success, Try}

@Named
class ValkyrieAuthorizationFilter @Inject()(configurationService: ConfigurationService, akkaServiceClientFactory: AkkaServiceClientFactory, datastoreService: DatastoreService)
  extends Filter
    with UpdateListener[ValkyrieAuthorizationConfig]
    with HttpDelegationManager
    with LazyLogging {

  private final val DEFAULT_CONFIG = "valkyrie-authorization.cfg.xml"
  private final val ACCOUNT_ADMIN = "account_admin"
  private final val CACHE_PREFIX = "VALKYRIE-FILTER"

  val datastore = datastoreService.getDefaultDatastore

  var configurationFile: String = DEFAULT_CONFIG
  var configuration: ValkyrieAuthorizationConfig = _
  var akkaServiceClient: AkkaServiceClient = _
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
    Option(akkaServiceClient).foreach(_.destroy())
    configurationService.unsubscribeFrom(configurationFile, this)
  }

  override def doFilter(servletRequest: ServletRequest, servletResponse: ServletResponse, filterChain: FilterChain): Unit = {
    if (!isInitialized) {
      logger.error("Filter has not yet initialized... Please check your configuration files and your artifacts directory.")
      servletResponse.asInstanceOf[HttpServletResponse].sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE)
    } else {
      val httpRequest = new HttpServletRequestWrapper(servletRequest.asInstanceOf[HttpServletRequest])
      val httpResponse = new HttpServletResponseWrapper(servletResponse.asInstanceOf[HttpServletResponse], ResponseMode.PASSTHROUGH, ResponseMode.MUTABLE)

      def nullOrWhitespace(str: Option[String]): Option[String] = str.map(_.trim).filter(!"".equals(_))

      val requestedTenantId = nullOrWhitespace(Option(httpRequest.getHeader(OpenStackServiceHeader.TENANT_ID.toString)))
      val requestedDeviceId = nullOrWhitespace(Option(httpRequest.getHeader("X-Device-Id")))
      val requestedContactId = nullOrWhitespace(Option(httpRequest.getHeader("X-Contact-Id")))
      val tracingHeader = nullOrWhitespace(Option(httpRequest.getHeader(CommonHttpHeader.TRACE_GUID.toString)))
      val urlPath: String = new URL(httpRequest.getRequestURL.toString).getPath
      val matchingResources: Seq[Resource] = Option(configuration.getCollectionResources)
        .map(_.getResource.asScala.filter(resource => {
          val pathRegex = resource.getPathRegex
          pathRegex.getValue.r.pattern.matcher(urlPath).matches() && {
            val httpMethods = pathRegex.getHttpMethods
            httpMethods.isEmpty ||
              httpMethods.contains(HttpMethod.ALL) ||
              httpMethods.contains(HttpMethod.fromValue(httpRequest.getMethod))
          }
        })).getOrElse(Seq.empty[Resource])
      val translateAccountPermissions: Option[AnyRef] = Option(configuration.getTranslatePermissionsToRoles)

      def checkHeaders(tenantId: Option[String], contactId: Option[String]): ValkyrieResult = {
        (requestedTenantId, requestedContactId) match {
          case (None, _) => ResponseResult(SC_UNAUTHORIZED, "No tenant ID specified")
          case (Some(tenant), _) if "(hybrid:.*)".r.findFirstIn(tenant).isEmpty => ResponseResult(SC_FORBIDDEN, "Not Authorized")
          case (_, None) => ResponseResult(SC_UNAUTHORIZED, "No contact ID specified")
          case (Some(tenant), Some(contact)) => UserInfo(tenant.substring(tenant.indexOf(":") + 1), contact)
        }

      }

      def getPermissions(headerResult: ValkyrieResult): ValkyrieResult = {
        def parsePermissions(inputStream: InputStream): Try[UserPermissions] = {

          @tailrec
          def parseJson(permissionName: List[String], deviceToPermissions: List[DeviceToPermission], values: List[JsValue]): UserPermissions = {
            if (values.isEmpty) {
              UserPermissions(permissionName.toVector, deviceToPermissions.toVector)
            } else {
              val currentPermission: JsValue = values.head
              (currentPermission \ "item_type_name").as[String] match {
                case "accounts" =>
                  parseJson((currentPermission \ "permission_name").as[String] +: permissionName, deviceToPermissions, values.tail)
                case "devices" =>
                  parseJson(permissionName,
                    DeviceToPermission((currentPermission \ "item_id").as[Int], (currentPermission \ "permission_name").as[String]) +: deviceToPermissions, values.tail)
                case _ => parseJson(permissionName, deviceToPermissions, values.tail)
              }
            }
          }

          val input: String = Source.fromInputStream(inputStream).getLines() mkString ""
          try {
            val json = Json.parse(input)
            val permissions: List[JsValue] = (json \ "contact_permissions").as[List[JsValue]]
            Success(parseJson(List.empty[String], List.empty[DeviceToPermission], permissions))
          } catch {
            case e: Exception =>
              logger.error(s"Invalid Json response from Valkyrie: $input", e)
              Failure(new Exception("Invalid Json response from Valkyrie", e))
          }
        }

        headerResult match {
          case UserInfo(tenant, contact) =>
            //  authorize device            || cull list                  || translate account permissions
            if (requestedDeviceId.isDefined || matchingResources.nonEmpty || translateAccountPermissions.isDefined) {
              datastoreValue(tenant, contact, "any", configuration.getValkyrieServer, Option(httpRequest.getHeader("x-auth-token")), _.asInstanceOf[UserPermissions], parsePermissions, tracingHeader)
            } else {
              ResponseResult(SC_OK)
            }
          case _ => headerResult
        }
      }

      def getInventory(userPermissions: ValkyrieResult, checkHeader: ValkyrieResult): ValkyrieResult = {
        def parseInventory(inputStream: InputStream): Try[DevicePermissions] = {

          @tailrec
          def parseJson(deviceToPermissions: List[DeviceToPermission], values: List[JsValue]): DevicePermissions = {
            if (values.isEmpty) {
              DevicePermissions(deviceToPermissions.toVector)
            } else {
              val currentItem: JsValue = values.head
              (currentItem \ "id").as[Int] match {
                case id if id > 0 =>
                  parseJson(DeviceToPermission(id, ACCOUNT_ADMIN) +: deviceToPermissions, values.tail)
                case _ => parseJson(deviceToPermissions, values.tail)
              }
            }
          }

          val input: String = Source.fromInputStream(inputStream).getLines() mkString ""
          try {
            val json = Json.parse(input)
            val inventory: List[JsValue] = (json \ "inventory").as[List[JsValue]]
            Success(parseJson(List.empty[DeviceToPermission], inventory))
          } catch {
            case e: Exception =>
              logger.error(s"Invalid Json response from Valkyrie: $input", e)
              Failure(new Exception("Invalid Json response from Valkyrie", e))
          }
        }

        userPermissions match {
          case UserPermissions(deviceRoles, devicePermissions) =>
            if (!configuration.isEnableBypassAccountAdmin && deviceRoles.contains(ACCOUNT_ADMIN)) {
              val inventoryResult = checkHeader match {
                case UserInfo(tenant, contact) =>
                  datastoreValue(tenant, contact, ACCOUNT_ADMIN, configuration.getValkyrieServer, Option(httpRequest.getHeader("x-auth-token")), _.asInstanceOf[DevicePermissions], parseInventory, tracingHeader)
                case _ => userPermissions
              }
              inventoryResult match {
                case DevicePermissions(adminPermissions) =>
                  UserPermissions(deviceRoles, devicePermissions ++ adminPermissions)
                case _ => inventoryResult
              }
            } else {
              userPermissions
            }
          case _ => userPermissions
        }
      }

      def authorizeDevice(valkyrieCallResult: ValkyrieResult, deviceIdHeader: Option[String]): ValkyrieResult = {
        def authorize(deviceId: String, permissions: UserPermissions, method: String): ValkyrieResult = {
          val deviceBasedResult: ValkyrieResult = permissions.devices.find(_.device.toString == deviceId).map { deviceToPermission =>
            lazy val permissionsWithDevicePermissions = permissions.copy(roles = permissions.roles :+ deviceToPermission.permission)
            deviceToPermission.permission match {
              case "view_product" if List("GET", "HEAD").contains(method) => permissionsWithDevicePermissions
              case "edit_product" => permissionsWithDevicePermissions
              case "admin_product" => permissionsWithDevicePermissions
              case ACCOUNT_ADMIN => permissionsWithDevicePermissions
              case _ => ResponseResult(SC_FORBIDDEN, "Not Authorized")
            }
          } getOrElse {
            ResponseResult(SC_FORBIDDEN, "Not Authorized")
          }

          deviceBasedResult match {
            case ResponseResult(SC_FORBIDDEN, _, _) =>
              if (permissions.roles.contains(ACCOUNT_ADMIN) && configuration.isEnableBypassAccountAdmin) {
                permissions
              } else {
                deviceBasedResult
              }
            case _ => deviceBasedResult
          }
        }

        (valkyrieCallResult, deviceIdHeader) match {
          case (permissions: UserPermissions, Some(deviceId)) => authorize(deviceId, permissions, httpRequest.getMethod)
          case (result, _) => result
        }
      }

      def addRoles(result: ValkyrieResult): ResponseResult = {
        (result, translateAccountPermissions) match {
          case (UserPermissions(roles, _), Some(_)) =>
            roles.foreach(httpRequest.addHeader("X-Roles", _))
            ResponseResult(SC_OK)
          case (UserPermissions(_, _), None) => ResponseResult(SC_OK)
          case (responseResult: ResponseResult, _) => responseResult
        }
      }

      def mask403s(valkyrieResponse: ResponseResult): ResponseResult = {
        valkyrieResponse match {
          case ResponseResult(SC_FORBIDDEN, _, _) if configuration.isEnableMasking403S => ResponseResult(SC_NOT_FOUND, "Not Found")
          case result => result
        }
      }

      val preAuthRoles = Option(configuration.getPreAuthorizedRoles)
        .map(_.getRole.asScala)
        .getOrElse(List.empty)
      val reqAuthRoles = httpRequest.getHeaders(OpenStackServiceHeader.ROLES.toString).asScala.toSeq
        .foldLeft(List.empty[String])((list: List[String], value: String) => list ++ value.split(","))

      if (preAuthRoles.intersect(reqAuthRoles).nonEmpty) {
        filterChain.doFilter(httpRequest, httpResponse)
      } else {
        val checkHeader = checkHeaders(requestedTenantId, requestedContactId)
        val userPermissions = getPermissions(checkHeader)
        val allPermissions = getInventory(userPermissions, checkHeader)
        val authPermissions = authorizeDevice(allPermissions, requestedDeviceId)
        mask403s(addRoles(authPermissions)) match {
          case ResponseResult(SC_OK, _, _) =>
            filterChain.doFilter(httpRequest, httpResponse)
            val status = httpResponse.getStatus
            if (SC_OK <= status && status < SC_MULTIPLE_CHOICES) {
              try {
                cullResponse(httpResponse, authPermissions, matchingResources)
              } catch {
                case rce: ResponseCullingException =>
                  logger.debug("Failed to cull response, wiping out response.", rce)
                  sendError(httpResponse, SC_INTERNAL_SERVER_ERROR, rce.getMessage)
              }
            }
          case ResponseResult(code, message, retryTime) if Option(configuration.getDelegating).isDefined =>
            buildDelegationHeaders(code, "valkyrie-authorization", message, configuration.getDelegating.getQuality).foreach { case (key, values) =>
              values.foreach { value => httpRequest.addHeader(key, value) }
            }
            filterChain.doFilter(httpRequest, httpResponse)
            retryTime.foreach(httpResponse.addHeader(RETRY_AFTER, _))
          case ResponseResult(code, message, retryTime) =>
            sendError(httpResponse, code, message, retryTime)
        }
      }

      httpResponse.commitToResponse()
    }
  }

  def datastoreValue(transformedTenant: String,
                     contactId: String,
                     callType: String,
                     valkyrieServer: ValkyrieServer,
                     authToken: Option[String],
                     datastoreTransform: java.io.Serializable => ValkyrieResult,
                     responseParser: InputStream => Try[java.io.Serializable],
                     tracingHeader: Option[String] = None): ValkyrieResult = {
    def tryValkyrieCall(): Try[ServiceClientResponse] = {
      import collection.JavaConversions._
      val requestTracingHeader = tracingHeader.map(guid => Map(CommonHttpHeader.TRACE_GUID.toString -> guid)).getOrElse(Map())
      val uri = if (callType.equals(ACCOUNT_ADMIN)) {
        s"/account/$transformedTenant/inventory"
      } else {
        s"/account/$transformedTenant/permissions/contacts/$callType/by_contact/$contactId/effective"
      }
      (Option(valkyrieServer.getUsername), Option(valkyrieServer.getPassword)) match {
        case (Some(username), Some(password)) =>
          Try(akkaServiceClient.get(cacheKey(callType, transformedTenant, contactId),
            valkyrieServer.getUri + uri,
            Map("X-Auth-User" -> username, "X-Auth-Token" -> password) ++ requestTracingHeader)
          )
        case _ =>
          Try(akkaServiceClient.get(cacheKey(callType, transformedTenant, contactId),
            valkyrieServer.getUri + uri,
            Map("X-Auth-Token" -> authToken.getOrElse(null)) ++ requestTracingHeader)
          )
      }
    }

    def valkyrieAuthorize(): ValkyrieResult = {
      tryValkyrieCall() match {
        case Success(response) =>
          if (response.getStatus == SC_OK) {
            responseParser(response.getData) match {
              case Success(values) =>
                datastore.put(cacheKey(callType, transformedTenant, contactId), values, configuration.getCacheTimeoutMillis, TimeUnit.MILLISECONDS)
                datastoreTransform(values)
              case Failure(x) => ResponseResult(SC_BAD_GATEWAY, x.getMessage) //JSON Parsing failure
            }
          } else {
            val retryTime = response.getHeaders.find(_.getName == RETRY_AFTER).map(_.getValue)
            (Option(configuration.getValkyrieServer.getUsername), Option(configuration.getValkyrieServer.getPassword)) match {
              //admin creds
              case (Some(_), Some(_)) =>
                response.getStatus match {
                  case SC_BAD_REQUEST => ResponseResult(SC_INTERNAL_SERVER_ERROR, "Valkyrie rejected the request for being bad")
                  case SC_UNAUTHORIZED => ResponseResult(SC_INTERNAL_SERVER_ERROR, "Valkyrie said the credentials weren't authorized")
                  case SC_FORBIDDEN => ResponseResult(SC_INTERNAL_SERVER_ERROR, "Valkyrie said the credentials were forbidden")
                  case SC_INTERNAL_SERVER_ERROR => ResponseResult(SC_BAD_GATEWAY, "Valkyrie failed for an unspecified reason")
                  case SC_REQUEST_ENTITY_TOO_LARGE | SC_TOO_MANY_REQUESTS | SC_SERVICE_UNAVAILABLE => ResponseResult(SC_SERVICE_UNAVAILABLE, "Valkyrie rate limited the request", retryTime)
                  case statusCode => ResponseResult(SC_BAD_GATEWAY, s"Valkyrie returned a $statusCode")
                }
              //user token
              case _ =>
                response.getStatus match {
                  case SC_BAD_REQUEST => ResponseResult(SC_INTERNAL_SERVER_ERROR, "Valkyrie rejected the request for being bad")
                  case SC_UNAUTHORIZED => ResponseResult(SC_UNAUTHORIZED, "Valkyrie said the user was unauthorized")
                  case SC_FORBIDDEN => ResponseResult(SC_FORBIDDEN, "Valkyrie said the user was forbidden")
                  case SC_INTERNAL_SERVER_ERROR => ResponseResult(SC_BAD_GATEWAY, "Valkyrie failed for an unspecified reason")
                  case SC_REQUEST_ENTITY_TOO_LARGE => ResponseResult(SC_REQUEST_ENTITY_TOO_LARGE, "Valkyrie rate limited the request", retryTime)
                  case SC_TOO_MANY_REQUESTS => ResponseResult(SC_TOO_MANY_REQUESTS, "Valkyrie rate limited the request", retryTime)
                  case SC_SERVICE_UNAVAILABLE => ResponseResult(SC_SERVICE_UNAVAILABLE, "Valkyrie rate limited the request", retryTime)
                  case statusCode => ResponseResult(SC_BAD_GATEWAY, s"Valkyrie returned a $statusCode")
                }
            }
          }
        case Failure(exception) =>
          ResponseResult(SC_BAD_GATEWAY, s"Unable to communicate with Valkyrie: ${exception.getMessage}")
      }
    }

    Option(datastore.get(cacheKey(callType, transformedTenant, contactId))) match {
      case Some(x) => datastoreTransform(x)
      case None => valkyrieAuthorize()
    }
  }

  def cacheKey(typeOfCall: String, transformedTenant: String, contactId: String): String = {
    CACHE_PREFIX + typeOfCall + transformedTenant + contactId
  }

  def cullResponse(response: HttpServletResponseWrapper, potentialUserPermissions: ValkyrieResult,
                   matchingResources: Seq[Resource]): Unit = {

    def getJsPathFromString(jsonPath: String): JsPath = {
      val pathTokens: List[PathToken] = (new Parser).compile(jsonPath).getOrElse({
        throw MalformedJsonPathException(s"Unable to parse JsonPath: $jsonPath")
      })
      pathTokens.foldLeft(new JsPath) { (path, token) =>
        token match {
          case RootNode => path
          case Field(name) => path \ name
        }
      }
    }

    def cullJsonArray(jsonArray: Seq[JsValue], devicePath: DevicePath, devicePermissions: Vector[DeviceToPermission]): Seq[JsValue] = {
      def extractDeviceIdFieldValue(jsValue: JsValue): Try[String] = {
        Try(JSONPath.query(devicePath.getPath, jsValue)) match {
          case Success(value) => value match {
            case jsValue: JsNumber =>
              Success(jsValue.value.toString())
            case jsValue: JsString =>
              Success(jsValue.value)
            case _ =>
              Failure(InvalidJsonTypeException(s"Invalid JSON type in: ${devicePath.getPath}"))
          }
          case Failure(e) =>
            Failure(InvalidJsonPathException(s"Invalid path specified for device id: ${devicePath.getPath}", e))
        }
      }

      def parseDeviceId(fieldValue: String): Try[String] = {
        Try {
          val matcher = devicePath.getRegex.getValue.r.pattern.matcher(fieldValue)
          if (matcher.matches()) {
            matcher.group(devicePath.getRegex.getCaptureGroup)
          } else {
            throw NonMatchingRegexException(s"Regex: ${devicePath.getRegex.getValue} did not match $fieldValue")
          }
        } recoverWith {
          case pse: PatternSyntaxException => Failure(MalformedRegexException("Unable to parse regex for device id", pse))
          case ioobe: IndexOutOfBoundsException => Failure(InvalidCaptureGroupException("Bad capture group specified", ioobe))
        }
      }

      jsonArray filter { value =>
        extractDeviceIdFieldValue(value) flatMap { deviceIdFieldValue =>
          parseDeviceId(deviceIdFieldValue)
        } map { deviceId =>
          devicePermissions.exists(_.device == deviceId.toInt)
        } recover {
          case e@(_: InvalidJsonTypeException | _: NonMatchingRegexException) =>
            configuration.getCollectionResources.getDeviceIdMismatchAction match {
              case DeviceIdMismatchAction.KEEP => true
              case DeviceIdMismatchAction.REMOVE => false
              case DeviceIdMismatchAction.FAIL => throw e
            }
        } get
      }
    }

    def updateItemCount(json: JsObject, pathToItemCount: String, newCount: Int): JsObject = {
      Option(pathToItemCount) match {
        case Some(path) =>
          Try(JSONPath.query(path, json)) match {
            case Success(v) =>
              val countTransform: Reads[JsObject] = getJsPathFromString(path).json.update(__.read[JsNumber].map { _ => new JsNumber(newCount) })
              json.transform(countTransform).getOrElse {
                throw TransformException("Unable to transform json while updating the count.")
              }
            case Failure(e) => throw InvalidJsonPathException(s"Invalid path specified for item count: $path", e)
          }
        case None => json
      }
    }

    potentialUserPermissions match {
      case UserPermissions(roles, devicePermissions) =>
        if (!configuration.isEnableBypassAccountAdmin || !roles.contains(ACCOUNT_ADMIN)) {
          if (matchingResources.nonEmpty) {
            val input: String = Source.fromInputStream(response.getOutputStreamAsInputStream).getLines() mkString ""
            val initialJson: JsValue = Try(Json.parse(input))
              .recover({ case jpe: JsonParseException => throw UnexpectedJsonException("Response contained improper json.", jpe) })
              .get
            val finalJson = matchingResources.foldLeft(initialJson) { (resourceJson, resource) =>
              resource.getCollection.asScala.foldLeft(resourceJson) { (collectionJson, collection) =>
                val array: Seq[JsValue] = Try(JSONPath.query(collection.getJson.getPathToCollection, collectionJson).as[Seq[JsValue]])
                  .recover({ case e: Exception if e.getMessage.equals("Bad JSONPath query Couldn't find field") =>
                    throw InvalidJsonPathException(s"Invalid path specified for collection: ${collection.getJson.getPathToCollection}", e) })
                  .get

                val culledArray: Seq[JsValue] = cullJsonArray(array, collection.getJson.getPathToDeviceId, devicePermissions)

                //these are a little complicated, look here for details: https://www.playframework.com/documentation/2.2.x/ScalaJsonTransformers
                val arrayTransform: Reads[JsObject] = getJsPathFromString(collection.getJson.getPathToCollection).json.update(__.read[JsArray].map { _ => new JsArray(culledArray) })
                val transformedJson = collectionJson.transform(arrayTransform).getOrElse({
                  throw TransformException("Unable to transform json while culling list.")
                })

                updateItemCount(transformedJson, collection.getJson.getPathToItemCount, culledArray.size)
              }
            }
            // Replace the existing output with the modified output
            response.setOutput(new ByteArrayInputStream(finalJson.toString().getBytes(response.getCharacterEncoding)))
          }
        }
      case _ =>
    }
  }

  // todo: remove this function when the HttpServletResponseWrapper supports sendError without writing through
  private def sendError(response: HttpServletResponseWrapper, statusCode: Int, message: String, retryTime: Option[String] = None): Unit = {
    retryTime.foreach(response.addHeader(RETRY_AFTER, _))
    response.setStatus(statusCode)
    response.setOutput(null)
    response.setContentType(MediaType.TEXT_PLAIN)
    response.getOutputStream.print(message)
  }

  override def configurationUpdated(configurationObject: ValkyrieAuthorizationConfig): Unit = {
    configuration = configurationObject

    val akkaServiceClientOld = Option(akkaServiceClient)
    akkaServiceClient = akkaServiceClientFactory.newAkkaServiceClient(configuration.getConnectionPoolId)
    akkaServiceClientOld.foreach(_.destroy())

    initialized = true
  }

  override def isInitialized: Boolean = initialized

  trait ValkyrieResult

  case class UserInfo(tenantId: String, contactId: String) extends ValkyrieResult

  case class UserPermissions(roles: Vector[String], devices: Vector[DeviceToPermission]) extends ValkyrieResult

  case class DevicePermissions(devices: Vector[DeviceToPermission]) extends ValkyrieResult

  case class DeviceToPermission(device: Int, permission: String)

  case class ResponseResult(statusCode: Int, message: String = "", retryTime: Option[String] = None) extends ValkyrieResult

}
