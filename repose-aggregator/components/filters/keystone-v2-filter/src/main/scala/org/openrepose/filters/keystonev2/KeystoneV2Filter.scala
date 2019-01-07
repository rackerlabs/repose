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

import java.io.InterruptedIOException
import java.util.concurrent.TimeUnit

import javax.inject.{Inject, Named}
import javax.servlet._
import javax.servlet.http.HttpServletResponse._
import javax.servlet.http.{HttpServletRequest, HttpServletResponse}
import javax.ws.rs.core.HttpHeaders
import org.apache.commons.lang3.StringUtils
import org.apache.http.client.HttpClient
import org.apache.http.client.utils.DateUtils
import org.openrepose.commons.config.manager.UpdateListener
import org.openrepose.commons.utils.http._
import org.openrepose.commons.utils.json.JsonHeaderHelper
import org.openrepose.commons.utils.servlet.http.ResponseMode.{MUTABLE, PASSTHROUGH}
import org.openrepose.commons.utils.servlet.http.{HttpServletRequestWrapper, HttpServletResponseWrapper}
import org.openrepose.commons.utils.string.Base64Helper
import org.openrepose.core.services.config.ConfigurationService
import org.openrepose.core.services.datastore.types.{PatchableSet, SetPatch}
import org.openrepose.core.services.datastore.{Datastore, DatastoreService}
import org.openrepose.core.services.httpclient.HttpClientService
import org.openrepose.core.systemmodel.config.SystemModel
import org.openrepose.filters.keystonev2.AbstractKeystoneV2Filter.{KeystoneV2Result, Reject}
import org.openrepose.filters.keystonev2.KeystoneRequestHandler._
import org.openrepose.filters.keystonev2.KeystoneV2Authorization.{AuthorizationFailed, AuthorizationPassed, UnparsableTenantException}
import org.openrepose.filters.keystonev2.KeystoneV2Common._
import org.openrepose.filters.keystonev2.config._
import org.openrepose.nodeservice.atomfeed.{AtomFeedListener, AtomFeedService, LifecycleEvents}

import scala.Function.tupled
import scala.collection.JavaConverters._
import scala.language.implicitConversions
import scala.util.{Failure, Random, Success, Try}
import scala.xml.XML

@Named
class KeystoneV2Filter @Inject()(configurationService: ConfigurationService,
                                 httpClientService: HttpClientService,
                                 atomFeedService: AtomFeedService,
                                 datastoreService: DatastoreService)
  extends AbstractKeystoneV2Filter[KeystoneV2AuthenticationConfig](configurationService) {

  import KeystoneV2Filter._


  override val DEFAULT_CONFIG = "keystone-v2.cfg.xml"
  override val SCHEMA_LOCATION = "/META-INF/schema/config/keystone-v2.xsd"

  // The local datastore
  private val datastore: Datastore = datastoreService.getDefaultDatastore

  var ignoredRoles: Set[String] = _
  var httpClient: HttpClient = _

  private var sendTraceHeader = true
  private var isSelfValidating: Boolean = _

  override def doInit(filterConfig: FilterConfig): Unit = {
    configurationService.subscribeTo(
      SystemModelConfig,
      getClass.getResource("/META-INF/schema/system-model/system-model.xsd"),
      SystemModelConfigListener,
      classOf[SystemModel]
    )
  }

  override def doDestroy(): Unit = {
    configurationService.unsubscribeFrom(SystemModelConfig, SystemModelConfigListener)
    CacheInvalidationFeedListener.unRegisterFeeds()
  }


  override def doWork(servletRequest: HttpServletRequest, servletResponse: HttpServletResponse, chain: FilterChain): Unit = {
    val response = new HttpServletResponseWrapper(servletResponse, MUTABLE, PASSTHROUGH)

    super.doWork(servletRequest, response, chain)

    response.uncommit()
    if (response.getStatus == SC_UNAUTHORIZED) {
      response.addHeader(HttpHeaders.WWW_AUTHENTICATE, s"Keystone uri=${configuration.getIdentityService.getUri}")
    }
    response.commitToResponse()
  }

  override def doAuth(request: HttpServletRequestWrapper): Try[Unit.type] = {
    /**
      * DECLARE COMMON VALUES
      */
    lazy val traceId = Option(request.getHeader(CommonHttpHeader.TRACE_GUID)).filter(_ => sendTraceHeader)
    lazy val requestHandler = new KeystoneRequestHandler(configuration.getIdentityService.getUri, httpClient, traceId)

    /**
      * DEFINING FUNCTIONS IN SCOPE
      */
    def getAuthToken: Try[String] = {
      logger.trace("Getting the x-auth-token header value")

      Option(request.getHeader(CommonHttpHeader.AUTH_TOKEN)) match {
        case Some(token) if StringUtils.isNotBlank(token) => Success(token)
        case _ => Failure(MissingAuthTokenException("X-Auth-Token header not found"))
      }
    }

    def getValidatingToken(authToken: String, force: Boolean): Try[String] = {
      logger.trace("Getting the validating token")

      if (isSelfValidating) {
        Success(authToken)
      } else {
        getAdminToken(configuration.getIdentityService.getUsername, configuration.getIdentityService.getPassword, force)
      }
    }

    def getAdminToken(username: String, password: String, force: Boolean): Try[String] = {
      logger.trace("Getting an admin token with the configured credentials")

      // If force is true, clear the cache and acquire a new token
      if (force) datastore.remove(ADMIN_TOKEN_KEY)

      Option(datastore.get(ADMIN_TOKEN_KEY).asInstanceOf[String]) match {
        case Some(cachedAdminToken) =>
          logger.trace("Found cached admin token to use")
          Success(cachedAdminToken)
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

      request.addHeader(AuthTokenKey, s"$TOKEN_KEY_PREFIX$authToken")

      Option(datastore.get(s"$TOKEN_KEY_PREFIX$authToken").asInstanceOf[ValidToken]) map { validationResult =>
        logger.trace("Found cached user token to use")
        Success(validationResult)
      } getOrElse {
        getValidatingToken(authToken, force = false) flatMap { validatingToken =>
          requestHandler.validateToken(validatingToken, authToken, configuration.getIdentityService.isApplyRcnRoles, ignoredRoles) recoverWith {
            case _: AdminTokenUnauthorizedException =>
              // Force acquiring of the admin token, and call the validation function again (retry once)
              logger.trace("Forcing acquisition of new admin token")
              getValidatingToken(authToken, force = true) match {
                case Success(newValidatingToken) =>
                  logger.trace("Obtained admin token on second chance")
                  requestHandler.validateToken(newValidatingToken, authToken, configuration.getIdentityService.isApplyRcnRoles, ignoredRoles, checkCache = false)
                case Failure(x) => Failure(IdentityAdminTokenException("Unable to reacquire admin token", x))
              }
          } cacheOnSuccess { validToken =>
            val cacheSettings = configuration.getCache.getTimeouts
            val timeToLive = getTtl(cacheSettings.getToken, cacheSettings.getVariability, Some(validToken))

            timeToLive foreach { ttl =>
              datastore.patch(s"$USER_ID_KEY_PREFIX${validToken.userId}", SetPatch(authToken), ttl, TimeUnit.SECONDS)
              datastore.put(s"$TOKEN_KEY_PREFIX$authToken", validToken, ttl, TimeUnit.SECONDS)
            }
          }
        }
      }
    }

    def getEndpoints(authToken: String): Try[EndpointsData] = {
      logger.trace(s"Getting endpoints for: $authToken")

      // todo: extract the "make call with admin token and retry" logic since that pattern is used elsewhere
      Option(datastore.get(s"$ENDPOINTS_KEY_PREFIX$authToken").asInstanceOf[EndpointsData]) match {
        case Some(endpointsData) =>
          logger.trace("Found cached endpoints to use")
          Success(endpointsData)
        case None =>
          getValidatingToken(authToken, force = false) flatMap { adminToken =>
            requestHandler.getEndpointsForToken(adminToken, authToken, configuration.getIdentityService.isApplyRcnRoles) recoverWith {
              case _: AdminTokenUnauthorizedException =>
                // Force acquiring of the admin token, and call the endpoints function again (retry once)
                getValidatingToken(authToken, force = true) match {
                  case Success(newAdminToken) => requestHandler.getEndpointsForToken(newAdminToken, authToken, configuration.getIdentityService.isApplyRcnRoles, checkCache = false)
                  case Failure(x) => Failure(IdentityAdminTokenException("Unable to reacquire admin token", x))
                }
            } cacheOnSuccess { endpointsJson =>
              val cacheSettings = configuration.getCache.getTimeouts
              val timeToLive = getTtl(cacheSettings.getEndpoints, cacheSettings.getVariability)
              timeToLive foreach { ttl =>
                datastore.put(s"$ENDPOINTS_KEY_PREFIX$authToken", endpointsJson, ttl, TimeUnit.SECONDS)
              }
            }
          }
      }
    }

    def getGroups(authToken: String, validToken: ValidToken): Try[Vector[String]] = {
      logger.trace(s"Getting groups for: $authToken")

      Option(datastore.get(s"$GROUPS_KEY_PREFIX$authToken").asInstanceOf[Vector[String]]) match {
        case Some(groups) =>
          logger.trace("Found cached groups to use")
          Success(groups)
        case None =>
          getValidatingToken(authToken, force = false) flatMap { adminToken =>
            requestHandler.getGroups(adminToken, validToken.userId) recoverWith {
              case _: AdminTokenUnauthorizedException =>
                // Force acquiring of the admin token, and call the endpoints function again (retry once)
                getValidatingToken(authToken, force = true) match {
                  case Success(newAdminToken) => requestHandler.getGroups(newAdminToken, validToken.userId, checkCache = false)
                  case Failure(x) => Failure(IdentityAdminTokenException("Unable to reacquire admin token", x))
                }
              case _: NotFoundException =>
                Success(Vector.empty)
            } cacheOnSuccess { groups =>
              val cacheSettings = configuration.getCache.getTimeouts
              val timeToLive = getTtl(cacheSettings.getGroup, cacheSettings.getVariability)
              timeToLive foreach { ttl =>
                datastore.put(s"$GROUPS_KEY_PREFIX$authToken", groups, ttl, TimeUnit.SECONDS)
              }
            }
          }
      }
    }

    def buildTenantToRolesMap(token: ValidToken): TenantToRolesMap = {
      token.defaultTenantId.map(_ -> Set.empty[String]).toMap ++
        token.roles.groupBy(_.tenantId).map({ case (key, value) => key.getOrElse(DomainRoleTenantKey) -> value.map(_.name).toSet })
    }

    def buildTenantHeader(defaultTenant: Option[String],
                          roleTenants: Set[String],
                          matchedTenants: Set[String]): Set[ValueWithQuality] = {
      val sendAllTenants = configuration.getTenantHandling.isSendAllTenantIds
      val sendTenantIdQuality = Option(configuration.getTenantHandling.getSendTenantIdQuality)
      val sendQuality = sendTenantIdQuality.isDefined
      val defaultTenantQuality = sendTenantIdQuality.map(_.getDefaultTenantQuality).getOrElse(0.0)
      val matchedTenantQuality = sendTenantIdQuality.map(_.getUriTenantQuality).getOrElse(0.0)
      val rolesTenantQuality = sendTenantIdQuality.map(_.getRolesTenantQuality).getOrElse(0.0)

      val preferredTenants =
        if (matchedTenants.nonEmpty) {
          matchedTenants map {
            case matchedTenant if defaultTenant.exists(matchedTenant.equals) =>
              matchedTenant -> Math.max(matchedTenantQuality, defaultTenantQuality)
            case matchedTenant =>
              matchedTenant -> matchedTenantQuality
          }
        } else if (defaultTenant.nonEmpty) {
          defaultTenant.toSet[String].map(_ -> defaultTenantQuality)
        } else if (roleTenants.nonEmpty) {
          val tenantsFromRoles = roleTenants.map(_ -> rolesTenantQuality)
          if (sendAllTenants) tenantsFromRoles else tenantsFromRoles.take(1)
        } else {
          Set.empty
        }

      if (sendAllTenants && sendQuality) {
        val matched = matchedTenants.map(_ -> Some(matchedTenantQuality))
        val default = defaultTenant.map(_ -> Some(defaultTenantQuality))
        val roles = roleTenants.map(_ -> Some(rolesTenantQuality))
        matched ++ default ++ roles
      } else if (sendAllTenants && !sendQuality) {
        (defaultTenant.toSet ++ roleTenants).map(_ -> None)
      } else if (!sendAllTenants && sendQuality) {
        preferredTenants.map(tupled((tenant, quality) => tenant -> Some(quality)))
      } else {
        preferredTenants.map(tupled((tenant, _) => tenant -> None))
      }
    }

    def addTokenHeaders(token: ValidToken, scopedTenantToRolesMap: TenantToRolesMap, matchedTenants: Set[String]): Unit = {
      // Add standard headers
      request.addHeader(OpenStackServiceHeader.USER_ID, token.userId)
      request.addHeader(OpenStackServiceHeader.X_EXPIRATION, token.expirationDate)
      token.username.foreach(request.addHeader(PowerApiHeader.USER, _))
      token.username.foreach(request.addHeader(OpenStackServiceHeader.USER_NAME, _))
      token.tenantName.foreach(request.addHeader(OpenStackServiceHeader.TENANT_NAME, _))
      token.domainId.foreach(request.addHeader(OpenStackServiceHeader.DOMAIN_ID, _))
      token.defaultRegion.foreach(request.addHeader(OpenStackServiceHeader.DEFAULT_REGION, _))
      token.contactId.foreach(request.addHeader(OpenStackServiceHeader.CONTACT_ID, _))
      token.impersonatorId.foreach(request.addHeader(OpenStackServiceHeader.IMPERSONATOR_ID, _))
      token.impersonatorName.foreach(request.addHeader(OpenStackServiceHeader.IMPERSONATOR_NAME, _))

      // Construct and add impersonator roles, if available
      val impersonatorRoles = token.impersonatorRoles
      if (impersonatorRoles.nonEmpty) {
        request.addHeader(OpenStackServiceHeader.IMPERSONATOR_ROLES, impersonatorRoles.mkString(","))
      }

      // Construct and add the tenant id header
      buildTenantHeader(token.defaultTenantId, token.tenantIds.toSet, matchedTenants) foreach {
        case (tenant, Some(quality)) => request.appendHeader(OpenStackServiceHeader.TENANT_ID, tenant, quality)
        case (tenant, None) => request.appendHeader(OpenStackServiceHeader.TENANT_ID, tenant)
      }

      // If configured, add roles header
      if (configuration.getIdentityService.isSetRolesInHeader) {
        val sendAllTenantIds = configuration.getTenantHandling.isSendAllTenantIds
        val tenantToRolesMap = if (sendAllTenantIds) buildTenantToRolesMap(token) else scopedTenantToRolesMap
        if (tenantToRolesMap.nonEmpty) {
          request.addHeader(OpenStackServiceHeader.TENANT_ROLES_MAP, JsonHeaderHelper.anyToJsonHeader(tenantToRolesMap))
        }

        Option(configuration.getTenantHandling.getValidateTenant).map(_.isEnableLegacyRolesMode) match {
          case Some(true) => token.roles.map(_.name).foreach(request.appendHeader(OpenStackServiceHeader.ROLES, _))
          case _ => scopedTenantToRolesMap.values.flatten.foreach(request.appendHeader(OpenStackServiceHeader.ROLES, _))
        }
      }

      // If present, add the tenant from the URI as part of the Proxy header, otherwise use the default tenant id
      if (matchedTenants.nonEmpty) {
        matchedTenants.map(tenant => s"$XAuthProxy $tenant").foreach(request.addHeader(OpenStackServiceHeader.EXTENDED_AUTHORIZATION, _))
      } else {
        token.defaultTenantId match {
          case Some(tenant) =>
            request.addHeader(OpenStackServiceHeader.EXTENDED_AUTHORIZATION, s"$XAuthProxy $tenant")
          case None =>
            request.addHeader(OpenStackServiceHeader.EXTENDED_AUTHORIZATION, XAuthProxy)
        }
      }

      // Construct and add authenticatedBy, if available
      token.authenticatedBy foreach {
        _ foreach {
          request.addHeader(OpenStackServiceHeader.AUTHENTICATED_BY, _)
        }
      }
    }

    def addCatalogHeader(maybeEndpoints: => Try[EndpointsData]): Try[Unit.type] = {
      if (configuration.getIdentityService.isSetCatalogInHeader) {
        maybeEndpoints map { endpoints =>
          request.addHeader(PowerApiHeader.X_CATALOG, Base64Helper.base64EncodeUtf8(endpoints.json))
          Unit
        }
      } else {
        Success(Unit)
      }
    }

    def addGroupsHeader(maybeGroups: => Try[Vector[String]]): Try[Unit.type] = {
      if (configuration.getIdentityService.isSetGroupsInHeader) {
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

    /**
      * BEGIN PROCESSING
      */
    getAuthToken flatMap { authToken =>
      validateToken(authToken) flatMap { validToken =>
        lazy val endpoints = getEndpoints(authToken) // Prevents making call if its not needed
        val tenantToRolesMap = buildTenantToRolesMap(validToken)
        val authResult = KeystoneV2Authorization.doAuthorization(configuration, request, tenantToRolesMap, endpoints)

        addTokenHeaders(validToken, authResult.scopedTenantToRolesMap, authResult.matchedTenants)
        authResult match {
          case AuthorizationPassed(_, _) =>
            addCatalogHeader(endpoints) flatMap { _ =>
              addGroupsHeader(getGroups(authToken, validToken))
            }
          case AuthorizationFailed(_, _, exception) => Failure(exception)
        }
      }
    }
  }

  override val handleFailures: PartialFunction[Try[Unit.type], KeystoneV2Result] = {
    KeystoneV2Authorization.handleFailures orElse {
      case Failure(e: MissingAuthTokenException) => Reject(SC_UNAUTHORIZED, Some(e.getMessage))
      case Failure(e: NotFoundException) => Reject(SC_UNAUTHORIZED, Some(e.getMessage))
      case Failure(e: UnparsableTenantException) => Reject(SC_UNAUTHORIZED, Some(e.getMessage))
      case Failure(e: IdentityCommunicationException) => Reject(SC_BAD_GATEWAY, Some(e.getMessage))
      case Failure(e: OverLimitException) =>
        if (isSelfValidating) {
          Reject(e.statusCode, Some(e.getMessage), Map(HttpHeaders.RETRY_AFTER -> e.retryAfter))
        } else {
          Reject(SC_SERVICE_UNAVAILABLE, Some(e.getMessage), Map(HttpHeaders.RETRY_AFTER -> e.retryAfter))
        }
      case Failure(e) if e.getCause.isInstanceOf[InterruptedIOException] =>
        Reject(SC_GATEWAY_TIMEOUT, Some(s"Call timed out: ${e.getMessage}"))
      case Failure(_: AdminTokenUnauthorizedException) if isSelfValidating =>
        Reject(SC_UNAUTHORIZED, Some("Token unauthorized"))
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
      case (Some(_), None) => None
      case (Some(tttl), Some(cttl)) => Some(Math.min(tttl, cttl))
      case (None, Some(cttl)) => Some(cttl)
      case (None, None) => None
    }
  }

  override def filterInitialized: Boolean = SystemModelConfigListener.isInitialized && super.filterInitialized

  object SystemModelConfigListener extends UpdateListener[SystemModel] {
    private var initialized = false

    override def configurationUpdated(configurationObject: SystemModel): Unit = {
      sendTraceHeader = Option(configurationObject.getTracingHeader).isEmpty || configurationObject.getTracingHeader.isEnabled
      initialized = true
    }

    override def isInitialized: Boolean = initialized
  }

  override def doConfigurationUpdated(configurationObject: KeystoneV2AuthenticationConfig): KeystoneV2AuthenticationConfig = {
    def fixMyDefaults(stupidConfig: KeystoneV2AuthenticationConfig): KeystoneV2AuthenticationConfig = {
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

    val config = fixMyDefaults(super.doConfigurationUpdated(configurationObject))

    Option(config.getTenantHandling).map(_.getValidateTenant) foreach { _ =>
      logger.warn("Tenant validation has been moved to the keystone-v2-authorization filter, and is considered deprecated in the keystone-v2 filter")
    }
    Option(config.getRequireServiceEndpoint) foreach { _ =>
      logger.warn("Service endpoint requirements have been moved to the keystone-v2-authorization filter, and are considered deprecated in the keystone-v2 filter")
    }
    Option(config.getPreAuthorizedRoles) foreach { _ =>
      logger.warn("Pre-authorized roles have been moved to the keystone-v2-authorization filter, and are considered deprecated in the keystone-v2 filter")
    }

    // Removes an extra slash at the end of the URI if applicable
    val serviceUri = config.getIdentityService.getUri
    config.getIdentityService.setUri(serviceUri.stripSuffix("/"))
    CacheInvalidationFeedListener.registerFeeds(
      // This will also force the un-registering of the Atom Feeds if the new config doesn't have a Cache element.
      Option(config.getCache).getOrElse(new CacheType).getAtomFeed.asScala.toList
    )

    httpClient = httpClientService.getClient(config.getIdentityService.getConnectionPoolId)

    ignoredRoles = config.getIgnoredRoles.split(' ').to[Set]

    isSelfValidating = Option(config.getIdentityService.getUsername).isEmpty ||
      Option(config.getIdentityService.getPassword).isEmpty

    config
  }

  object CacheInvalidationFeedListener extends AtomFeedListener {

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
      logger.debug("Processing atom feed entry: {}", atomEntry)
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

    case class RegisteredFeed(id: String, unique: String)

  }

}

object KeystoneV2Filter {

  private final val SystemModelConfig = "system-model.cfg.xml"
  private final val XAuthProxy = "Proxy"

  val AuthTokenKey = "X-Auth-Token-Key"

  type ValueWithQuality = (String, Option[Double])

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

  abstract class AuthenticationException(message: String, cause: Throwable) extends Exception(message, cause)

  case class MissingAuthTokenException(message: String, cause: Throwable = null) extends AuthenticationException(message, cause)

}
