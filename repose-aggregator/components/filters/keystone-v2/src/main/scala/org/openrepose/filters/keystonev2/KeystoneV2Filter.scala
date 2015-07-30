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
import java.util.concurrent.TimeUnit
import javax.inject.{Inject, Named}
import javax.servlet._
import javax.servlet.http.HttpServletResponse._
import javax.servlet.http.{HttpServletRequest, HttpServletResponse}

import com.rackspace.httpdelegation.HttpDelegationManager
import com.typesafe.scalalogging.slf4j.LazyLogging
import org.apache.commons.codec.binary.Base64
import org.apache.http.HttpHeaders
import org.apache.http.client.utils.DateUtils
import org.openrepose.commons.config.manager.UpdateListener
import org.openrepose.commons.utils.http._
import org.openrepose.commons.utils.servlet.http.MutableHttpServletRequest
import org.openrepose.core.filter.FilterConfigHelper
import org.openrepose.core.services.config.ConfigurationService
import org.openrepose.core.services.datastore.{Datastore, DatastoreService}
import org.openrepose.core.services.serviceclient.akka.AkkaServiceClient
import org.openrepose.core.systemmodel.SystemModel
import org.openrepose.filters.keystonev2.KeystoneRequestHandler._
import org.openrepose.filters.keystonev2.config._

import scala.collection.JavaConverters._
import scala.language.implicitConversions
import scala.util.{Failure, Random, Success, Try}

@Named
class KeystoneV2Filter @Inject()(configurationService: ConfigurationService,
                                 akkaServiceClient: AkkaServiceClient,
                                 datastoreService: DatastoreService)
  extends Filter
  with HttpDelegationManager
  with LazyLogging {

  import KeystoneV2Filter._

  private var configurationFile: String = DEFAULT_CONFIG
  private var sendTraceHeader = true

  private val datastore: Datastore = datastoreService.getDefaultDatastore //Which happens to be the local datastore

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
    /**
     * STATIC REFERENCE TO CONFIG
     */
    val config = keystoneV2Config

    /**
     * DECLARE COMMON VALUES
     */
    lazy val request = MutableHttpServletRequest.wrap(servletRequest.asInstanceOf[HttpServletRequest])
    // Not using the mutable wrapper because it doesn't work properly at the moment, and
    // we don't need to modify the response from further down the chain
    lazy val response = servletResponse.asInstanceOf[HttpServletResponse]
    lazy val traceId = Option(request.getHeader(CommonHttpHeader.TRACE_GUID.toString)).filter(_ => sendTraceHeader)
    lazy val requestHandler = new KeystoneRequestHandler(config.getIdentityService.getUri, akkaServiceClient, traceId)

    /**
     * BEGIN PROCESSING
     */
    if (!isInitialized) {
      logger.error("Keystone v2 filter has not yet initialized")
      servletResponse.asInstanceOf[HttpServletResponse].sendError(SC_INTERNAL_SERVER_ERROR)
    } else {
      logger.trace("Keystone v2 filter processing request...")

      val keystoneAuthenticateHeader = s"Keystone uri=${config.getIdentityService.getUri}"

      val filterResult =
        if (isWhitelisted(request.getRequestURI)) {
          Pass
        } else {
          val processingResult =
            getAuthToken flatMap { authToken =>
              validateToken(getAdminToken, authToken) flatMap { validToken =>
                addTokenHeaders(validToken)
                authorizeTenant(tenantFromUri, validToken) flatMap { _ =>
                  lazy val endpoints = getEndpoints(getAdminToken, authToken, validToken) // Prevents making call if its not needed
                  addCatalogHeader(endpoints) flatMap { _ =>
                    authorizeEndpoints(validToken, endpoints) flatMap { _ =>
                      addGroupsHeader(getGroups(getAdminToken, authToken, validToken))
                    }
                  }
                }
              }
            }

          processingResult match {
            case Success(_) => Pass
            case Failure(e: MissingAuthTokenException) => Reject(SC_UNAUTHORIZED, Some(e.getMessage))
            case Failure(e: InvalidTokenException) => Reject(SC_UNAUTHORIZED, Some(e.getMessage))
            case Failure(e: InvalidTenantException) => Reject(SC_UNAUTHORIZED, Some(e.getMessage))
            case Failure(e: UnparseableTenantException) => Reject(SC_UNAUTHORIZED, Some(e.getMessage))
            case Failure(e: IdentityCommunicationException) => Reject(SC_BAD_GATEWAY, Some(e.getMessage))
            case Failure(e: UnauthorizedEndpointException) => Reject(SC_FORBIDDEN, Some(e.getMessage))
            case Failure(e: OverLimitException) =>
              response.addHeader(HttpHeaders.RETRY_AFTER, e.retryAfter)
              Reject(HttpServletResponse.SC_SERVICE_UNAVAILABLE, Some(e.getMessage))
            case Failure(e) => Reject(SC_INTERNAL_SERVER_ERROR, Some(e.getMessage))
          }
        }

      filterResult match {
        case Pass =>
          logger.trace("Processing completed, passing to next filter or service")
          addIdentityStatusHeader(confirmed = true)
          chain.doFilter(request, response)
        case Reject(statusCode, message) =>
          Option(config.getDelegating) match {
            case Some(delegating) =>
              logger.debug(s"Delegating with status $statusCode caused by: ${message.getOrElse("unspecified")}")

              val delegationHeaders = buildDelegationHeaders(statusCode,
                "keystone-v2",
                message.getOrElse("Failure in the Keystone v2 filter").replace("\n", " "),
                delegating.getQuality)

              addIdentityStatusHeader(confirmed = false)
              delegationHeaders foreach { case (key, values) =>
                values foreach { value =>
                  request.addHeader(key, value)
                }
              }

              chain.doFilter(request, response)

              logger.trace(s"Processing response with status code: $statusCode")

              val wwwAuthenticateHeader = response.getHeader(CommonHttpHeader.WWW_AUTHENTICATE)

              response.getStatus match {
                case HttpServletResponse.SC_UNAUTHORIZED | HttpServletResponse.SC_FORBIDDEN =>
                  if (DELEGATED.equalsIgnoreCase(wwwAuthenticateHeader.trim)) {
                    logger.debug("The origin service could not authenticate the delegated request")
                    response.setHeader(CommonHttpHeader.WWW_AUTHENTICATE, keystoneAuthenticateHeader)
                  }
                case HttpServletResponse.SC_NOT_IMPLEMENTED =>
                  if (DELEGATED.equalsIgnoreCase(wwwAuthenticateHeader.trim)) {
                    logger.error("Configured to delegate, but the origin service does not support delegation")
                    response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR)
                  }
                case _ => ()
              }
            case None =>
              logger.debug(s"Rejecting with status $statusCode")

              if (statusCode == 401) {
                response.addHeader(CommonHttpHeader.WWW_AUTHENTICATE.toString, keystoneAuthenticateHeader)
              }

              message match {
                case Some(m) =>
                  logger.debug(s"Rejection message: $m")
                  response.sendError(statusCode, m)
                case None => response.sendError(statusCode)
              }
          }
      }
    }

    /**
     * DEFINING FUNCTIONS IN SCOPE
     */
    lazy val tenantFromUri: Option[String] = {
      Option(config.getTenantHandling.getValidateTenant) flatMap { validateTenantConfig =>
        Option(validateTenantConfig.getUriExtractionRegex) flatMap { uriExtractionRegex =>
          val regex = uriExtractionRegex.r
          request.getRequestURI match {
            case regex(tenantId, _*) => Some(tenantId)
            case _ => None
          }
        }
      }
    }

    def isWhitelisted(requestUri: String): Boolean = {
      logger.trace("Comparing request URI to whitelisted URIs")

      val whiteListUris: List[String] = config.getWhiteList.getUriRegex.asScala.toList

      whiteListUris exists { pattern =>
        logger.debug(s"checking $requestUri against $pattern")
        requestUri.matches(pattern)
      }
    }

    def getAuthToken: Try[String] = {
      logger.trace("Getting the x-auth-token header value")

      Option(request.getHeader(CommonHttpHeader.AUTH_TOKEN)) match {
        case Some(token) => Success(token)
        case None => Failure(MissingAuthTokenException("X-Auth-Token header not found"))
      }
    }

    def getAdminToken(force: Boolean): Try[String] = {
      logger.trace("Getting an admin token with the configured credentials")

      // If force is true, clear the cache and acquire a new token
      if (force) datastore.remove(ADMIN_TOKEN_KEY)

      Option(datastore.get(ADMIN_TOKEN_KEY).asInstanceOf[String]) match {
        case Some(cachedAdminToken) => Success(cachedAdminToken)
        case None =>
          requestHandler.getAdminToken(config.getIdentityService.getUsername, config.getIdentityService.getPassword) match {
            case Success(adminToken) =>
              datastore.put(ADMIN_TOKEN_KEY, adminToken)
              Success(adminToken)
            case f: Failure[_] => f
          }
      }
    }

    def validateToken(getAdminToken: Boolean => Try[String], authToken: String): Try[ValidToken] = {
      logger.trace(s"Validating token: $authToken")

      Option(datastore.get(s"$TOKEN_KEY_PREFIX$authToken").asInstanceOf[ValidToken]) map { validationResult =>
        Success(validationResult)
      } getOrElse {
        getAdminToken(false) flatMap { validatingToken =>
          requestHandler.validateToken(validatingToken, authToken) recoverWith {
            case _: AdminTokenUnauthorizedException =>
              // Force acquiring of the admin token, and call the validation function again (retry once)
              getAdminToken(true) match {
                case Success(newValidatingToken) => requestHandler.validateToken(newValidatingToken, authToken)
                case Failure(x) => Failure(IdentityAdminTokenException("Unable to reacquire admin token", x))
              }
          } cacheOnSuccess { validToken =>
            val cacheSettings = config.getCache
            val timeToLive = getTtl(cacheSettings.getTimeouts.getToken,
              cacheSettings.getTimeouts.getVariability,
              Some(validToken))
            datastore.put(s"$TOKEN_KEY_PREFIX$authToken", validToken, timeToLive, TimeUnit.SECONDS)
          }
        }
      }
    }

    def authorizeTenant(expectedTenant: Option[String], validToken: ValidToken): Try[Unit.type] = {
      Option(config.getTenantHandling.getValidateTenant) map { validateTenant =>
        Option(validateTenant.getBypassValidationRoles) filter {
          _.getRole.asScala.intersect(validToken.roles).nonEmpty
        } map { _ =>
          Success(Unit)
        } getOrElse {
          logger.trace("Validating tenant")

          expectedTenant map { reqTenant =>
            val tokenTenants = Set(validToken.defaultTenantId) ++ validToken.tenantIds
            tokenTenants.find(reqTenant.equals)
              .map(_ => Success(Unit))
              .getOrElse(Failure(InvalidTenantException("Tenant from URI does not match any of the tenants associated with the provided token")))
          } getOrElse {
            Failure(UnparseableTenantException("Could not parse tenant from the URI"))
          }
        }
      } getOrElse Success(Unit)
    }

    def getEndpoints(getAdminToken: Boolean => Try[String], authToken: String, validToken: ValidToken): Try[EndpointsData] = {
      logger.trace(s"Getting endpoints for: $authToken")

      // todo: extract the "make call with admin token and retry" logic since that pattern is used elsewhere
      Option(datastore.get(s"$ENDPOINTS_KEY_PREFIX$authToken").asInstanceOf[EndpointsData]) match {
        case Some(endpointsData) => Success(endpointsData)
        case None =>
          getAdminToken(false) flatMap { adminToken =>
            requestHandler.getEndpointsForToken(adminToken, authToken) recoverWith {
              case _: AdminTokenUnauthorizedException =>
                // Force acquiring of the admin token, and call the endpoints function again (retry once)
                getAdminToken(true) match {
                  case Success(newAdminToken) => requestHandler.getEndpointsForToken(newAdminToken, authToken)
                  case Failure(x) => Failure(IdentityAdminTokenException("Unable to reacquire admin token", x))
                }
            } cacheOnSuccess { endpointsJson =>
              val cacheSettings = config.getCache
              val timeToLive = getTtl(cacheSettings.getTimeouts.getEndpoints,
                cacheSettings.getTimeouts.getVariability)
              datastore.put(s"$ENDPOINTS_KEY_PREFIX$authToken", endpointsJson, timeToLive, TimeUnit.SECONDS)
            }
          }
      }
    }

    def authorizeEndpoints(validToken: ValidToken, maybeEndpoints: => Try[EndpointsData]): Try[Unit.type] = {
      Option(config.getRequireServiceEndpoint) match {
        case Some(configuredEndpoint) =>
          logger.trace(s"Authorizing endpoints")

          maybeEndpoints flatMap { endpoints =>
            lazy val requiredEndpoint =
              Endpoint(
                publicURL = configuredEndpoint.getPublicUrl,
                name = Option(configuredEndpoint.getName),
                endpointType = Option(configuredEndpoint.getType),
                region = Option(configuredEndpoint.getRegion)
              )

            val bypassRoles = Option(configuredEndpoint.getBypassValidationRoles)
              .map(_.getRole.asScala)
              .getOrElse(List.empty)

            if (bypassRoles.intersect(validToken.roles).nonEmpty || endpoints.vector.exists(_.meetsRequirement(requiredEndpoint))) Success(Unit)
            else Failure(UnauthorizedEndpointException("User did not have the required endpoint"))
          }
        case None => Success(Unit)
      }
    }

    def getGroups(getAdminToken: Boolean => Try[String], authToken: String, validToken: ValidToken): Try[Vector[String]] = {
      logger.trace(s"Getting groups for: $authToken")

      Option(datastore.get(s"$GROUPS_KEY_PREFIX$authToken").asInstanceOf[Vector[String]]) match {
        case Some(groups) => Success(groups)
        case None =>
          getAdminToken(false) flatMap { adminToken =>
            requestHandler.getGroups(adminToken, authToken) recoverWith {
              case _: AdminTokenUnauthorizedException =>
                // Force acquiring of the admin token, and call the endpoints function again (retry once)
                getAdminToken(true) match {
                  case Success(newAdminToken) => requestHandler.getGroups(newAdminToken, authToken)
                  case Failure(x) => Failure(IdentityAdminTokenException("Unable to reacquire admin token", x))
                }
            } cacheOnSuccess { groups =>
              val cacheSettings = config.getCache
              val timeToLive = getTtl(cacheSettings.getTimeouts.getGroup, cacheSettings.getTimeouts.getVariability)
              datastore.put(s"$GROUPS_KEY_PREFIX$authToken", groups, timeToLive, TimeUnit.SECONDS)
            }
          }
      }
    }

    def buildTenantHeader(defaultTenant: String,
                          roleTenants: Seq[String],
                          uriTenant: Option[String]): Vector[String] = {
      val sendAllTenants = config.getTenantHandling.isSendAllTenantIds
      val sendTenantIdQuality = Option(config.getTenantHandling.getSendTenantIdQuality)
      val sendQuality = sendTenantIdQuality.isDefined
      val defaultTenantQuality = sendTenantIdQuality.map(_.getDefaultTenantQuality).getOrElse(0.0)
      val uriTenantQuality = sendTenantIdQuality.map(_.getUriTenantQuality).getOrElse(0.0)
      val rolesTenantQuality = sendTenantIdQuality.map(_.getRolesTenantQuality).getOrElse(0.0)

      var preferredTenant = defaultTenant
      var preferredTenantQuality = defaultTenantQuality
      uriTenant foreach { tenant =>
        preferredTenant = tenant

        preferredTenantQuality = if (defaultTenant.equals(tenant)) {
          math.max(defaultTenantQuality, uriTenantQuality)
        } else {
          uriTenantQuality
        }
      }

      if (sendAllTenants && sendQuality) {
        val priorityTenants = uriTenant match {
          case Some(tenant) => Vector(s"$defaultTenant;q=$defaultTenantQuality", s"$tenant;q=$uriTenantQuality")
          case None => Vector(s"$defaultTenant;q=$defaultTenantQuality")
        }
        priorityTenants ++ roleTenants.map(tid => s"$tid;q=$rolesTenantQuality")
      } else if (sendAllTenants && !sendQuality) {
        Vector(defaultTenant) ++ roleTenants
      } else if (!sendAllTenants && sendQuality) {
        Vector(s"$preferredTenant;q=$preferredTenantQuality")
      } else {
        Vector(preferredTenant)
      }
    }

    def addTokenHeaders(token: ValidToken): Unit = {
      // Add standard headers
      request.addHeader(PowerApiHeader.USER.toString, token.username)
      request.addHeader(OpenStackServiceHeader.USER_NAME.toString, token.username)
      request.addHeader(OpenStackServiceHeader.USER_ID.toString, token.userId)
      request.addHeader(OpenStackServiceHeader.TENANT_NAME.toString, token.tenantName)
      request.addHeader(OpenStackServiceHeader.X_EXPIRATION.toString, token.expirationDate)
      token.defaultRegion.foreach(request.addHeader(OpenStackServiceHeader.DEFAULT_REGION.toString, _))
      token.contactId.foreach(request.addHeader(OpenStackServiceHeader.CONTACT_ID.toString, _))
      token.impersonatorId.foreach(request.addHeader(OpenStackServiceHeader.IMPERSONATOR_ID.toString, _))
      token.impersonatorName.foreach(request.addHeader(OpenStackServiceHeader.IMPERSONATOR_NAME.toString, _))

      // Construct and add the tenant id header
      val tenantsToPass = buildTenantHeader(token.defaultTenantId, token.tenantIds, tenantFromUri)
      request.addHeader(OpenStackServiceHeader.TENANT_ID.toString, tenantsToPass.mkString(","))

      // If configured, add roles header
      if (config.getIdentityService.isSetRolesInHeader) {
        request.addHeader(OpenStackServiceHeader.ROLES.toString, token.roles.mkString(","))
      }

      // If present, add the tenant from the URI as part of the Proxy header, otherwise use the default tenant id
      tenantFromUri match {
        case Some(uriTenant) =>
          request.addHeader(OpenStackServiceHeader.EXTENDED_AUTHORIZATION.toString, s"$X_AUTH_PROXY $uriTenant")
        case None =>
          request.addHeader(OpenStackServiceHeader.EXTENDED_AUTHORIZATION.toString, s"$X_AUTH_PROXY ${token.defaultTenantId}")
      }
    }

    def addCatalogHeader(maybeEndpoints: => Try[EndpointsData]): Try[Unit.type] = {
      if (config.getIdentityService.isSetCatalogInHeader) {
        maybeEndpoints map { endpoints =>
          request.addHeader(PowerApiHeader.X_CATALOG.toString, Base64.encodeBase64String(endpoints.json.getBytes))
          Unit
        }
      } else {
        Success(Unit)
      }
    }

    def addGroupsHeader(maybeGroups: => Try[Vector[String]]): Try[Unit.type] = {
      if (config.getIdentityService.isSetGroupsInHeader) {
        maybeGroups map { groups =>
          if (groups.nonEmpty) {
            request.addHeader(PowerApiHeader.GROUPS, groups.mkString(","))
          }
          Unit
        }
      } else {
        Success(Unit)
      }
    }

    def addIdentityStatusHeader(confirmed: Boolean): Unit = {
      if (Option(config.getDelegating).isDefined) {
        if (confirmed) request.addHeader(OpenStackServiceHeader.IDENTITY_STATUS, IdentityStatus.Confirmed.toString)
        else request.addHeader(OpenStackServiceHeader.IDENTITY_STATUS, IdentityStatus.Indeterminate.toString)
      }
    }
  }

  def isInitialized = SystemModelConfigListener.isInitialized && KeystoneV2ConfigListener.isInitialized

  object SystemModelConfigListener extends UpdateListener[SystemModel] {
    private var initialized = false

    override def configurationUpdated(configurationObject: SystemModel): Unit = {
      sendTraceHeader = configurationObject.isTracingHeader
      initialized = true
    }

    override def isInitialized: Boolean = initialized
  }

  object KeystoneV2ConfigListener extends UpdateListener[KeystoneV2Config] {
    private var initialized = false

    override def configurationUpdated(configurationObject: KeystoneV2Config): Unit = {
      def fixMyDefaults(stupidConfig: KeystoneV2Config): KeystoneV2Config = {
        // LOLJAXB  	(╯°□°）╯︵ ┻━┻
        //This relies on the Default Settings plugin and the fluent_api plugin added to the Jaxb code generation plugin
        // I'm sorry
        if (stupidConfig.getTenantHandling == null) {
          stupidConfig.withTenantHandling(new TenantHandlingType())
        }

        if (stupidConfig.getWhiteList == null) {
          stupidConfig.withWhiteList(new WhiteListType())
        }

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

  private final val SYSTEM_MODEL_CONFIG = "system-model.cfg.xml"
  private final val DEFAULT_CONFIG = "keystone-v2.cfg.xml"
  private final val X_AUTH_PROXY = "Proxy"
  private final val DELEGATED = "Delegated"

  implicit def autoHeaderToString(hc: HeaderConstant): String = hc.toString

  implicit def toCachingTry[T](tryToWrap: Try[T]): CachingTry[T] = new CachingTry(tryToWrap)

  def getTtl(baseTtl: Int, variability: Int, tokenOption: Option[ValidToken] = None) = {
    def safeLongToInt(l: Long) = math.min(l, Int.MaxValue).toInt

    val configuredTtl = if (baseTtl == 0 || variability == 0) {
      baseTtl
    } else {
      math.max(1, baseTtl + Random.nextInt(variability * 2 + 1) - variability) // To avoid cases where result is zero or negative
    }

    tokenOption match {
      case Some(token) =>
        val tokenExpiration = DateUtils.parseDate(token.expirationDate).getTime - System.currentTimeMillis()
        if (tokenExpiration < 1) {
          1
        } else {
          val tokenTtl = safeLongToInt(tokenExpiration / 1000)
          math.min(tokenTtl, configuredTtl)
        }
      case None =>
        configuredTtl
    }
  }

  class CachingTry[T](wrappedTry: Try[T]) {
    def cacheOnSuccess(cachingFunction: T => Unit): Try[T] = {
      wrappedTry match {
        case Success(it) =>
          cachingFunction(it)
          wrappedTry
        case f: Failure[_] => f
      }
    }
  }

  sealed trait KeystoneV2Result

  object Pass extends KeystoneV2Result

  case class Reject(status: Int, message: Option[String] = None) extends KeystoneV2Result

  case class MissingAuthTokenException(message: String, cause: Throwable = null) extends Exception(message, cause)

  case class UnauthorizedEndpointException(message: String, cause: Throwable = null) extends Exception(message, cause)

  case class InvalidTenantException(message: String, cause: Throwable = null) extends Exception(message, cause)

}
