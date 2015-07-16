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
package org.openrepose.filters.keystonev2

import java.net.URL
import javax.inject.{Inject, Named}
import javax.servlet._
import javax.servlet.http.HttpServletResponse._
import javax.servlet.http.{HttpServletRequest, HttpServletResponse}

import com.rackspace.httpdelegation.HttpDelegationManager
import com.typesafe.scalalogging.slf4j.LazyLogging
import org.apache.commons.codec.binary.Base64
import org.apache.http.HttpHeaders
import org.openrepose.commons.config.manager.UpdateListener
import org.openrepose.commons.utils.http._
import org.openrepose.commons.utils.servlet.http.MutableHttpServletRequest
import org.openrepose.core.filter.FilterConfigHelper
import org.openrepose.core.services.config.ConfigurationService
import org.openrepose.core.services.datastore.{Datastore, DatastoreService}
import org.openrepose.core.services.serviceclient.akka.AkkaServiceClient
import org.openrepose.core.systemmodel.SystemModel
import org.openrepose.filters.keystonev2.RequestHandler._
import org.openrepose.filters.keystonev2.config._

import scala.collection.mutable
import scala.util.{Failure, Success, Try}

@Named
class KeystoneV2Filter @Inject()(configurationService: ConfigurationService,
                                 akkaServiceClient: AkkaServiceClient,
                                 datastoreService: DatastoreService)
  extends Filter
  with HttpDelegationManager
  with LazyLogging {

  import KeystoneV2Filter._

  private final val SYSTEM_MODEL_CONFIG = "system-model.cfg.xml"
  private final val DEFAULT_CONFIG = "keystone-v2.cfg.xml"
  private final val X_AUTH_PROXY = "Proxy"

  private var configurationFile: String = DEFAULT_CONFIG
  private var sendTraceHeader = true

  //Which happens to be the local datastore
  private val datastore: Datastore = datastoreService.getDefaultDatastore

  implicit val autoHeaderToString: HeaderConstant => String = hc => hc.toString

  var keystoneV2Config: KeystoneV2Config = _

  override def init(filterConfig: FilterConfig): Unit = {
    configurationFile = new FilterConfigHelper(filterConfig).getFilterConfig(DEFAULT_CONFIG)
    logger.info(s"Initializing Keystone V2 Filter using config $configurationFile")
    val xsdURL: URL = getClass.getResource("/META-INF/schema/config/keystone-v2.xsd")
    configurationService.subscribeTo(
      SYSTEM_MODEL_CONFIG,
      getClass.getResource("/META-INF/schema/system-model/system-model.xsd"),
      SystemModelConfigListener,
      classOf[SystemModel]
    )
    configurationService.subscribeTo(
      filterConfig.getFilterName,
      configurationFile,
      xsdURL,
      KeystoneV2ConfigListener,
      classOf[KeystoneV2Config]
    )
  }

  override def destroy(): Unit = {
    configurationService.unsubscribeFrom(configurationFile, KeystoneV2ConfigListener)
    configurationService.unsubscribeFrom(SYSTEM_MODEL_CONFIG, SystemModelConfigListener)
  }

  override def doFilter(servletRequest: ServletRequest, servletResponse: ServletResponse, chain: FilterChain): Unit = {
    if (!isInitialized) {
      logger.error("Keystone v2 filter has not yet initialized...")
      servletResponse.asInstanceOf[HttpServletResponse].sendError(SC_INTERNAL_SERVER_ERROR)
    } else {
      logger.trace("Keystone v2 filter processing request...")

      val config = keystoneV2Config
      val request = MutableHttpServletRequest.wrap(servletRequest.asInstanceOf[HttpServletRequest])
      // Not using the mutable wrapper because it doesn't work properly at the moment, and we don't need to modify the response from further down the chain
      val response = servletResponse.asInstanceOf[HttpServletResponse]
      val traceId = Option(request.getHeader(CommonHttpHeader.TRACE_GUID.toString)).filter(_ => sendTraceHeader)
      val requestHandler = new RequestHandler(config, akkaServiceClient, datastore, traceId)

      //Check our whitelist
      val whiteListURIs: Option[List[String]] =
        for {
          jaxbIntermediary <- Option(config.getWhiteList)
          regexList <- Option(jaxbIntermediary.getUriRegex)
        } yield {
          import scala.collection.JavaConversions._
          regexList.toList
        }

      val whiteListMatch: Boolean = whiteListURIs exists { uriList =>
        uriList exists { pattern =>
          logger.debug(s"checking ${request.getRequestURI} against $pattern")
          request.getRequestURI.matches(pattern)
        }
      }

      val authTokenValue = Option(request.getHeader(CommonHttpHeader.AUTH_TOKEN))

      // Initialize the headers map here so it is accessible when handling the result
      var addtlReqHdrs: mutable.Map[String, String] = mutable.Map.empty[String, String]
      var addtlRespHdrs: mutable.Map[String, String] = mutable.Map.empty[String, String]

      //Get the authenticating token!
      val result: KeystoneV2Result = if (whiteListMatch) {
        Pass
      } else {
        authTokenValue map { authToken =>
          //This block of code tries to get the token from the datastore, and provides it from real calls, if it isn't
          val tokenValidationResult: Try[AuthResult] =
            Option(datastore.get(s"$TOKEN_KEY_PREFIX$authToken").asInstanceOf[AuthResult]) map { validationResult =>
              Success(validationResult)
            } getOrElse {
              //flatMap to unbox the Try[Try[TokenValidationResult]] so all Failure's are just packaged along
              requestHandler.getAdminToken flatMap { adminToken =>
                requestHandler.validateToken(adminToken, authToken) recoverWith {
                  //Recover if the exception is an AdminTokenUnauthorizedException
                  //This way we can specify however we want to what we want to do to retry.
                  //Also it only retries ONCE! No loops or anything. Fails gloriously
                  case unauth: AdminTokenUnauthorizedException =>
                    //Clear the cache, call this method again
                    datastore.remove(ADMIN_TOKEN_KEY)
                    requestHandler.getAdminToken match {
                      case Success(newAdminToken) =>
                        requestHandler.validateToken(newAdminToken, authToken)
                      case Failure(x) => Failure(IdentityAdminTokenException("Unable to reacquire admin token", x))
                    }
                }
              }
            }

          tokenValidationResult match {
            case Success(InvalidToken) =>
              Reject(SC_UNAUTHORIZED)
            case Success(validToken: ValidToken) =>
              // Add standard headers from token information
              addtlReqHdrs ++= Map(PowerApiHeader.USER.toString -> validToken.username,
                OpenStackServiceHeader.USER_NAME.toString -> validToken.username,
                OpenStackServiceHeader.USER_ID.toString -> validToken.userId,
                OpenStackServiceHeader.TENANT_NAME.toString -> validToken.tenantName,
                OpenStackServiceHeader.X_EXPIRATION.toString -> validToken.expirationDate)
              validToken.defaultRegion.map(dr => OpenStackServiceHeader.DEFAULT_REGION.toString -> dr).foreach(addtlReqHdrs.+=)
              validToken.contactId.map(ci => OpenStackServiceHeader.CONTACT_ID.toString -> ci).foreach(addtlReqHdrs.+=)
              validToken.impersonatorId.map(iid => OpenStackServiceHeader.IMPERSONATOR_ID.toString -> iid).foreach(addtlReqHdrs.+=)
              validToken.impersonatorName.map(in => OpenStackServiceHeader.IMPERSONATOR_NAME.toString -> in).foreach(addtlReqHdrs.+=)

              // If configured, add roles header
              if (config.getIdentityService.isSetRolesInHeader) {
                addtlReqHdrs += (OpenStackServiceHeader.ROLES.toString -> validToken.roles.mkString(","))
              }

              // Extract the tenant and add the x-authorization header
              val uriTenantOption = requestHandler.extractTenant(request.getRequestURI)
              uriTenantOption match {
                case Some(uriTenant) =>
                  addtlReqHdrs += (OpenStackServiceHeader.EXTENDED_AUTHORIZATION.toString -> s"$X_AUTH_PROXY $uriTenant")
                case None =>
                  addtlReqHdrs += (OpenStackServiceHeader.EXTENDED_AUTHORIZATION.toString -> X_AUTH_PROXY)
              }

              // If configured, authorize the URI tenant against the token
              val authorizedTenant = requestHandler.tenantAuthorization(uriTenantOption, validToken) match {
                case Some(Success(tenantHeaderValues)) =>
                  addtlReqHdrs += (OpenStackServiceHeader.TENANT_ID.toString -> tenantHeaderValues.mkString(","))
                  Pass
                case Some(Failure(e)) =>
                  Reject(SC_UNAUTHORIZED, failure = Some(e))
                case None =>
                  Pass
              }

              // Authorize token endpoints and, if configured, populate the x-catalog header
              val endpoints = authorizedTenant match {
                case Pass =>
                  requestHandler.handleEndpoints(authToken, validToken) match {
                    case Some(Success(endpointsData)) =>
                      //If I'm configured to put the endpoints into a x-catalog do it
                      if (config.getIdentityService.isSetCatalogInHeader) {
                        // todo: don't check this flag twice
                        addtlReqHdrs += (PowerApiHeader.X_CATALOG.toString -> Base64.encodeBase64String(endpointsData.endpointsJson.getBytes))
                        Pass
                      } else {
                        Pass
                      }
                    case Some(Failure(x)) =>
                      //Reject them with 403
                      Reject(SC_FORBIDDEN, failure = Some(x))
                    case None =>
                      //Do more things in here
                      Pass
                  }
                case reject: Reject => reject
              }

              // If configured, populate the x-pp-groups endpoint
              val userGroups = endpoints match {
                case Pass =>
                  requestHandler.getGroups(authToken, validToken) match {
                    case Some(Success(groups)) =>
                      if (groups.isEmpty) {
                        Pass
                      } else {
                        addtlReqHdrs += (PowerApiHeader.GROUPS.toString -> groups.mkString(","))
                        Pass
                      }
                    case Some(Failure(e)) =>
                      logger.error(s"Could not get groups: ${e.getMessage}")
                      Pass
                    case None => Pass
                  }
                case reject: Reject => reject
              }

              // Add the x-identity-status header once all other checks pass
              userGroups match {
                case Pass =>
                  addtlReqHdrs += (OpenStackServiceHeader.IDENTITY_STATUS.toString -> IdentityStatus.Confirmed.toString)
                case _ => ()
              }

              userGroups
            case Failure(x: IdentityAdminTokenException) =>
              Reject(SC_INTERNAL_SERVER_ERROR, failure = Some(x))
            case Failure(x: IdentityCommunicationException) =>
              Reject(SC_BAD_GATEWAY, failure = Some(x))
            case Failure(x) =>
              //TODO: this isn't yet complete, should return other status codes?
              Reject(SC_INTERNAL_SERVER_ERROR, failure = Some(x))
          }
        } getOrElse {
          //Don't have an auth token to validate
          Reject(SC_UNAUTHORIZED, Some("Auth token not found in headers"))
        }
      }

      // Add headers regardless of the result because it's cheap
      addtlReqHdrs.foreach { case (k, v) =>
        request.addHeader(k, v)
      }

      //Handle the result of the filter
      result match {
        case r: Reject =>
          //Transform the result for common failures
          val reject = r match {
            case Reject(_, _, Some(ole: OverLimitException)) =>
              // Handle rate limiting case
              addtlRespHdrs += (HttpHeaders.RETRY_AFTER -> ole.retryAfter)
              Reject(HttpServletResponse.SC_SERVICE_UNAVAILABLE, Some(ole.message), Some(ole))
            case _ =>
              // No transformation necessary
              r
          }

          //Extract the message from the Reject
          val message: Option[String] = reject match {
            case Reject(_, Some(x), _) => Some(x)
            case Reject(code, None, Some(failure)) => Some(failure.getMessage)
            case _ => None
          }

          //Handle delegation if necessary
          Option(config.getDelegating) match {
            case Some(delegating) =>
              logger.debug(s"Delegating with status ${reject.status} caused by: ${message.getOrElse("unspecified")}")
              val delegationHeaders = buildDelegationHeaders(reject.status,
                "keystone-v2",
                message.getOrElse("Failure in the Keystone v2 filter").replace("\n", " "),
                delegating.getQuality)

              request.addHeader(OpenStackServiceHeader.IDENTITY_STATUS.toString, IdentityStatus.Indeterminate.toString)
              delegationHeaders foreach { case (key, values) =>
                values foreach { value =>
                  request.addHeader(key, value)
                }
              }

              chain.doFilter(request, response)

            // todo: handle response, add www-authenticate header and retry-after header
            case None =>
              logger.debug(s"Rejecting with status ${reject.status}")
              addtlRespHdrs += (CommonHttpHeader.WWW_AUTHENTICATE.toString -> s"Keystone uri=${config.getIdentityService.getUri}")
              addtlRespHdrs foreach { case (k, v) =>
                response.addHeader(k, v)
              }
              message match {
                case Some(m) =>
                  logger.debug(s"Rejection message: $m")
                  response.sendError(reject.status, m)
                case None => response.sendError(reject.status)
              }
          }

        case Pass =>
          //If the token validation passed and we're configured to do more things, we can do additional authorizations
          chain.doFilter(request, response)

          logger.trace("Keystone v2 filter processing response...")
          val wwwAuthenticateValue = response.getHeader(CommonHttpHeader.WWW_AUTHENTICATE)

          response.getStatus match {
            case HttpServletResponse.SC_UNAUTHORIZED | HttpServletResponse.SC_FORBIDDEN =>
              Option(config.getDelegating) foreach { delegating =>
                // If in the case that the origin service supports delegated authentication
                // we should then communicate to the client how to authenticate with us
                response.addHeader(CommonHttpHeader.WWW_AUTHENTICATE, s"Keystone uri=${config.getIdentityService.getUri}")
              }
            case HttpServletResponse.SC_NOT_IMPLEMENTED =>
              Option(config.getDelegating) foreach { delegating =>
                logger.error("Keystone v2 filter is configured to delegate, but the origin service does not support delegation")
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR)
              }
            case _ => // nothing to do here
          }
      }
    }
  }

  def isInitialized = SystemModelConfigListener.isInitialized && KeystoneV2ConfigListener.isInitialized

  object SystemModelConfigListener extends UpdateListener[SystemModel] {
    var initialized = false

    override def configurationUpdated(configurationObject: SystemModel): Unit = {
      sendTraceHeader = configurationObject.isTracingHeader
      initialized = true
    }

    override def isInitialized: Boolean = initialized
  }

  object KeystoneV2ConfigListener extends UpdateListener[KeystoneV2Config] {
    var initialized = false

    override def configurationUpdated(configurationObject: KeystoneV2Config): Unit = {
      def fixMyDefaults(stupidConfig: KeystoneV2Config): KeystoneV2Config = {
        // LOLJAXB  	(╯°□°）╯︵ ┻━┻
        //This relies on the Default Settings plugin and the fluent_api plugin added to the Jaxb code generation plugin
        // I'm sorry
        if (stupidConfig.getCache == null) {
          stupidConfig.withCache(new CacheType().withTimeouts(new CacheTimeoutsType()))
        } else if (stupidConfig.getCache.getTimeouts == null) {
          stupidConfig.getCache.withTimeouts(new CacheTimeoutsType())
          stupidConfig
        } else {
          stupidConfig
        }
      }

      keystoneV2Config = fixMyDefaults(configurationObject)
      initialized = true
    }

    override def isInitialized: Boolean = initialized
  }

}

object KeystoneV2Filter {

  sealed trait KeystoneV2Result

  object Pass extends KeystoneV2Result

  case class Reject(status: Int,
                    message: Option[String] = None,
                    failure: Option[Throwable] = None) extends KeystoneV2Result

}
