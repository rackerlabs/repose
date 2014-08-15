package com.rackspace.papi.components.keystone.v3

import javax.servlet.http.HttpServletRequest
import javax.ws.rs.core.{HttpHeaders, MediaType}

import com.rackspace.papi.commons.util.http.{ServiceClientResponse, HttpStatusCode}
import com.rackspace.papi.commons.util.servlet.http.ReadableHttpServletResponse
import com.rackspace.papi.components.keystone.v3.config.KeystoneV3Config
import com.rackspace.papi.components.keystone.v3.json.spray.IdentityJsonProtocol._
import com.rackspace.papi.components.keystone.v3.objects._
import com.rackspace.papi.components.keystone.v3.utilities.{InvalidSubjectTokenException, InvalidAdminCredentialsException, KeystoneServiceException}
import com.rackspace.papi.filter.logic.common.AbstractFilterLogicHandler
import com.rackspace.papi.filter.logic.impl.FilterDirectorImpl
import com.rackspace.papi.filter.logic.{FilterAction, FilterDirector}
import com.rackspace.papi.service.datastore.DatastoreService
import com.rackspace.papi.service.httpclient.HttpClientService
import com.rackspace.papi.service.serviceclient.akka.AkkaServiceClient
import org.apache.http.Header
import org.slf4j.LoggerFactory
import spray.json._

import scala.collection.JavaConverters._
import scala.io.Source
import scala.util.{Try, Failure, Success}

class KeystoneV3Handler(keystoneConfig: KeystoneV3Config, akkaServiceClient: AkkaServiceClient, datastoreService: DatastoreService, connectionPoolService: HttpClientService[_, _])
        extends AbstractFilterLogicHandler {

    private final val LOG = LoggerFactory.getLogger(classOf[KeystoneV3Handler])
    private final val ADMIN_TOKEN_KEY = "ADMIN_TOKEN"
    private final val TOKEN_KEY_PREFIX = "TOKEN:"
    private final val TOKEN_ENDPOINT = "/v3/auth/tokens"
    private final val X_AUTH_TOKEN_HEADER = "X-Auth-Token"
    private final val X_SUBJECT_TOKEN_HEADER = "X-Subject-Token"

    // TODO: stop being lazy!
    private lazy val keystoneServiceUri = keystoneConfig.getKeystoneService.getUri

    override def handleRequest(request: HttpServletRequest, response: ReadableHttpServletResponse): FilterDirector = {
        if (isUriWhitelisted(request.getRequestURI, keystoneConfig.getWhiteList.getUriPattern.asScala.toList)) {
            LOG.debug("Request URI matches a configured whitelist pattern! Allowing request to pass through.")
            val filterDirector: FilterDirector = new FilterDirectorImpl()
            filterDirector.setFilterAction(FilterAction.PASS)
            filterDirector
        } else {
            authenticate(request)
        }
    }

    private def isUriWhitelisted(requestUri: String, whiteList: List[String]) =
        whiteList.filter(requestUri.matches).nonEmpty

    private def authenticate(request: HttpServletRequest) = {
        val filterDirector: FilterDirector = new FilterDirectorImpl()
        filterDirector.setFilterAction(FilterAction.RETURN)
        filterDirector.setResponseStatus(HttpStatusCode.UNAUTHORIZED)
        val subjectToken = request.getHeader(X_SUBJECT_TOKEN_HEADER)

        if (subjectToken == null) {
            filterDirector.setResponseStatus(HttpStatusCode.UNAUTHORIZED)
            filterDirector.setFilterAction(FilterAction.RETURN)
        } else {
//            val tokenObject = validateSubjectToken(subjectToken) match {
//                case Success(v) => v
//                case Failure(_) => None
//            }
            // TODO
        }

        filterDirector
    }

    private def validateSubjectToken(subjectToken: String) = {
        def getTokenFromResponse(response: ServiceClientResponse) = {
            response.getData.reset() // TODO: Why is this necessary?
            val responseJson = Source.fromInputStream(response.getData).mkString
            val responseObject = responseJson.parseJson.convertTo[AuthResponse]
            responseObject.token
        }

        // TODO: Check cache for user token
        //        datastoreService.getDefaultDatastore

        val fetchedAdminToken = fetchAdminToken()

        fetchedAdminToken match {
            case Success(adminToken) =>
                // TODO: Extract this logic and the request logic to its own method to allow reuse on force fetchAdminToken
                val headerMap = Map(X_AUTH_TOKEN_HEADER -> adminToken, X_SUBJECT_TOKEN_HEADER -> subjectToken, HttpHeaders.ACCEPT -> MediaType.APPLICATION_JSON)
                val serviceClientResponse = akkaServiceClient.get(TOKEN_KEY_PREFIX + subjectToken, keystoneServiceUri + TOKEN_ENDPOINT, headerMap.asJava)

                HttpStatusCode.fromInt(serviceClientResponse.getStatusCode) match {
                    case HttpStatusCode.OK =>
                        Success(getTokenFromResponse(serviceClientResponse))
                    case HttpStatusCode.NOT_FOUND =>
                        LOG.error("Subject token validation failed. Response Code: 404")
                        Failure(new InvalidSubjectTokenException("Failed to validate subject token"))
                    case HttpStatusCode.UNAUTHORIZED =>
                        LOG.error("Request made with an expired admin token. Fetching a fresh admin token and retrying token validation. Response Code: 401")
                        // TODO: Implement this after caching is implemented
                        Failure(new NotImplementedError())
                    case _ =>
                        LOG.error("Keystone service returned an unexpected response status code. Response Code: " + serviceClientResponse.getStatusCode)
                        Failure(new KeystoneServiceException("Failed to validate subject token"))
                }
            case Failure(e) => fetchedAdminToken
        }
    }

    private def fetchAdminToken() = {
        def createAdminAuthRequest = {
            val username = keystoneConfig.getKeystoneService.getUsername
            val password = keystoneConfig.getKeystoneService.getPassword
            val domainId = Option(keystoneConfig.getKeystoneService.getDomainId)
            var domainType: Option[DomainType] = None

            if (domainId.isDefined) domainType = {
                Some(DomainType(id = domainId))
            }

            AuthRequestRoot(
                AuthRequest(
                    AuthIdentityRequest(
                        methods = List("password"),
                        password = Some(PasswordCredentials(
                            UserNamePasswordRequest(
                                domain = domainType,
                                name = Some(username),
                                password = password
                            )
                        ))
                    )
                )
            ).toJson.compactPrint
        }

        // TODO: Check cache (datastore)
        //        datastoreService.getDefaultDatastore.get(CACHE_KEY)

        val generateAuthTokenResponse = akkaServiceClient.post(ADMIN_TOKEN_KEY, keystoneServiceUri + TOKEN_ENDPOINT, Map[String, String]().asJava, createAdminAuthRequest, MediaType.APPLICATION_JSON_TYPE, MediaType.APPLICATION_JSON_TYPE)
        HttpStatusCode.fromInt(generateAuthTokenResponse.getStatusCode) match {
            // Since the operation is a POST, a 201 should be returned if the operation was successful
            case HttpStatusCode.CREATED =>
                //                val expirationTtl = // TODO: Parse the expires-at field from the response
                // TODO: datastoreService.getDefaultDatastore.put(CACHE_KEY, "", expirationTime - System.currentTimeMillis())
                Success(generateAuthTokenResponse.getHeaders.filter((header: Header) => header.getName.equalsIgnoreCase(X_SUBJECT_TOKEN_HEADER)).head.getValue)
            case _ =>
                LOG.error("Unable to get admin token. Please verify your admin credentials. Response Code: " + generateAuthTokenResponse.getStatusCode)
                Failure(new InvalidAdminCredentialsException("Failed to fetch admin token"))
        }
    }

    private def writeProjectHeader(projectFromUri: String, roles: List[Role], writeAll: Boolean, request: HttpServletRequest) = {}

    private def containsEndpoint(endpoints: List[EndpointType], url: String): Boolean = endpoints.exists { endpoint: EndpointType => endpoint.url == url}

    private def hasIgnoreEnabledRole(ignoreProjectRoles: List[String], userRoles: List[Role]): Boolean = true

    private def matchesProject(projectFromUri: String, roles: List[Role]): Boolean = true
}
