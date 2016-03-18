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
import java.util.concurrent.{TimeUnit, TimeoutException}
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
import org.openrepose.commons.utils.servlet.http.HttpServletRequestWrapper
import org.openrepose.core.filter.FilterConfigHelper
import org.openrepose.core.services.config.ConfigurationService
import org.openrepose.core.services.datastore.types.{PatchableSet, SetPatch}
import org.openrepose.core.services.datastore.{Datastore, DatastoreService}
import org.openrepose.core.services.serviceclient.akka.{AkkaServiceClient, AkkaServiceClientException, AkkaServiceClientFactory}
import org.openrepose.core.systemmodel.SystemModel
import org.openrepose.filters.keystonev2.KeystoneRequestHandler._
import org.openrepose.filters.keystonev2.config._
import org.openrepose.nodeservice.atomfeed.{AtomFeedListener, AtomFeedService, LifecycleEvents}

import scala.collection.JavaConverters._
import scala.language.implicitConversions
import scala.util.{Failure, Random, Success, Try}
import scala.xml.XML

@Named
class KeystoneV2Filter @Inject()(configurationService: ConfigurationService,
                                 akkaServiceClientFactory: AkkaServiceClientFactory,
                                 atomFeedService: AtomFeedService,
                                 datastoreService: DatastoreService)
  extends Filter
    with HttpDelegationManager
    with LazyLogging {

  import KeystoneV2Filter._

  // The local datastore
  private val datastore: Datastore = datastoreService.getDefaultDatastore

  var keystoneV2Config: KeystoneV2Config = _
  var akkaServiceClient: AkkaServiceClient = _

  private var configurationFile: String = DEFAULT_CONFIG
  private var sendTraceHeader = true

  override def init(filterConfig: FilterConfig): Unit = {
    configurationFile = new FilterConfigHelper(filterConfig).getFilterConfig(DEFAULT_CONFIG)
    logger.info(s"Initializing Keystone V2 Filter using config $configurationFile")
    val xsdURL: URL = this.getClass.getResource("/META-INF/schema/config/keystone-v2.xsd")
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
    Option(akkaServiceClient).foreach(_.destroy())
    configurationService.unsubscribeFrom(configurationFile, KeystoneV2ConfigListener)
    configurationService.unsubscribeFrom(SYSTEM_MODEL_CONFIG, SystemModelConfigListener)
    CacheInvalidationFeedListener.unRegisterFeeds()
  }

  override def doFilter(servletRequest: ServletRequest, servletResponse: ServletResponse, chain: FilterChain): Unit = {
    /**
      * STATIC REFERENCE TO CONFIG
      */
    val config = keystoneV2Config

    /**
      * DECLARE COMMON VALUES
      */
    lazy val request = new HttpServletRequestWrapper(servletRequest.asInstanceOf[HttpServletRequest])
    // Not using the mutable wrapper because it doesn't work properly at the moment, and
    // we don't need to modify the response from further down the chain
    lazy val response = servletResponse.asInstanceOf[HttpServletResponse]
    lazy val traceId = Option(request.getHeader(CommonHttpHeader.TRACE_GUID.toString)).filter(_ => sendTraceHeader)
    lazy val requestHandler = new KeystoneRequestHandler(keystoneV2Config.getIdentityService.getUri, akkaServiceClient, traceId)
    lazy val isSelfValidating = Option(config.getIdentityService.getUsername).isEmpty ||
      Option(config.getIdentityService.getPassword).isEmpty

    /**
      * BEGIN PROCESSING
      */
    if (!isInitialized) {
      logger.error("Keystone v2 filter has not yet initialized")
      response.sendError(SC_INTERNAL_SERVER_ERROR)
    } else {
      logger.debug("Keystone v2 filter processing request...")

      val keystoneAuthenticateHeader = s"Keystone uri=${keystoneV2Config.getIdentityService.getUri}"

      val filterResult =
        if (isWhitelisted(request.getRequestURI)) {
          Pass
        } else {
          val processingResult =
            getAuthToken flatMap { authToken =>
              validateToken(authToken) flatMap { validToken =>
                val doTenantCheck = doAuthorizeTenant(validToken)
                val matchedUriTenant = getMatchingUriTenant(doTenantCheck, validToken)
                addTokenHeaders(validToken, matchedUriTenant.getOrElse(None))
                authorizeTenant(doTenantCheck, matchedUriTenant) flatMap { _ =>
                  lazy val endpoints = getEndpoints(authToken, validToken) // Prevents making call if its not needed
                  addCatalogHeader(endpoints) flatMap { _ =>
                    authorizeEndpoints(validToken, endpoints) flatMap { _ =>
                      addGroupsHeader(getGroups(authToken, validToken))
                    }
                  }
                }
              }
            }

          processingResult match {
            case Success(_) => Pass
            case Failure(e: MissingAuthTokenException) => Reject(SC_UNAUTHORIZED, Some(e.getMessage))
            case Failure(e: NotFoundException) => Reject(SC_UNAUTHORIZED, Some(e.getMessage))
            case Failure(e: InvalidTenantException) => Reject(SC_UNAUTHORIZED, Some(e.getMessage))
            case Failure(e: UnparseableTenantException) => Reject(SC_UNAUTHORIZED, Some(e.getMessage))
            case Failure(e: IdentityCommunicationException) => Reject(SC_BAD_GATEWAY, Some(e.getMessage))
            case Failure(e: UnauthorizedEndpointException) => Reject(SC_FORBIDDEN, Some(e.getMessage))
            case Failure(e: OverLimitException) =>
              response.addHeader(HttpHeaders.RETRY_AFTER, e.retryAfter)
              if (isSelfValidating) {
                Reject(e.statusCode, Some(e.getMessage))
              } else {
                Reject(SC_SERVICE_UNAVAILABLE, Some(e.getMessage))
              }
            case Failure(e) if e.getCause.isInstanceOf[AkkaServiceClientException] && e.getCause.getCause.isInstanceOf[TimeoutException] =>
              Reject(SC_GATEWAY_TIMEOUT, Some(s"Call timed out: ${e.getMessage}"))
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

              if (response.getStatus == SC_UNAUTHORIZED) {
                response.addHeader(CommonHttpHeader.WWW_AUTHENTICATE.toString, keystoneAuthenticateHeader)
              }
            case None =>
              logger.debug(s"Rejecting with status $statusCode")

              if (statusCode == SC_UNAUTHORIZED) {
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
    def tenantFromUri: Try[Option[String]] = {
      Try(
        Option(config.getTenantHandling.getValidateTenant) flatMap { validateTenantConfig =>
          Option(validateTenantConfig.getUriExtractionRegex) flatMap { uriExtractionRegex =>
            val regex = uriExtractionRegex.r
            request.getRequestURI match {
              case regex(tenantId, _*) => Some(tenantId)
              case _ => throw UnparseableTenantException("Could not parse tenant from the URI")
            }
          }
        }
      )
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

    def getValidatingToken(authToken: String, force: Boolean): Try[String] = {
      logger.trace("Getting the validating token")

      if (isSelfValidating) {
        Success(authToken)
      } else {
        getAdminToken(config.getIdentityService.getUsername, config.getIdentityService.getPassword, force)
      }
    }

    def getAdminToken(username: String, password: String, force: Boolean): Try[String] = {
      logger.trace("Getting an admin token with the configured credentials")

      // If force is true, clear the cache and acquire a new token
      if (force) datastore.remove(ADMIN_TOKEN_KEY)

      Option(datastore.get(ADMIN_TOKEN_KEY).asInstanceOf[String]) match {
        case Some(cachedAdminToken) => Success(cachedAdminToken)
        case None =>
          requestHandler.getAdminToken(username, password) match {
            case Success(adminToken) =>
              datastore.put(ADMIN_TOKEN_KEY, adminToken)
              Success(adminToken)
            case f: Failure[_] => f
          }
      }
    }

    def validateToken(authToken: String): Try[ValidToken] = {
      logger.trace(s"Validating token: $authToken")

      Option(datastore.get(s"$TOKEN_KEY_PREFIX$authToken").asInstanceOf[ValidToken]) map { validationResult =>
        Success(validationResult)
      } getOrElse {
        getValidatingToken(authToken, force = false) flatMap { validatingToken =>
          requestHandler.validateToken(validatingToken, authToken) recoverWith {
            case _: AdminTokenUnauthorizedException =>
              // Force acquiring of the admin token, and call the validation function again (retry once)
              getValidatingToken(authToken, force = true) match {
                case Success(newValidatingToken) => requestHandler.validateToken(newValidatingToken, authToken)
                case Failure(x) => Failure(IdentityAdminTokenException("Unable to reacquire admin token", x))
              }
          } cacheOnSuccess { validToken =>
            val cacheSettings = config.getCache.getTimeouts
            val timeToLive = getTtl(cacheSettings.getToken,
              cacheSettings.getVariability,
              Some(validToken))

            timeToLive foreach { ttl =>
              datastore.patch(s"$USER_ID_KEY_PREFIX${validToken.userId}", new SetPatch(authToken), ttl, TimeUnit.SECONDS)
              datastore.put(s"$TOKEN_KEY_PREFIX$authToken", validToken, ttl, TimeUnit.SECONDS)
            }
          }
        }
      }
    }

    def doAuthorizeTenant(validToken: ValidToken): Boolean = {
      Option(config.getTenantHandling.getValidateTenant) exists { validateTenant =>
        Option(config.getPreAuthorizedRoles) forall {
          _.getRole.asScala.intersect(validToken.roles).isEmpty
        }
      }
    }

    def getMatchingUriTenant(doTenantCheck: Boolean, validToken: ValidToken): Try[Option[String]] = {
      if (doTenantCheck) {
        tenantFromUri map {
          _ flatMap { uriTenant =>
            val tokenTenants = validToken.defaultTenantId.toSet ++ validToken.tenantIds
            val prefixes = Option(config.getTenantHandling.getValidateTenant.getStripTokenTenantPrefixes).map(_.split('/')).getOrElse(Array.empty[String])
            tokenTenants find { tokenTenant =>
              tokenTenant.equals(uriTenant) || prefixes.exists(prefix =>
                tokenTenant.startsWith(prefix) && tokenTenant.substring(prefix.length).equals(uriTenant)
              )
            }
          }
        }
      } else {
        Success(None)
      }
    }

    def authorizeTenant(doTenantCheck: Boolean, matchedUriTenant: Try[Option[String]]): Try[Unit.type] = {
      logger.trace("Validating tenant")

      if (doTenantCheck) {
        matchedUriTenant flatMap { uriTenant =>
          if (uriTenant.isDefined) Success(Unit)
          else Failure(InvalidTenantException("Tenant from URI does not match any of the tenants associated with the provided token"))
        }
      } else {
        Success(Unit)
      }
    }

    def getEndpoints(authToken: String, validToken: ValidToken): Try[EndpointsData] = {
      logger.trace(s"Getting endpoints for: $authToken")

      // todo: extract the "make call with admin token and retry" logic since that pattern is used elsewhere
      Option(datastore.get(s"$ENDPOINTS_KEY_PREFIX$authToken").asInstanceOf[EndpointsData]) match {
        case Some(endpointsData) => Success(endpointsData)
        case None =>
          getValidatingToken(authToken, force = false) flatMap { adminToken =>
            requestHandler.getEndpointsForToken(adminToken, authToken) recoverWith {
              case _: AdminTokenUnauthorizedException =>
                // Force acquiring of the admin token, and call the endpoints function again (retry once)
                getValidatingToken(authToken, force = true) match {
                  case Success(newAdminToken) => requestHandler.getEndpointsForToken(newAdminToken, authToken)
                  case Failure(x) => Failure(IdentityAdminTokenException("Unable to reacquire admin token", x))
                }
            } cacheOnSuccess { endpointsJson =>
              val cacheSettings = config.getCache.getTimeouts
              val timeToLive = getTtl(cacheSettings.getEndpoints, cacheSettings.getVariability)
              timeToLive foreach { ttl =>
                datastore.put(s"$ENDPOINTS_KEY_PREFIX$authToken", endpointsJson, ttl, TimeUnit.SECONDS)
              }
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

            val preAuthRoles = Option(config.getPreAuthorizedRoles)
              .map(_.getRole.asScala)
              .getOrElse(List.empty)

            if (preAuthRoles.intersect(validToken.roles).nonEmpty || endpoints.vector.exists(_.meetsRequirement(requiredEndpoint))) Success(Unit)
            else Failure(UnauthorizedEndpointException("User did not have the required endpoint"))
          }
        case None => Success(Unit)
      }
    }

    def getGroups(authToken: String, validToken: ValidToken): Try[Vector[String]] = {
      logger.trace(s"Getting groups for: $authToken")

      Option(datastore.get(s"$GROUPS_KEY_PREFIX$authToken").asInstanceOf[Vector[String]]) match {
        case Some(groups) => Success(groups)
        case None =>
          getValidatingToken(authToken, force = false) flatMap { adminToken =>
            requestHandler.getGroups(adminToken, validToken.userId) recoverWith {
              case _: AdminTokenUnauthorizedException =>
                // Force acquiring of the admin token, and call the endpoints function again (retry once)
                getValidatingToken(authToken, force = true) match {
                  case Success(newAdminToken) => requestHandler.getGroups(newAdminToken, validToken.userId)
                  case Failure(x) => Failure(IdentityAdminTokenException("Unable to reacquire admin token", x))
                }
              case _: NotFoundException =>
                Success(Vector.empty)
            } cacheOnSuccess { groups =>
              val cacheSettings = config.getCache.getTimeouts
              val timeToLive = getTtl(cacheSettings.getGroup, cacheSettings.getVariability)
              timeToLive foreach { ttl =>
                datastore.put(s"$GROUPS_KEY_PREFIX$authToken", groups, ttl, TimeUnit.SECONDS)
              }
            }
          }
      }
    }

    def buildTenantHeader(defaultTenant: Option[String],
                          roleTenants: Seq[String],
                          matchedUriTenant: Option[String]): Vector[String] = {
      val sendAllTenants = config.getTenantHandling.isSendAllTenantIds
      val sendTenantIdQuality = Option(config.getTenantHandling.getSendTenantIdQuality)
      val sendQuality = sendTenantIdQuality.isDefined
      val defaultTenantQuality = sendTenantIdQuality.map(_.getDefaultTenantQuality).getOrElse(0.0)
      val uriTenantQuality = sendTenantIdQuality.map(_.getUriTenantQuality).getOrElse(0.0)
      val rolesTenantQuality = sendTenantIdQuality.map(_.getRolesTenantQuality).getOrElse(0.0)

      case class PreferredTenant(id: String, quality: Double)

      val preferredTenant = (defaultTenant, matchedUriTenant) match {
        case (Some(default), Some(uri)) =>
          val quality = if (default.equals(uri)) {
            math.max(defaultTenantQuality, uriTenantQuality)
          } else {
            uriTenantQuality
          }
          Some(PreferredTenant(uri, quality))
        case (None, Some(uri)) =>
          Some(PreferredTenant(uri, uriTenantQuality))
        case (Some(default), None) =>
          Some(PreferredTenant(default, defaultTenantQuality))
        case (None, None) =>
          if (roleTenants.nonEmpty) Some(PreferredTenant(roleTenants.head, rolesTenantQuality))
          else None
      }

      if (sendAllTenants && sendQuality) {
        val priorityTenants = (defaultTenant, matchedUriTenant) match {
          case (Some(default), Some(uri)) => Vector(s"$default;q=$defaultTenantQuality", s"$uri;q=$uriTenantQuality")
          case (Some(default), None) => Vector(s"$defaultTenant;q=$defaultTenantQuality")
          case (None, Some(uri)) => Vector(s"$uri;q=$uriTenantQuality")
          case (None, None) => Vector.empty[String]
        }
        priorityTenants ++ roleTenants.map(tid => s"$tid;q=$rolesTenantQuality")
      } else if (sendAllTenants && !sendQuality) {
        (defaultTenant.toSet ++ roleTenants).toVector
      } else if (!sendAllTenants && sendQuality) {
        preferredTenant.map(tenant => Vector(s"${tenant.id};q=${tenant.quality}")).getOrElse(Vector.empty)
      } else {
        preferredTenant.map(tenant => Vector(s"${tenant.id}")).getOrElse(Vector.empty)
      }
    }

    def addTokenHeaders(token: ValidToken, matchedUriTenant: Option[String]): Unit = {
      // Add standard headers
      request.addHeader(OpenStackServiceHeader.USER_ID.toString, token.userId)
      request.addHeader(OpenStackServiceHeader.X_EXPIRATION.toString, token.expirationDate)
      token.username.foreach(request.addHeader(PowerApiHeader.USER.toString, _))
      token.username.foreach(request.addHeader(OpenStackServiceHeader.USER_NAME.toString, _))
      token.tenantName.foreach(request.addHeader(OpenStackServiceHeader.TENANT_NAME.toString, _))
      token.defaultRegion.foreach(request.addHeader(OpenStackServiceHeader.DEFAULT_REGION.toString, _))
      token.contactId.foreach(request.addHeader(OpenStackServiceHeader.CONTACT_ID.toString, _))
      token.impersonatorId.foreach(request.addHeader(OpenStackServiceHeader.IMPERSONATOR_ID.toString, _))
      token.impersonatorName.foreach(request.addHeader(OpenStackServiceHeader.IMPERSONATOR_NAME.toString, _))

      // Construct and add impersonator roles, if available
      val impersonatorRoles = token.impersonatorRoles
      if (impersonatorRoles.nonEmpty) {
        request.addHeader(OpenStackServiceHeader.IMPERSONATOR_ROLES.toString, impersonatorRoles.mkString(","))
      }

      // Construct and add the tenant id header
      val tenantsToPass = buildTenantHeader(token.defaultTenantId, token.tenantIds, matchedUriTenant)
      if (tenantsToPass.nonEmpty) {
        request.addHeader(OpenStackServiceHeader.TENANT_ID.toString, tenantsToPass.mkString(","))
      }

      // If configured, add roles header
      if (config.getIdentityService.isSetRolesInHeader) {
        request.addHeader(OpenStackServiceHeader.ROLES.toString, token.roles.mkString(","))
      }

      // If present, add the tenant from the URI as part of the Proxy header, otherwise use the default tenant id
      matchedUriTenant match {
        case Some(uriTenant) =>
          request.addHeader(OpenStackServiceHeader.EXTENDED_AUTHORIZATION.toString, s"$X_AUTH_PROXY $uriTenant")
        case None =>
          token.defaultTenantId match {
            case Some(tenant) =>
              request.addHeader(OpenStackServiceHeader.EXTENDED_AUTHORIZATION.toString, s"$X_AUTH_PROXY $tenant")
            case None =>
              request.addHeader(OpenStackServiceHeader.EXTENDED_AUTHORIZATION.toString, X_AUTH_PROXY)
          }
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

  def getTtl(baseTtl: Int, variability: Int, tokenOption: Option[ValidToken] = None): Option[Int] = {
    def safeLongToInt(l: Long): Int = math.min(l, Int.MaxValue).toInt

    val tokenTtl = {
      tokenOption match {
        case Some(token) =>
          // If a token has been provided, calculate the TTL
          val tokenExpiration = DateUtils.parseDate(token.expirationDate).getTime - System.currentTimeMillis()

          if (tokenExpiration < 1) {
            // If the token has already expired, don't cache
            None
          } else {
            val tokenExpirationSeconds = tokenExpiration / 1000

            if (tokenExpirationSeconds > Int.MaxValue) {
              logger.warn("Token expiration time exceeds maximum possible value -- setting to maximum possible value")
            }
            // Cache for the token TTL after converting from milliseconds to seconds
            Some(safeLongToInt(tokenExpirationSeconds))
          }
        case None =>
          // If a token has not been provided, don't cache
          None
      }
    }

    val configuredTtl = {
      if (baseTtl == -1) {
        // Caching is disabled by configuration
        None
      } else if (baseTtl == 0) {
        // Caching is set to forever
        Some(baseTtl)
      } else {
        val modifiedTtl = baseTtl + Random.nextInt(variability * 2 + 1) - variability

        if (modifiedTtl > 0) {
          // Cache for the calculated configured TTL
          Some(modifiedTtl)
        } else {
          // Caching would have been negative, but instead, we simply don't cache
          None
        }
      }
    }

    (tokenTtl, configuredTtl) match {
      case (Some(tttl), None) => None
      case (Some(tttl), Some(cttl)) => Some(Math.min(tttl, cttl))
      case (None, Some(cttl)) => Some(cttl)
      case (None, None) => None
    }
  }

  def isInitialized = SystemModelConfigListener.isInitialized && KeystoneV2ConfigListener.isInitialized

  object SystemModelConfigListener extends UpdateListener[SystemModel] {
    private var initialized = false

    override def configurationUpdated(configurationObject: SystemModel): Unit = {
      sendTraceHeader = Option(configurationObject.getTracingHeader).isEmpty || configurationObject.getTracingHeader.isEnabled
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

      // Removes an extra slash at the end of the URI if applicable
      val serviceUri = keystoneV2Config.getIdentityService.getUri
      keystoneV2Config.getIdentityService.setUri(serviceUri.stripSuffix("/"))
      CacheInvalidationFeedListener.registerFeeds(
        // This will also force the un-registering of the Atom Feeds if the new config doesn't have a Cache element.
        Option(keystoneV2Config.getCache).getOrElse(new CacheType).getAtomFeed.asScala.toList
      )

      val akkaServiceClientOld = Option(akkaServiceClient)
      akkaServiceClient = akkaServiceClientFactory.newAkkaServiceClient(keystoneV2Config.getIdentityService.getConnectionPoolId)
      akkaServiceClientOld.foreach(_.destroy())

      initialized = true
    }

    override def isInitialized: Boolean = initialized
  }

  object CacheInvalidationFeedListener extends AtomFeedListener {

    case class RegisteredFeed(id: String, unique: String)

    private var registeredFeeds = List.empty[RegisteredFeed]

    def unRegisterFeeds(): Unit = {
      registeredFeeds.synchronized {
        registeredFeeds.foreach(feed =>
          atomFeedService.unregisterListener(feed.unique)
        )
      }
    }

    def registerFeeds(feeds: List[AtomFeedType]): Unit = {
      registeredFeeds.synchronized {
        // Unregister the feeds we no longer care about.
        val inFeeds = feeds.map(_.getId)
        val feedsToUnRegister = registeredFeeds.filterNot(regFeed => inFeeds.contains(regFeed.id))
        feedsToUnRegister.foreach(feed => atomFeedService.unregisterListener(feed.unique))
        // Register with only the new feeds we aren't already registered with.
        val registeredFeedIds = registeredFeeds.map(_.id)
        val feedsToRegister = inFeeds.filterNot(posFeed => registeredFeedIds.contains(posFeed))
        val newRegisteredFeeds = feedsToRegister.map(feedId => RegisteredFeed(feedId, atomFeedService.registerListener(feedId, this)))
        // Update to the still and newly registered feeds.
        registeredFeeds = registeredFeeds.diff(feedsToUnRegister) ++ newRegisteredFeeds
      }
    }

    override def onNewAtomEntry(atomEntry: String): Unit = {
      val atomXml = XML.loadString(atomEntry)
      val resourceId = (atomXml \\ "event" \\ "@resourceId").map(_.text).headOption
      if (resourceId.isDefined) {
        val resourceType = (atomXml \\ "event" \\ "@resourceType").map(_.text)
        val authTokens: Option[collection.Set[String]] = resourceType.headOption match {
          // User OR Token Revocation Record (TRR) event
          case Some("USER") | Some("TRR_USER") =>
            val tokens = Option(datastore.get(s"$USER_ID_KEY_PREFIX${resourceId.get}").asInstanceOf[PatchableSet[String]])
            datastore.remove(s"$USER_ID_KEY_PREFIX${resourceId.get}")
            tokens
          case Some("TOKEN") => Some(Set(resourceId.get))
          case _ => None
        }

        authTokens.getOrElse(List.empty[String]).foreach(authToken => {
          datastore.remove(s"$TOKEN_KEY_PREFIX$authToken")
          datastore.remove(s"$ENDPOINTS_KEY_PREFIX$authToken")
          datastore.remove(s"$GROUPS_KEY_PREFIX$authToken")
        })
      }
    }

    override def onLifecycleEvent(event: LifecycleEvents): Unit = {
      logger.debug(s"Received Lifecycle Event: $event")
    }
  }

}

object KeystoneV2Filter {

  private final val SYSTEM_MODEL_CONFIG = "system-model.cfg.xml"
  private final val DEFAULT_CONFIG = "keystone-v2.cfg.xml"
  private final val X_AUTH_PROXY = "Proxy"
  private final val DELEGATED = "Delegated"

  implicit def autoHeaderToString(hc: HeaderConstant): String = hc.toString

  implicit def toCachingTry[T](tryToWrap: Try[T]): CachingTry[T] = new CachingTry(tryToWrap)

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
