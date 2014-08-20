package com.rackspace.papi.components.keystone.v3

import java.util.concurrent.TimeUnit
import javax.servlet.http.HttpServletRequest
import javax.ws.rs.core.{HttpHeaders, MediaType}

import com.rackspace.papi.commons.util.http.{HttpStatusCode, ServiceClientResponse}
import com.rackspace.papi.commons.util.servlet.http.ReadableHttpServletResponse
import com.rackspace.papi.components.keystone.v3.config.KeystoneV3Config
import com.rackspace.papi.components.keystone.v3.json.spray.IdentityJsonProtocol._
import com.rackspace.papi.components.keystone.v3.objects._
import com.rackspace.papi.components.keystone.v3.utilities._
import com.rackspace.papi.filter.logic.common.AbstractFilterLogicHandler
import com.rackspace.papi.filter.logic.impl.FilterDirectorImpl
import com.rackspace.papi.filter.logic.{FilterAction, FilterDirector}
import com.rackspace.papi.service.datastore.DatastoreService
import com.rackspace.papi.service.serviceclient.akka.AkkaServiceClient
import org.apache.http.Header
import org.joda.time.DateTime
import org.slf4j.LoggerFactory
import spray.json._

import scala.collection.JavaConverters._
import scala.io.Source
import scala.util.{Failure, Success}

class KeystoneV3Handler(keystoneConfig: KeystoneV3Config, akkaServiceClient: AkkaServiceClient, datastoreService: DatastoreService)
        extends AbstractFilterLogicHandler {

    private final val LOG = LoggerFactory.getLogger(classOf[KeystoneV3Handler])
    private final val ADMIN_TOKEN_KEY = "ADMIN_TOKEN"
    private final val TOKEN_KEY_PREFIX = "TOKEN:"

    private lazy val keystoneServiceUri = keystoneConfig.getKeystoneService.getUri
    private lazy val datastore = datastoreService.getDefaultDatastore

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

    private def authenticate(request: HttpServletRequest) = {
        val filterDirector: FilterDirector = new FilterDirectorImpl()
        filterDirector.setFilterAction(FilterAction.RETURN)
        filterDirector.setResponseStatus(HttpStatusCode.UNAUTHORIZED)
        val subjectToken = request.getHeader(KeystoneV3Headers.X_SUBJECT_TOKEN)

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
        Option(datastore.get(TOKEN_KEY_PREFIX + subjectToken)) match {
            case Some(cachedSubjectTokenObject) =>
                Success(cachedSubjectTokenObject.asInstanceOf[AuthenticateResponse])
            case None =>
                val fetchedAdminToken = fetchAdminToken()

                fetchedAdminToken match {
                    case Success(adminToken) =>
                        // TODO: Extract this logic and the request logic to its own method to allow reuse on force fetchAdminToken
                        val headerMap = Map(
                            KeystoneV3Headers.X_AUTH_TOKEN -> adminToken,
                            KeystoneV3Headers.X_SUBJECT_TOKEN -> subjectToken,
                            HttpHeaders.ACCEPT -> MediaType.APPLICATION_JSON
                        )
                        val validateTokenResponse = akkaServiceClient.get(TOKEN_KEY_PREFIX + subjectToken, keystoneServiceUri + KeystoneV3Endpoints.TOKEN, headerMap.asJava)

                        HttpStatusCode.fromInt(validateTokenResponse.getStatusCode) match {
                            case HttpStatusCode.OK =>
                                val subjectTokenObject = readToAuthResponseObject(validateTokenResponse).token

                                val expiration = new DateTime(subjectTokenObject.expires_at)
                                val ttl = safeLongToInt(expiration.getMillis - DateTime.now.getMillis)
                                LOG.debug("Caching token " + subjectToken + " with TTL set to: " + ttl + "ms")
                                datastore.put(TOKEN_KEY_PREFIX + subjectToken, subjectTokenObject, ttl, TimeUnit.MILLISECONDS)

                                Success(subjectTokenObject)
                            case HttpStatusCode.NOT_FOUND =>
                                LOG.error("Subject token validation failed. Response Code: 404")
                                Failure(new InvalidSubjectTokenException("Failed to validate subject token"))
                            case HttpStatusCode.UNAUTHORIZED =>
                                LOG.error("Request made with an expired admin token. Fetching a fresh admin token and retrying token validation. Response Code: 401")
                                // TODO: Implement this after caching is implemented
                                Failure(???)
                            case _ =>
                                LOG.error("Keystone service returned an unexpected response status code. Response Code: " + validateTokenResponse.getStatusCode)
                                Failure(new KeystoneServiceException("Failed to validate subject token"))
                        }
                    case Failure(e) => fetchedAdminToken
                }
        }
    }

    private def fetchAdminToken() = {
        val createAdminAuthRequest = () => {
            val username = keystoneConfig.getKeystoneService.getUsername
            val password = keystoneConfig.getKeystoneService.getPassword

            val projectScope = Option(keystoneConfig.getKeystoneService.getProjectId) match {
                case Some(projectId) => Some(Scope(project = Some(ProjectScope(id = projectId))))
                case _ => None
            }

            AuthRequestRoot(
                AuthRequest(
                    AuthIdentityRequest(
                        methods = List("password"),
                        password = Some(PasswordCredentials(
                            UserNamePasswordRequest(
                                scope = projectScope,
                                name = Some(username),
                                password = password
                            )
                        ))
                    )
                )
            ).toJson.compactPrint
        }

        // Check the cache for the admin token. If present, return it. Validity of the token is handled in validateSubjectToken and by the TTL.
        Option(datastore.get(ADMIN_TOKEN_KEY)) match {
            case Some(cachedAdminToken) =>
                Success(cachedAdminToken.asInstanceOf[String])
            case None =>
                val generateAuthTokenResponse = akkaServiceClient.post(ADMIN_TOKEN_KEY, keystoneServiceUri + KeystoneV3Endpoints.TOKEN, Map[String, String]().asJava, createAdminAuthRequest(), MediaType.APPLICATION_JSON_TYPE, MediaType.APPLICATION_JSON_TYPE)
                HttpStatusCode.fromInt(generateAuthTokenResponse.getStatusCode) match {
                    // Since the operation is a POST, a 201 should be returned if the operation was successful
                    case HttpStatusCode.CREATED =>
                        val adminToken = generateAuthTokenResponse.getHeaders.filter((header: Header) => header.getName.equalsIgnoreCase(KeystoneV3Headers.X_SUBJECT_TOKEN)).head.getValue

                        val expiration = new DateTime(readToAuthResponseObject(generateAuthTokenResponse).token.expires_at)
                        val ttl = safeLongToInt(expiration.getMillis - DateTime.now.getMillis)
                        LOG.debug("Caching admin token with TTL set to: " + ttl + "ms")
                        datastore.put(ADMIN_TOKEN_KEY, adminToken, ttl, TimeUnit.MILLISECONDS)

                        Success(adminToken)
                    case _ =>
                        LOG.error("Unable to get admin token. Please verify your admin credentials. Response Code: " + generateAuthTokenResponse.getStatusCode)
                        Failure(new InvalidAdminCredentialsException("Failed to fetch admin token"))
                }
        }
    }

    private def readToAuthResponseObject(response: ServiceClientResponse) = {
        response.getData.reset() // TODO: Remove this when we can. It relies on our implementatinon returning an InputStream that supports reset.
        val responseJson = Source.fromInputStream(response.getData).mkString
        responseJson.parseJson.convertTo[AuthResponse]
    }

    private def writeProjectHeader(projectFromUri: String, roles: List[Role], writeAll: Boolean, filterDirector: FilterDirector) = {
        val projectsFromRoles: Set[String] = {
            if(writeAll) {
                roles.map({role => role.project_id.get}).toSet
            }
            else {
                Set.empty
            }
        }

        def projects: Set[String] = projectsFromRoles + projectFromUri

        filterDirector.requestHeaderManager().appendHeader("X-PROJECT-ID", projects.toArray:_*)
    }

    private def containsEndpoint(endpoints: List[EndpointType], url: String): Boolean = endpoints.exists { endpoint: EndpointType => endpoint.url == url}

    private def hasIgnoreEnabledRole(ignoreProjectRoles: List[String], userRoles: List[Role]): Boolean = true

    private def matchesProject(projectFromUri: String, roles: List[Role]): Boolean = true

    private val isUriWhitelisted = (requestUri: String, whiteList: List[String]) =>
        whiteList.filter(requestUri.matches).nonEmpty

    private val safeLongToInt = (l: Long) =>
        math.min(l, Int.MaxValue).toInt
}
