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
import org.openrepose.commons.config.manager.UpdateListener
import org.openrepose.commons.utils.http._
import org.openrepose.commons.utils.servlet.http.MutableHttpServletRequest
import org.openrepose.core.filter.FilterConfigHelper
import org.openrepose.core.services.config.ConfigurationService
import org.openrepose.core.services.datastore.{Datastore, DatastoreService}
import org.openrepose.core.services.serviceclient.akka.AkkaServiceClient
import org.openrepose.filters.keystonev2.RequestHandler._
import org.openrepose.filters.keystonev2.config._

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
  private final val X_AUTH_PROXY = "Proxy"

  private var configurationFile: String = DEFAULT_CONFIG
  private var initialized = false

  private val datastore: Datastore = datastoreService.getDefaultDatastore //Which happens to be the local datastore

  var configuration: KeystoneV2Config = _

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

  implicit val autoHeaderToString: HeaderConstant => String = hc => hc.toString

  override def doFilter(servletRequest: ServletRequest, servletResponse: ServletResponse, chain: FilterChain): Unit = {
    val request = MutableHttpServletRequest.wrap(servletRequest.asInstanceOf[HttpServletRequest])
    val response = servletResponse.asInstanceOf[HttpServletResponse] // Not using the mutable wrapper because it doesn't work properly at the moment, and we don't need to modify the response from further down the chain
    val requestHandler = new RequestHandler(configuration, akkaServiceClient, datastore)

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
          Option(datastore.get(s"$TOKEN_KEY_PREFIX$authToken").asInstanceOf[AuthResult]).map { validationResult =>
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
            val uriTenantOption = requestHandler.extractTenant(request.getRequestURI)
            val authorizedTenant = requestHandler.tenantAuthorization(uriTenantOption, validToken) match {
              case Some(Success(tenantHeaderValues)) =>
                Pass(Map(OpenStackServiceHeader.TENANT_ID.toString -> tenantHeaderValues.mkString(",")))
              case Some(Failure(e)) =>
                Reject(SC_UNAUTHORIZED, failure = Some(e))
              case None =>
                Pass(Map.empty[String, String])
            }

            val authorizedEndpoints = authorizedTenant match {
              case Pass(headers) =>
                requestHandler.endpointAuthorization(authToken, validToken) match {
                  case Some(Success(endpointsData)) =>
                    //If I'm configured to put the endpoints into a x-catalog do it
                    // todo: do this even if configured even if authorization is not
                    if (configuration.getIdentityService.isSetCatalogInHeader) {
                      val endpointsHeader = PowerApiHeader.X_CATALOG.toString -> Base64.encodeBase64String(endpointsData.endpointsJson.getBytes)
                      Pass(headers + endpointsHeader)
                    } else {
                      Pass(headers)
                    }
                  case Some(Failure(x)) =>
                    //Reject them with 403
                    Reject(SC_FORBIDDEN, failure = Some(x))
                  case None =>
                    //Do more things in here
                    Pass(headers)
                }
              case reject: Reject => reject
            }

            val userGroups = authorizedEndpoints match {
              case Pass(headers) =>
                requestHandler.getGroups(authToken, validToken) match {
                  case Some(Success(groups)) =>
                    // todo: only if configured to send groups
                    val groupsHeader = PowerApiHeader.GROUPS.toString -> groups.mkString(",")
                    Pass(headers + groupsHeader)
                  case Some(Failure(e)) =>
                    logger.error(s"Could not get groups: ${e.getMessage}")
                    Pass(headers)
                  case None => Pass(headers)
                }
              case reject: Reject => reject
            }

            val addHeaders = userGroups match {
              case Pass(headers) =>
                val userHeaders = Map(PowerApiHeader.USER.toString -> validToken.username,
                  OpenStackServiceHeader.USER_NAME.toString -> validToken.username,
                  OpenStackServiceHeader.USER_ID.toString -> validToken.userId)

                val xAuthHeader = uriTenantOption match {
                  case Some(uriTenant) =>
                    OpenStackServiceHeader.EXTENDED_AUTHORIZATION.toString -> s"$X_AUTH_PROXY $uriTenant"
                  case None =>
                    OpenStackServiceHeader.EXTENDED_AUTHORIZATION.toString -> X_AUTH_PROXY
                }

                // todo: only if configured to send roles
                val rolesHeader = OpenStackServiceHeader.ROLES.toString -> validToken.roles.mkString(",")

                val tenantName = OpenStackServiceHeader.TENANT_NAME.toString -> validToken.tenantName

                val defaultRegion = validToken.defaultRegion.map(dr => OpenStackServiceHeader.DEFAULT_REGION.toString -> dr)

                val contactId = validToken.contactId.map(ci => OpenStackServiceHeader.CONTACT_ID.toString -> ci)

                val expirationDate = OpenStackServiceHeader.X_EXPIRATION.toString -> validToken.expirationDate

                val impersonatorId = validToken.impersonatorId.map(iid => OpenStackServiceHeader.IMPERSONATOR_ID.toString -> iid)
                val impersonatorName = validToken.impersonatorName.map(in => OpenStackServiceHeader.IMPERSONATOR_NAME.toString -> in)

                //todo: after we implement delegation, we should be able to set IdentityStatus.Indeterminate
                val identityStatus = OpenStackServiceHeader.IDENTITY_STATUS.toString -> IdentityStatus.Confirmed.toString

                //todo: endpoints

                Pass(headers ++ userHeaders + rolesHeader + tenantName + xAuthHeader ++ defaultRegion ++ contactId
                  + expirationDate ++ impersonatorId ++ impersonatorName + identityStatus)
              case reject: Reject => reject
            }

            addHeaders
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
        //todo: delegation
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

    configuration = fixMyDefaults(configurationObject)
    initialized = true
  }

  override def isInitialized: Boolean = initialized
}
