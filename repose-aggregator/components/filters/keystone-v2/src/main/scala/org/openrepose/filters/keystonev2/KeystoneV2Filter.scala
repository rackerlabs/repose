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

import java.io.InputStream
import java.net.URL
import java.util.concurrent.TimeUnit
import javax.inject.{Inject, Named}
import javax.servlet._
import javax.servlet.http.HttpServletResponse._
import javax.servlet.http.{HttpServletRequest, HttpServletResponse}
import javax.ws.rs.core.MediaType

import com.fasterxml.jackson.core.JsonProcessingException
import com.rackspace.httpdelegation.HttpDelegationManager
import com.typesafe.scalalogging.slf4j.LazyLogging
import org.openrepose.commons.config.manager.UpdateListener
import org.openrepose.commons.utils.http.{PowerApiHeader, CommonHttpHeader, OpenStackServiceHeader}
import org.openrepose.commons.utils.servlet.http.MutableHttpServletRequest
import org.openrepose.core.filter.FilterConfigHelper
import org.openrepose.core.services.config.ConfigurationService
import org.openrepose.core.services.datastore.DatastoreService
import org.openrepose.core.services.serviceclient.akka.AkkaServiceClient
import org.openrepose.filters.keystonev2.config.{CacheSettingsType, CacheTimeoutsType, KeystoneV2Config, ServiceEndpointType}

import scala.collection.JavaConverters._
import scala.io.Source
import scala.util.{Failure, Success, Try}

@Named
class KeystoneV2Filter @Inject()(configurationService: ConfigurationService,
                                 akkaServiceClient: AkkaServiceClient,
                                 datastoreService: DatastoreService)
  extends Filter
  with UpdateListener[KeystoneV2Config]
  with HttpDelegationManager
  with LazyLogging {

  private final val DEFAULT_CONFIG = "keystone-v2.cfg.xml"
  private final val SC_TOO_MANY_REQUESTS = 429

  final val TOKEN_ENDPOINT = "/v2.0/tokens"
  final val GROUPS_ENDPOINT = (userId: String) => s"/v2.0/users/$userId/RAX-KSGRP"
  final val ENDPOINTS_ENDPOINT = (token: String) => s"/v2.0/tokens/$token/endpoints"
  final val ADMIN_TOKEN_KEY = "IDENTITY:V2:ADMIN_TOKEN"
  final val TOKEN_KEY_PREFIX = "IDENTITY:V2:TOKEN:"
  final val GROUPS_KEY_PREFIX = "IDENTITY:V2:GROUPS:"
  final val ENDPOINTS_KEY_PREFIX = "IDENTITY:V2:ENDPOINTS:"

  var configurationFile: String = DEFAULT_CONFIG
  var configuration: KeystoneV2Config = _
  var initialized = false

  val datastore = datastoreService.getDefaultDatastore //Which happens to be the local datastore

  override def init(filterConfig: FilterConfig): Unit = {
    configurationFile = new FilterConfigHelper(filterConfig).getFilterConfig(DEFAULT_CONFIG)
    logger.info(s"Initializing Keystone V2 Filter using config $configurationFile")
    val xsdURL: URL = getClass.getResource("/META-INF/schema/config/keystone-v2.xsd")
    configurationService.subscribeTo(
      filterConfig.getFilterName,
      configurationFile,
      xsdURL,
      this,
      classOf[KeystoneV2Config]
    )
  }

  override def destroy(): Unit = {
    configurationService.unsubscribeFrom(configurationFile, this)
  }

  implicit val autoHeaderToString: CommonHttpHeader => String = { chh =>
    chh.toString
  }

  override def doFilter(servletRequest: ServletRequest, servletResponse: ServletResponse, chain: FilterChain): Unit = {
    val request = MutableHttpServletRequest.wrap(servletRequest.asInstanceOf[HttpServletRequest])
    val response = servletResponse.asInstanceOf[HttpServletResponse] // Not using the mutable wrapper because it doesn't work properly at the moment, and we don't need to modify the response from further down the chain

    //Check our whitelist
    val whiteListURIs: Option[List[String]] = (for {
      jaxbIntermediary <- Option(configuration.getWhiteList)
      regexList <- Option(jaxbIntermediary.getUriRegex)
    } yield {
        import scala.collection.JavaConversions._
        Some(regexList.toList)
      }).getOrElse(None)

    val whiteListMatch: Boolean = whiteListURIs.exists { uriList =>
      uriList exists { pattern =>
        logger.debug(s"checking ${request.getRequestURI} against $pattern")
        request.getRequestURI.matches(pattern)
      }
    }

    val authTokenValue = Option(request.getHeader(CommonHttpHeader.AUTH_TOKEN))

    //Get the authenticating token!
    val result: KeystoneV2Result = if (whiteListMatch) {
      Pass(Map.empty[String, String])
    } else {
      authTokenValue map { authToken =>
        //This block of code tries to get the token from the datastore, and provides it from real calls, if it isn't
        val tokenValidationResult: Try[AuthResult] =
          Option(datastore.get(authToken).asInstanceOf[AuthResult]).map { validationResult =>
            Success(validationResult)
          } getOrElse {
            //flatMap to unbox the Try[Try[TokenValidationResult]] so all Failure's are just packaged along
            getAdminToken.flatMap { adminToken =>
              validateToken(adminToken, authToken).recoverWith {
                //Recover if the exception is an AdminTokenUnauthorizedException
                //This way we can specify however we want to what we want to do to retry.
                //Also it only retries ONCE! No loops or anything. Fails gloriously
                case unauth: AdminTokenUnauthorizedException =>
                  //Clear the cache, call this method again
                  datastore.remove(ADMIN_TOKEN_KEY)
                  getAdminToken match {
                    case Success(newAdminToken) =>
                      validateToken(newAdminToken, authToken)
                    case Failure(x) => Failure(IdentityAdminTokenException("Unable to reacquire admin token", x))
                  }
              }
            }
          }

        tokenValidationResult match {
          case Success(InvalidToken) =>
            Reject(SC_UNAUTHORIZED)
          case Success(validToken: ValidToken) =>
            //TODO: should cache this here, at the final point? Can't cache it all, because differing timeouts :|
            val authorizedTenant = tenantAuthorization(extractTenant(request.getRequestURI), validToken) match {
              case Some(Success(tenantHeaderValues)) =>
                val tenantHeaders = Map(OpenStackServiceHeader.TENANT_ID.toString -> tenantHeaderValues.mkString(","))
                Pass(tenantHeaders)
              case Some(Failure(e)) =>
                Reject(SC_UNAUTHORIZED, failure = Some(e))
              case None =>
                Pass(Map.empty[String, String])
            }

            val authorizedEndpoints = authorizedTenant match {
              case Pass(headers) =>
                endpointAuthorization(authToken, validToken) match {
                  case Some(Success(endpointVector)) =>
                    //If I'm configured to put the endpoints into a x-catalog do it
                    //Do more things in here
                    val rolesHeader = Map(OpenStackServiceHeader.ROLES.toString -> validToken.roles.mkString(",")) //todo: not the place for this
                    Pass(rolesHeader ++ headers)
                  case Some(Failure(x)) =>
                    //Reject them with 403
                    Reject(SC_FORBIDDEN, failure = Some(x))
                  case None =>
                    //Do more things in here
                    val rolesHeader = Map(OpenStackServiceHeader.ROLES.toString -> validToken.roles.mkString(",")) //todo: not the place for this
                    Pass(rolesHeader ++ headers)
                }
              case reject: Reject => reject
            }

            val userGroups = authorizedEndpoints match {
              case Pass(headers) =>
                getGroups(authToken, validToken) match {
                  case Some(Success(groups)) =>
                    val groupsHeader = Map(PowerApiHeader.GROUPS.toString -> groups.mkString(",")) //todo: not the place for this
                    Pass(headers ++ groupsHeader)
                  case Some(Failure(e)) =>
                    logger.error(s"Could not get groups: ${e.getMessage}")
                    Pass(headers)
                  case None => Pass(headers)
                }
              case reject: Reject => reject
            }

            userGroups
          case Failure(x: IdentityAdminTokenException) =>
            Reject(SC_INTERNAL_SERVER_ERROR, failure = Some(x))
          case Failure(x: IdentityCommuncationException) =>
            Reject(SC_BAD_GATEWAY, failure = Some(x))
          case Failure(x) =>
            //TODO: this isn't yet complete
            Reject(SC_INTERNAL_SERVER_ERROR, failure = Some(x))
        }
      } getOrElse {
        //Don't have an auth token to validate
        Reject(SC_FORBIDDEN, Some("Auth token not found in headers"))
      }
    }

    //Handle the result of the filter to apply to the
    result match {
      case rejection: Reject =>
        val message: Option[String] = rejection match {
          case Reject(_, Some(x), _) => Some(x)
          case Reject(code, None, Some(failure)) =>
            logger.debug(s"Rejecting with status $code", failure)
            Some(failure.getMessage)
          case _ => None
        }
        message match {
          case Some(m) =>
            logger.debug(s"Rejection message: $m")
            response.sendError(rejection.status, m)
          case None => response.sendError(rejection.status)
        }

      case p: Pass =>
        //If the token validation passed and we're configured to do more things, we can do additional authorizations
        //Modify the request to add stuff
        p.headersToAdd.foreach { case (k, v) =>
          request.addHeader(k, v)
        }
        chain.doFilter(request, response)
    }
  }

  final def validateToken(authenticatingToken: String, token: String): Try[AuthResult] = {
    /**
     * Extract the user's information from the validate token response
     * @param inputStream the validate token response!
     * @return a success or failure of ValidToken information
     */
    def extractUserInformation(inputStream: InputStream): Try[ValidToken] = {
      import play.api.libs.json.Reads._
      import play.api.libs.json._

      val input: String = Source.fromInputStream(inputStream).getLines mkString ""
      try {
        val json = Json.parse(input)
        //Have to convert it to a vector, because List isn't serializeable in 2.10
        val roleNames: Seq[String] = (json \ "access" \ "user" \ "roles" \\ "name").map(_.as[String]).toVector
        val defaultTenantId: String = (json \ "access" \ "token" \ "tenant" \ "id").as[String]
        val tenantIds: Seq[String] = (json \ "access" \ "user" \ "roles" \\ "tenantId").map(_.as[String]).toVector
        val validToken = ValidToken(defaultTenantId, tenantIds, roleNames)
        //TODO: if I cache this here, I don't know if I'll get endpoints :|
        datastore.put(token, validToken, configuration.getCacheSettings.getTimeouts.getToken, TimeUnit.SECONDS)
        Success(validToken)
      } catch {
        case oops@(_: JsResultException | _: JsonProcessingException) =>
          Failure(IdentityCommuncationException("Unable to parse JSON from identity validate token response", oops))
      }
    }

    //TODO: pass in the configuration as well, because state change
    val identityEndpoint = configuration.getIdentityService.getUri

    import scala.collection.JavaConverters._
    Try(akkaServiceClient.get(s"$TOKEN_KEY_PREFIX$token",
      s"$identityEndpoint$TOKEN_ENDPOINT/$token",
      Map(CommonHttpHeader.AUTH_TOKEN.toString -> authenticatingToken).asJava)
    ) match {
      case Success(serviceClientResponse) =>
        //DEAL WITH IT
        //Parse the response for validating a token?
        logger.debug(s"SERVICE CLIENT RESPONSE: ${serviceClientResponse.getStatus}")
        logger.debug(s"Admin Token: $authenticatingToken")
        serviceClientResponse.getStatus match {
          case SC_OK | SC_NON_AUTHORITATIVE_INFORMATION =>
            //Extract the roles from the JSON and stick it in the ValidToken result
            extractUserInformation(serviceClientResponse.getData)
          case SC_BAD_REQUEST => Failure(IdentityValidationException("Bad Token Validation request to identity!"))
          case SC_UNAUTHORIZED => Failure(AdminTokenUnauthorizedException("Unable to validate token, authenticating token unauthorized"))
          case SC_FORBIDDEN => Failure(IdentityAdminTokenException("Admin token unauthorized to validate token"))
          case SC_NOT_FOUND =>
            datastore.put(token, InvalidToken, configuration.getCacheSettings.getTimeouts.getToken, TimeUnit.SECONDS)
            Success(InvalidToken)
          case SC_SERVICE_UNAVAILABLE => Failure(IdentityValidationException("Identity Service not available to authenticate token"))
          case _ => Failure(IdentityCommuncationException("Unhandled response from Identity, unable to continue"))
        }
      case Failure(x) => Failure(IdentityCommuncationException("Unable to successfully validate token with Identity", x))
    }
  }

  /**
   * Check the cache, or call to identity to get the admin token
   * @return Returns a Successful token, or a Failure
   */
  def getAdminToken: Try[String] = {
    //Check the cache first, then try the request
    Option(datastore.get(ADMIN_TOKEN_KEY)).map { value =>
      Success(value.asInstanceOf[String])
    } getOrElse {
      //authenticate, or get the admin token
      val identityEndpoint = configuration.getIdentityService.getUri

      import play.api.libs.json._
      val adminUsername = configuration.getIdentityService.getUsername
      val adminPassword = configuration.getIdentityService.getPassword

      val authenticationPayload = Json.obj(
        "auth" -> Json.obj(
          "passwordCredentials" -> Json.obj(
            "username" -> adminUsername,
            "password" -> adminPassword
          )
        )
      )

      import scala.collection.JavaConversions._
      val akkaResponse = Try(akkaServiceClient.post(ADMIN_TOKEN_KEY,
        identityEndpoint, //TODO: wrong URL
        Map.empty[String, String],
        Json.stringify(authenticationPayload),
        MediaType.APPLICATION_JSON_TYPE
      ))

      akkaResponse match {
        case Success(x) =>
          val jsonResponse = Source.fromInputStream(x.getData).getLines().mkString("")
          val json = Json.parse(jsonResponse)
          Try(Success((json \ "access" \ "token" \ "id").as[String])) match {
            case Success(s) =>
              datastore.put(ADMIN_TOKEN_KEY, s.get, configuration.getCacheSettings.getTimeouts.getToken, TimeUnit.SECONDS)
              s
            case Failure(f) => Failure(IdentityCommuncationException("Token not found in identity response during Admin Authentication", f))
          }
        case Failure(x) => Failure(IdentityCommuncationException("Failure communicating with identity during Admin Authentication", x))
      }
    }
  }

  def extractTenant(requestUri: String): Option[String] = {
    val maybeTenant = for {
      tenantHandling <- Option(configuration.getTenantHandling)
      validateTenant <- Option(tenantHandling.getValidateTenant)
      uriExtractionRegex <- Option(validateTenant.getUriExtractionRegex)
    } yield {
        val regex = uriExtractionRegex.r
        requestUri match {
          case regex(tenantId, _*) => Option(tenantId)
          case _ => Option.empty[String]
        }
      }
    maybeTenant.flatten
  }

  def tenantAuthorization(expectedTenant: Option[String], validToken: ValidToken): Option[Try[Vector[String]]] = {
    def buildTenantVector(defaultTenant: String, roleTenants: Seq[String], uriTenant: Option[String]): Vector[String] = {
      val sendAllTenants = configuration.getTenantHandling.isSendAllTenantIds
      val sendTenantIdQuality = Option(configuration.getTenantHandling.getSendTenantIdQuality)
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

    Option(configuration.getTenantHandling) map { tenantHandling =>
      Option(configuration.getTenantHandling.getValidateTenant) map { validateTenant =>
        Option(configuration.getTenantHandling.getValidateTenant.getBypassValidationRoles) map {
          _.getRole.asScala.intersect(validToken.roles).nonEmpty
        } match {
          case Some(true) => null //todo: don't use null here
          case _ =>
            Try[String] {
              expectedTenant match {
                case Some(reqTenant) =>
                  val tokenTenants = Set(validToken.defaultTenantId) ++ validToken.tenantIds
                  tokenTenants.find(reqTenant.equals) match {
                    case Some(uriTenant) => uriTenant
                    case None => throw InvalidTenantException("Tenant from URI does not match any of the tenants associated with the provided token")
                  }
                case None => throw UnparseableTenantException("Could not parse tenant from the URI")
              }
            }
        }
      } match {
        case Some(Failure(e)) => Failure(e)
        case Some(Success(uriTenant)) => Success(buildTenantVector(validToken.defaultTenantId, validToken.tenantIds, Some(uriTenant)))
        case _ => Success(buildTenantVector(validToken.defaultTenantId, validToken.tenantIds, None))
      }
    }
  }

  case class Endpoint(region: Option[String], name: Option[String], endpointType: Option[String], publicURL: String) {
    /**
     * Determines whether or not this endpoint meets the requirements set forth by the values contained in
     * endpointRequirement for the purpose of authorization.
     *
     * @param endpointRequirement an endpoint containing fields with required values
     * @return true if this endpoint has field values matching those in the endpointRequirement, false otherwise
     */
    def meetsRequirement(endpointRequirement: Endpoint) = {
      def compare(available: Option[String], required: Option[String]) = (available, required) match {
        case (Some(x), Some(y)) => x == y
        case (None, Some(_)) => false
        case _ => true
      }

      this.publicURL == endpointRequirement.publicURL &&
        compare(this.region, endpointRequirement.region) &&
        compare(this.name, endpointRequirement.name) &&
        compare(this.endpointType, endpointRequirement.endpointType)
    }
  }

  final def getEndpointsForToken(authenticatingToken: String, forToken: String): Try[Vector[Endpoint]] = {
    val identityEndpoint = configuration.getIdentityService.getUri
    /**
     * Extract the user's endpoints from the endpoints call
     * @param inputStream the Identity Endpoints call body
     * @return a success or failure of a Vector[Endpoint] information
     */
    def extractEndpointInfo(inputStream: InputStream): Try[Vector[Endpoint]] = {
      import play.api.libs.functional.syntax._
      import play.api.libs.json.Reads._
      import play.api.libs.json._

      implicit val endpointsReader = (
        (JsPath \ "region").readNullable[String] and
          (JsPath \ "name").readNullable[String] and
          (JsPath \ "type").readNullable[String] and
          (JsPath \ "publicURL").read[String]
        )(Endpoint.apply _)

      val input: String = Source.fromInputStream(inputStream).getLines mkString ""
      val json = Json.parse(input)
      //Have to convert it to a vector, because List isn't serializeable in 2.10
      (json \ "endpoints").validate[Vector[Endpoint]] match {
        case s: JsSuccess[Vector[Endpoint]] => Success(s.get)
        case f: JsError =>
          Failure(new IdentityCommuncationException("Identity didn't respond with proper Endpoints JSON"))
      }
    }

    Try(akkaServiceClient.get(s"$ENDPOINTS_KEY_PREFIX$forToken",
      s"$identityEndpoint${ENDPOINTS_ENDPOINT(forToken)}",
      Map(CommonHttpHeader.AUTH_TOKEN.toString -> authenticatingToken).asJava)) match {
      case Success(serviceClientResponse) =>
        serviceClientResponse.getStatus match {
          case SC_OK | SC_NON_AUTHORITATIVE_INFORMATION => extractEndpointInfo(serviceClientResponse.getData)
          case SC_UNAUTHORIZED => Failure(AdminTokenUnauthorizedException("Admin token unauthorized"))
          case SC_FORBIDDEN => Failure(IdentityAdminTokenException("Admin token forbidden from accessing endpoints"))
          case _ => Failure(new Exception("Unexpected response code from the endpoints call"))
        }
      case Failure(x) => Failure(x)
    }
  }

  /**
   * Get the user's endpoints, and validate that the configured restrictions match
   * Also handles caching of the result
   * @param authToken Assuming the auth token has been validated already
   * @return
   */
  def endpointAuthorization(authToken: String, validToken: ValidToken): Option[Try[Vector[Endpoint]]] = {
    Option(configuration.getRequireServiceEndpoint) map { requireServiceEndpoint =>
      Option(datastore.get(s"$ENDPOINTS_KEY_PREFIX$authToken").asInstanceOf[Vector[Endpoint]]).map { endpoints =>
        Success(endpoints)
      } getOrElse {
        getAdminToken flatMap { adminToken =>
          getEndpointsForToken(adminToken, authToken) recoverWith {
            case unauth: AdminTokenUnauthorizedException =>
              //Clear the cache, call this method again
              datastore.remove(ADMIN_TOKEN_KEY)
              getAdminToken match {
                case Success(newAdminToken) =>
                  getEndpointsForToken(newAdminToken, authToken)
                case Failure(x) => Failure(IdentityAdminTokenException("Unable to reacquire admin token", x))
              }
          }
        }
      } flatMap { endpointList =>
        //Create the endpoint requirement from teh configuration
        def convertServiceType(cfg: ServiceEndpointType): Endpoint = {
          Endpoint(
            publicURL = cfg.getPublicUrl,
            name = Option(cfg.getName),
            endpointType = Option(cfg.getType),
            region = Option(cfg.getRegion)
          )
        }
        //Have to see if they have a list of roles...
        //Have to use slightly more annoying parenthesis to make sure the for-comprehension does what I want
        val bypassRoles: List[String] = (for {
          jaxbIntermediaryObject <- Option(requireServiceEndpoint.getBypassValidationRoles)
          rolesList <- Option(jaxbIntermediaryObject.getRole)
        } yield {
            import scala.collection.JavaConversions._
            rolesList.toList
          }) getOrElse {
          List.empty[String]
        }

        if (bypassRoles.intersect(validToken.roles).nonEmpty ||
          endpointList.exists(endpoint => endpoint.meetsRequirement(convertServiceType(requireServiceEndpoint)))) {
          Success(endpointList)
        } else {
          Failure(new UnauthorizedEndpointException("User did not have the required endpoint"))
        }
      }
    }
  }

  final def getGroupsForToken(authenticatingToken: String, forToken: String): Try[Vector[String]] = {
    val identityEndpoint = configuration.getIdentityService.getUri

    /**
     * Extract the user's groups from the groups call
     * @param inputStream the Identity Groups call body
     * @return a success or failure of a Vector[String] information
     */
    def extractGroupInfo(inputStream: InputStream): Try[Vector[String]] = {
      import play.api.libs.json._

      Try {
        val input: String = Source.fromInputStream(inputStream).getLines mkString ""
        val json = Json.parse(input)

        (json \ "RAX-KSGRP:groups" \\ "id").map(_.as[String]).toVector
      }
    }

    Try(akkaServiceClient.get(s"$GROUPS_KEY_PREFIX$forToken",
      s"$identityEndpoint${GROUPS_ENDPOINT(forToken)}",
      Map(CommonHttpHeader.AUTH_TOKEN.toString -> authenticatingToken).asJava)) match {
      case Success(serviceClientResponse) =>
        serviceClientResponse.getStatus match {
          case SC_OK => extractGroupInfo(serviceClientResponse.getData)
          case SC_UNAUTHORIZED => Failure(AdminTokenUnauthorizedException("Admin token unauthorized"))
          case SC_FORBIDDEN => Failure(IdentityAdminTokenException("Admin token forbidden from accessing groups"))
          case SC_REQUEST_ENTITY_TOO_LARGE | SC_TOO_MANY_REQUESTS => Failure(OverLimitException("Rate limited when accessing groups"))
          case _ => Failure(new Exception("Unexpected response code from the groups call"))
        }
      case Failure(x) => Failure(x)
    }
  }

  def getGroups(authToken: String, validToken: ValidToken): Option[Try[Vector[String]]] = {
    Option(configuration.getIdentityService.isSetGroupsInHeader).filter(identity) map { sendGroups =>
      Option(datastore.get(s"$GROUPS_KEY_PREFIX$authToken").asInstanceOf[Vector[String]]) map { groups =>
        Success(groups)
      } getOrElse {
        getAdminToken flatMap { adminToken =>
          getGroupsForToken(adminToken, authToken) recoverWith {
            case unauth: AdminTokenUnauthorizedException =>
              //Clear the cache, call this method again
              datastore.remove(ADMIN_TOKEN_KEY)
              getAdminToken match {
                case Success(newAdminToken) =>
                  getGroupsForToken(newAdminToken, authToken)
                case Failure(e) => Failure(IdentityAdminTokenException("Unable to reacquire admin token", e))
              }
          }
        }
      }
    }
  }

  override def configurationUpdated(configurationObject: KeystoneV2Config): Unit = {
    def fixMyDefaults(stupidConfig: KeystoneV2Config): KeystoneV2Config = {
      // LOLJAXB  	(╯°□°）╯︵ ┻━┻
      //This relies on the Default Settings plugin and the fluent_api plugin added to the Jaxb code generation plugin
      // I'm sorry
      if (stupidConfig.getCacheSettings == null) {
        stupidConfig.withCacheSettings(new CacheSettingsType().withTimeouts(new CacheTimeoutsType()))
      } else if (stupidConfig.getCacheSettings.getTimeouts == null) {
        stupidConfig.getCacheSettings.withTimeouts(new CacheTimeoutsType())
        stupidConfig
      } else {
        stupidConfig
      }
    }

    configuration = fixMyDefaults(configurationObject)
    initialized = true
  }

  override def isInitialized: Boolean = initialized

  trait IdentityException

  case class IdentityAdminTokenException(message: String, cause: Throwable = null) extends Exception(message, cause) with IdentityException

  case class AdminTokenUnauthorizedException(message: String, cause: Throwable = null) extends Exception(message, cause) with IdentityException

  case class IdentityValidationException(message: String, cause: Throwable = null) extends Exception(message, cause) with IdentityException

  case class IdentityCommuncationException(message: String, cause: Throwable = null) extends Exception(message, cause) with IdentityException

  case class UnauthorizedEndpointException(message: String, cause: Throwable = null) extends Exception(message, cause) with IdentityException

  case class OverLimitException(message: String, cause: Throwable = null) extends Exception(message, cause) with IdentityException

  trait TenantException

  case class InvalidTenantException(message: String, cause: Throwable = null) extends Exception(message, cause) with TenantException

  case class UnparseableTenantException(message: String, cause: Throwable = null) extends Exception(message, cause) with TenantException

  sealed trait KeystoneV2Result

  case class Pass(headersToAdd: Map[String, String]) extends KeystoneV2Result

  case class Reject(status: Int, message: Option[String] = None, failure: Option[Throwable] = None) extends KeystoneV2Result

  sealed trait AuthResult

  case class ValidToken(defaultTenantId: String, tenantIds: Seq[String], roles: Seq[String]) extends AuthResult

  case object InvalidToken extends AuthResult

}
