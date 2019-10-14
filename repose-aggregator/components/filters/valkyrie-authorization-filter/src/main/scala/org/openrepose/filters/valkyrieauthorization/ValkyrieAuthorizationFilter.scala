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

import java.io.ByteArrayInputStream
import java.net.URL
import java.util
import java.util.concurrent.TimeUnit
import java.util.regex.PatternSyntaxException

import com.fasterxml.jackson.core.JsonParseException
import com.jayway.jsonpath.{DocumentContext, InvalidJsonException, JsonPath, PathNotFoundException}
import com.rackspace.httpdelegation.HttpDelegationManager
import com.typesafe.scalalogging.StrictLogging
import javax.inject.{Inject, Named}
import javax.servlet._
import javax.servlet.http.HttpServletResponse._
import javax.servlet.http.{HttpServletRequest, HttpServletResponse}
import javax.ws.rs.core.HttpHeaders.RETRY_AFTER
import org.apache.http.client.methods._
import org.apache.http.util.EntityUtils
import org.openrepose.commons.utils.http.CommonHttpHeader.{AUTH_TOKEN, TRACE_GUID}
import org.openrepose.commons.utils.http.OpenStackServiceHeader.{CONTACT_ID, ROLES, TENANT_ID, TENANT_ROLES_MAP}
import org.openrepose.commons.utils.http.normal.ExtendedStatusCodes.SC_TOO_MANY_REQUESTS
import org.openrepose.commons.utils.json.JsonHeaderHelper.{anyToJsonHeader, jsonHeaderToValue}
import org.openrepose.commons.utils.servlet.http.ResponseMode.{MUTABLE, PASSTHROUGH}
import org.openrepose.commons.utils.servlet.http.{HttpServletRequestWrapper, HttpServletResponseWrapper}
import org.openrepose.commons.utils.string.RegexStringOperators
import org.openrepose.core.filter.AbstractConfiguredFilter
import org.openrepose.core.services.config.ConfigurationService
import org.openrepose.core.services.datastore.DatastoreService
import org.openrepose.core.services.httpclient.{CachingHttpClientContext, HttpClientService, HttpClientServiceClient}
import org.openrepose.filters.valkyrieauthorization.config.DeviceIdMismatchAction.{FAIL, KEEP, REMOVE}
import org.openrepose.filters.valkyrieauthorization.config._
import play.api.libs.json._

import scala.Function.tupled
import scala.annotation.tailrec
import scala.collection.JavaConverters._
import scala.io.Source
import scala.language.postfixOps
import scala.util.{Failure, Success, Try}

@Named
class ValkyrieAuthorizationFilter @Inject()(configurationService: ConfigurationService, httpClientService: HttpClientService, datastoreService: DatastoreService)
  extends AbstractConfiguredFilter[ValkyrieAuthorizationConfig](configurationService)
    with HttpDelegationManager
    with RegexStringOperators
    with StrictLogging {

  import org.openrepose.filters.valkyrieauthorization.ValkyrieAuthorizationFilter._

  override val DEFAULT_CONFIG = "valkyrie-authorization.cfg.xml"
  override val SCHEMA_LOCATION = "/META-INF/schema/config/valkyrie-authorization.xsd"

  val datastore = datastoreService.getDefaultDatastore

  var httpClient: HttpClientServiceClient = _

  override def doWork(servletRequest: HttpServletRequest, servletResponse: HttpServletResponse, filterChain: FilterChain): Unit = {
    val httpRequest = new HttpServletRequestWrapper(servletRequest)
    val httpResponse = new HttpServletResponseWrapper(servletResponse, PASSTHROUGH, MUTABLE)

    def nullOrWhitespace(str: Option[String]): Option[String] = str.map(_.trim).filter(_.nonEmpty)

    val requestedTenantId = nullOrWhitespace(httpRequest.getPreferredSplittableHeaders(TENANT_ID).asScala.headOption)
    val requestedDeviceId = nullOrWhitespace(Option(httpRequest.getHeader(DeviceId)))
    val requestedContactId = nullOrWhitespace(Option(httpRequest.getHeader(CONTACT_ID)))
    val tracingHeader = nullOrWhitespace(Option(httpRequest.getHeader(TRACE_GUID)))
    val urlPath: String = new URL(httpRequest.getRequestURL.toString).getPath
    val matchingResources: Seq[Resource] = Option(configuration.getCollectionResources)
      .map(_.getResource.asScala.filter(resource => {
        val pathRegex = resource.getPathRegex
        (pathRegex.getValue =~ urlPath) && {
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
        case (Some(tenant), _) if "(hybrid:.*)".r.findFirstIn(tenant).isEmpty =>
          if (configuration.isPassNonDedicatedTenant) ResponseResult(SC_OK, "Pass non-dedicated tenant")
          else ResponseResult(SC_FORBIDDEN, "Not Authorized")
        case (_, None) => ResponseResult(SC_UNAUTHORIZED, "No contact ID specified")
        case (Some(tenant), Some(contact)) => UserInfo(tenant.substring(tenant.indexOf(":") + 1), contact)
      }
    }

    def getPermissions(headerResult: ValkyrieResult): ValkyrieResult = {
      def parsePermissions(input: String): Try[UserPermissions] = {

        @tailrec
        def parseJson(permissionName: List[String], deviceToPermissions: DeviceToPermissions, values: List[JsValue]): UserPermissions = {
          if (values.isEmpty) {
            UserPermissions(permissionName.toVector, deviceToPermissions)
          } else {
            val currentPermission: JsValue = values.head
            (currentPermission \ "item_type_name").as[String] match {
              case "accounts" =>
                parseJson((currentPermission \ "permission_name").as[String] +: permissionName, deviceToPermissions, values.tail)
              case "devices" =>
                val deviceId = (currentPermission \ "item_id").as[Int]
                val devicePermissionName = (currentPermission \ "permission_name").as[String]
                parseJson(
                  permissionName,
                  deviceToPermissions + (deviceId -> (deviceToPermissions.getOrElse(deviceId, Set.empty) + devicePermissionName)),
                  values.tail)
              case _ => parseJson(permissionName, deviceToPermissions, values.tail)
            }
          }
        }

        try {
          val json = Json.parse(input)
          val permissions: List[JsValue] = (json \ "contact_permissions").as[List[JsValue]]
          Success(parseJson(List.empty, Map.empty, permissions))
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
            datastoreValue(tenant, contact, "any", configuration.getValkyrieServer, Option(httpRequest.getHeader(AUTH_TOKEN)), _.asInstanceOf[UserPermissions], parsePermissions, tracingHeader)
          } else {
            ResponseResult(SC_OK)
          }
        case _ => headerResult
      }
    }

    def getInventory(userPermissions: ValkyrieResult, checkHeader: ValkyrieResult): ValkyrieResult = {
      def parseInventory(input: String): Try[DevicePermissions] = {

        @tailrec
        def parseJson(deviceToPermissions: DeviceToPermissions, values: List[JsValue]): DevicePermissions = {
          if (values.isEmpty) {
            DevicePermissions(deviceToPermissions)
          } else {
            val currentItem: JsValue = values.head
            (currentItem \ "id").as[Int] match {
              case id if id > 0 =>
                parseJson(deviceToPermissions + (id -> (deviceToPermissions.getOrElse(id, Set.empty) + AccountAdmin)), values.tail)
              case _ => parseJson(deviceToPermissions, values.tail)
            }
          }
        }

        try {
          val json = Json.parse(input)
          val inventory: List[JsValue] = (json \ "inventory").as[List[JsValue]]
          Success(parseJson(Map.empty, inventory))
        } catch {
          case e: Exception =>
            logger.error(s"Invalid Json response from Valkyrie: $input", e)
            Failure(new Exception("Invalid Json response from Valkyrie", e))
        }
      }

      userPermissions match {
        case UserPermissions(deviceRoles, devicePermissions) =>
          if (!configuration.isEnableBypassAccountAdmin && deviceRoles.contains(AccountAdmin)) {
            val inventoryResult = checkHeader match {
              case UserInfo(tenant, contact) =>
                datastoreValue(tenant, contact, AccountAdmin, configuration.getValkyrieServer, Option(httpRequest.getHeader(AUTH_TOKEN)), _.asInstanceOf[DevicePermissions], parseInventory, tracingHeader)
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
      def authorizedFor(method: String)(permission: String): Boolean = {
        permission == "view_product" && Set("GET", "HEAD").contains(method) ||
          permission == "edit_product" ||
          permission == "admin_product" ||
          permission == AccountAdmin
      }

      def authorize(deviceId: String, permissions: UserPermissions, method: String): ValkyrieResult = {
        val authorizingPermissions = permissions.devicePermissions.getOrElse(deviceId.toInt, Set.empty)
          .filter(authorizedFor(method))

        if (authorizingPermissions.nonEmpty) {
          permissions.copy(roles = permissions.roles ++ authorizingPermissions)
        } else if (permissions.roles.contains(AccountAdmin) && configuration.isEnableBypassAccountAdmin) {
          permissions
        } else if (permissions.roles.contains(UpgradeAccount) && configuration.isEnableUpgradeAccountPermissions && method == "DELETE") {
          permissions
        } else {
          ResponseResult(SC_FORBIDDEN, "Not Authorized")
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
          try {
            val a = Option(httpRequest.getHeader(TENANT_ROLES_MAP))
            val b = a.map(jsonHeaderToValue(_).as[Map[String, Set[String]]])
            val rolesMap = b.getOrElse(Map.empty)
            val currentRoles = rolesMap.getOrElse(requestedTenantId.get, Set.empty)
            httpRequest.replaceHeader(TENANT_ROLES_MAP, anyToJsonHeader(rolesMap.updated(requestedTenantId.get, currentRoles ++ roles)))
            roles.foreach(httpRequest.addHeader(ROLES, _))
            ResponseResult(SC_OK)
          } catch {
            case e@(_: IllegalArgumentException | _: JsonParseException) =>
              logger.error("A problem occurred while trying to parse the tenant to role map.", e)
              ResponseResult(SC_INTERNAL_SERVER_ERROR, e.getMessage)
          }
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
    val reqAuthRoles = httpRequest.getHeaders(ROLES).asScala.toSeq
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
                httpResponse.sendError(SC_INTERNAL_SERVER_ERROR, rce.getMessage)
            }
          }
        case ResponseResult(code, message, retryTime) if Option(configuration.getDelegating).isDefined =>
          buildDelegationHeaders(code, "valkyrie-authorization", message, configuration.getDelegating.getQuality).foreach { case (key, values) =>
            values.foreach { value => httpRequest.addHeader(key, value) }
          }
          filterChain.doFilter(httpRequest, httpResponse)
          retryTime.foreach(httpResponse.addHeader(RETRY_AFTER, _))
        case ResponseResult(code, message, retryTime) =>
          retryTime.foreach(httpResponse.addHeader(RETRY_AFTER, _))
          httpResponse.sendError(code, message)
      }
    }

    httpResponse.commitToResponse()
  }

  def datastoreValue(transformedTenant: String,
                     contactId: String,
                     callType: String,
                     valkyrieServer: ValkyrieServer,
                     authToken: Option[String],
                     datastoreTransform: java.io.Serializable => ValkyrieResult,
                     responseParser: String => Try[java.io.Serializable],
                     tracingHeader: Option[String] = None): ValkyrieResult = {
    def tryValkyrieCall(): Try[CloseableHttpResponse] = {
      val requestTracingHeader = tracingHeader.map(guid => Map(TRACE_GUID -> guid)).getOrElse(Map())
      val uri = if (callType.equals(AccountAdmin)) {
        s"/account/$transformedTenant/inventory"
      } else {
        s"/account/$transformedTenant/permissions/contacts/$callType/by_contact/$contactId/effective"
      }
      (Option(valkyrieServer.getUsername), Option(valkyrieServer.getPassword)) match {
        case (Some(username), Some(password)) =>
          val requestHeaders = Map("X-Auth-User" -> username, "X-Auth-Token" -> password) ++ requestTracingHeader
          val requestBuilder = RequestBuilder.get(valkyrieServer.getUri + uri)
          requestHeaders.foreach(tupled(requestBuilder.addHeader))
          val request = requestBuilder.build()

          val cacheContext = CachingHttpClientContext.create()
            .setCacheKey(cacheKey(callType, transformedTenant, contactId))

          Try(httpClient.execute(request, cacheContext))
        case _ =>
          val requestHeaders = authToken.map(token => Map(AUTH_TOKEN -> token)).getOrElse(Map.empty) ++ requestTracingHeader
          val requestBuilder = RequestBuilder.get(valkyrieServer.getUri + uri)
          requestHeaders.foreach(tupled(requestBuilder.addHeader))
          val request = requestBuilder.build()

          val cacheContext = CachingHttpClientContext.create()
            .setCacheKey(cacheKey(callType, transformedTenant, contactId))

          Try(httpClient.execute(request, cacheContext))
      }
    }

    def valkyrieAuthorize(): ValkyrieResult = {
      tryValkyrieCall() match {
        case Success(response) =>
          val responseBody = EntityUtils.toString(response.getEntity)
          if (response.getStatusLine.getStatusCode == SC_OK) {
            responseParser(responseBody) match {
              case Success(values) =>
                datastore.put(cacheKey(callType, transformedTenant, contactId), values, configuration.getCacheTimeoutMillis, TimeUnit.MILLISECONDS)
                datastoreTransform(values)
              case Failure(x) => ResponseResult(SC_BAD_GATEWAY, x.getMessage) //JSON Parsing failure
            }
          } else {
            val retryTime = response.getHeaders(RETRY_AFTER).map(_.getValue).headOption
            (Option(configuration.getValkyrieServer.getUsername), Option(configuration.getValkyrieServer.getPassword)) match {
              //admin creds
              case (Some(_), Some(_)) =>
                response.getStatusLine.getStatusCode match {
                  case SC_BAD_REQUEST => ResponseResult(SC_INTERNAL_SERVER_ERROR, "Valkyrie rejected the request for being bad")
                  case SC_UNAUTHORIZED => ResponseResult(SC_INTERNAL_SERVER_ERROR, "Valkyrie said the credentials weren't authorized")
                  case SC_FORBIDDEN => ResponseResult(SC_INTERNAL_SERVER_ERROR, "Valkyrie said the credentials were forbidden")
                  case SC_INTERNAL_SERVER_ERROR => ResponseResult(SC_BAD_GATEWAY, "Valkyrie failed for an unspecified reason")
                  case SC_REQUEST_ENTITY_TOO_LARGE | SC_TOO_MANY_REQUESTS | SC_SERVICE_UNAVAILABLE => ResponseResult(SC_SERVICE_UNAVAILABLE, "Valkyrie rate limited the request", retryTime)
                  case statusCode => ResponseResult(SC_BAD_GATEWAY, s"Valkyrie returned a $statusCode")
                }
              //user token
              case _ =>
                response.getStatusLine.getStatusCode match {
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
    CachePrefix + typeOfCall + transformedTenant + contactId
  }

  def cullResponse(response: HttpServletResponseWrapper, potentialUserPermissions: ValkyrieResult,
                   matchingResources: Seq[Resource]): Unit = {

    def cullJsonArray(jsonDoc: DocumentContext, pathTriplet: PathTriplet, devicePermissions: DeviceToPermissions): Unit = {
      val collectionPath = pathTriplet.getPathToCollection
      val devicePath = pathTriplet.getPathToDeviceId

      def parseDeviceId(fieldValue: String): Try[String] = {
        Try {
          val matcher = devicePath.getRegex.getValue ==~ fieldValue
          if (matcher.matches()) {
            matcher.group(devicePath.getRegex.getCaptureGroup)
          } else {
            throw NonMatchingRegexException(s"Regex: ${devicePath.getRegex.getValue} did not match $fieldValue")
          }
        } recoverWith {
          case npe: NullPointerException => Failure(InvalidJsonTypeException(s"Invalid JSON type in: ${devicePath.getPath}", npe))
          case pse: PatternSyntaxException => Failure(MalformedRegexException("Unable to parse regex for device id", pse))
          case ioobe: IndexOutOfBoundsException => Failure(InvalidCaptureGroupException("Bad capture group specified", ioobe))
        }
      }

      def processDeviceId(deviceIdValue: String): Boolean = {
        parseDeviceId(deviceIdValue)
          .map { deviceId =>
            devicePermissions.keySet.contains(deviceId.toInt)
          } recover {
          case e@(_: InvalidJsonTypeException | _: NonMatchingRegexException) =>
            configuration.getCollectionResources.getDeviceIdMismatchAction match {
              case KEEP => true
              case REMOVE => false
              case FAIL => throw e
            }
        } get
      }

      Try(jsonDoc.read[util.List[String]](
        // Full path from root ($); all partial/relative paths are expanded/merged.
        s"$collectionPath[*]${devicePath.getPath.replaceFirst("^\\$", "")}"
      )) map(_.asScala.map(processDeviceId)) match {
        case Success(keepRemoveSeq: Seq[Boolean]) =>
          if (keepRemoveSeq.isEmpty) {
            throw InvalidJsonPathException(s"Invalid path specified for device id: ${devicePath.getPath}")
          }

          // Remove the items from the JSON Document.
          keepRemoveSeq
            .zipWithIndex
            .filter { case (keepRemove, _) => !keepRemove }
            // Go from back to front so the indices don't change.
            .reverse.foreach {
            case (_, idx) =>
              jsonDoc.delete(s"$collectionPath[$idx]")
          }
          // Check to see if the Item Count Path exists.
          val pathToItemCount = pathTriplet.getPathToItemCount
          try {
            jsonDoc.read(pathToItemCount)
          } catch {
            case pnfe: PathNotFoundException =>
              throw InvalidJsonPathException(s"Invalid path specified for count: $pathToItemCount", pnfe)
          }
          // Update the count.
          jsonDoc.set(pathToItemCount, keepRemoveSeq.count(keepRemove => keepRemove))
        case Failure(e) =>
          throw InvalidJsonPathException(s"Invalid path specified for collection: $collectionPath", e)
      }
    }

    potentialUserPermissions match {
      case UserPermissions(roles, devicePermissions) =>
        if (!configuration.isEnableBypassAccountAdmin || !roles.contains(AccountAdmin)) {
          if (matchingResources.nonEmpty) {
            val input: String = Source.fromInputStream(response.getOutputStreamAsInputStream, response.getCharacterEncoding).getLines() mkString ""
            val jsonDoc: DocumentContext = Try(JsonPath.parse(input))
              .recover({ case jpe: InvalidJsonException => throw UnexpectedJsonException("Response contained improper json.", jpe) })
              .get
            matchingResources.foreach { resource =>
              resource.getCollection.asScala.foreach { collection =>
                cullJsonArray(jsonDoc, collection.getJson, devicePermissions)
              }
            }
            // Replace the existing output with the modified output
            response.setOutput(new ByteArrayInputStream(jsonDoc.jsonString().getBytes(response.getCharacterEncoding)))
          }
        }
      case _ =>
    }
  }

  override def doConfigurationUpdated(newConfig: ValkyrieAuthorizationConfig): ValkyrieAuthorizationConfig = {
    httpClient = httpClientService.getClient(newConfig.getConnectionPoolId)

    newConfig
  }
}

object ValkyrieAuthorizationFilter {
  final val DeviceId = "X-Device-Id"
  final val AccountAdmin = "account_admin"
  final val UpgradeAccount = "upgrade_account"
  final val CachePrefix = "VALKYRIE-FILTER"

  type DeviceToPermissions = Map[Int, Set[String]]

  sealed trait ValkyrieResult
  case class UserInfo(tenantId: String, contactId: String) extends ValkyrieResult
  case class UserPermissions(roles: Vector[String], devicePermissions: DeviceToPermissions) extends ValkyrieResult
  case class DevicePermissions(devicePermissions: DeviceToPermissions) extends ValkyrieResult
  case class ResponseResult(statusCode: Int, message: String = "", retryTime: Option[String] = None) extends ValkyrieResult
}
