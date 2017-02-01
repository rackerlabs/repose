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
package framework.mocks

import groovy.json.JsonBuilder
import groovy.text.SimpleTemplateEngine
import groovy.xml.MarkupBuilder
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import org.joda.time.format.DateTimeFormat
import org.rackspace.deproxy.Request
import org.rackspace.deproxy.Response

import javax.xml.transform.stream.StreamSource
import javax.xml.validation.Schema
import javax.xml.validation.SchemaFactory
import javax.xml.validation.Validator
import java.util.concurrent.atomic.AtomicInteger

import static javax.servlet.http.HttpServletResponse.*

/**
 * Created by jennyvo on 6/16/15.
 * Simulates responses from an Identity V2 Service
 */
class MockIdentityV2Service {
    static final String DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss'Z'"

    static final String PATH_REGEX_USER_GLOBAL_ROLES = '^/v2.0/users/([^/]+)/roles'
    static final String PATH_REGEX_GROUPS = '^/v2.0/users/([^/]+)/RAX-KSGRP'
    static final String PATH_REGEX_ENDPOINTS = '^/v2.0/tokens/([^/]+)/endpoints'
    static final String PATH_REGEX_VALIDATE_TOKEN = '^/v2.0/tokens/([^/]+)/?$'
    static final String PATH_REGEX_SAML_IDP_ISSUER = $/^/v2.0/RAX-AUTH/federation/identity-providers/?/$
    static final String PATH_REGEX_SAML_MAPPING = '^/v2.0/RAX-AUTH/federation/identity-providers/([^/]+)/mapping'

    static final String SAML_AUTH_BY_PASSWORD = "urn:oasis:names:tc:SAML:2.0:ac:classes:PasswordProtectedTransport"
    static final String SAML_AUTH_BY_RSAKEY = "urn:oasis:names:tc:SAML:2.0:ac:classes:TimeSyncToken"

    private AtomicInteger validateTokenCount = new AtomicInteger(0)
    private AtomicInteger getGroupsCount = new AtomicInteger(0)
    private AtomicInteger generateTokenCount = new AtomicInteger(0)
    private AtomicInteger getEndpointsCount = new AtomicInteger(0)
    private AtomicInteger getUserGlobalRolesCount = new AtomicInteger(0)
    private AtomicInteger getIdpFromIssuerCount = new AtomicInteger(0)
    private AtomicInteger getMappingPolicyForIdpCount = new AtomicInteger(0)
    private AtomicInteger generateTokenFromSamlResponseCount = new AtomicInteger(0)

    def templateEngine = new SimpleTemplateEngine()
    def xmlSlurper = new XmlSlurper()
    def random = new Random()

    // these fields are initialized by the constructor
    int port
    int originServicePort

    // these fields are initialized and reset by resetDefaultParameters()
    def client_token
    def client_tenantid
    def client_tenantname
    def client_tenantid2
    def client_username
    def client_userid
    def client_apikey
    def client_password
    def forbidden_apikey_or_pwd
    def not_found_apikey_or_pwd
    def admin_token
    def admin_tenant
    def admin_username
    def service_admin_role
    def endpointUrl
    def region
    def admin_userid
    def sleeptime
    def contact_id
    def contactIdJson
    def contactIdXml
    def additionalRolesXml
    def additionalRolesJson
    def impersonate_id
    def impersonate_name
    def validateTenant
    def appendedflag
    Validator validator
    boolean isTokenValid

    // these fields are initialized here and are never reset
    def tokenExpiresAt = null  // defaults to current time plus one day (on read)
    boolean checkTokenValid = false

    // these fields are initialized and reset by resetCounts()
    Closure<Response> handler
    Closure<Response> validateTokenHandler
    Closure<Response> getGroupsHandler
    Closure<Response> generateTokenHandler
    Closure<Response> getEndpointsHandler
    Closure<Response> getUserGlobalRolesHandler
    Closure<Response> getIdpFromIssuerHandler
    Closure<Response> getMappingPolicyForIdpHandler
    Closure<Response> generateTokenFromSamlResponseHandler

    MockIdentityV2Service(int identityPort, int originServicePort) {
        resetHandlers()
        resetDefaultParameters()

        this.port = identityPort
        this.originServicePort = originServicePort

        SchemaFactory factory = SchemaFactory.newInstance("http://www.w3.org/XML/XMLSchema/v1.1")
        factory.setFeature("http://apache.org/xml/features/validation/cta-full-xpath-checking", true)
        Schema schema = factory.newSchema(
                new StreamSource(MockIdentityV2Service.class.getResourceAsStream("/schema/openstack/credentials.xsd")))

        this.validator = schema.newValidator()
    }

    int getValidateTokenCount() {
        validateTokenCount.get()
    }

    int getGetGroupsCount() {
        getGroupsCount.get()
    }

    int getGenerateTokenCount() {
        generateTokenCount.get()
    }

    int getGetEndpointsCount() {
        getEndpointsCount.get()
    }

    int getGetIdpFromIssuerCount() {
        getIdpFromIssuerCount.get()
    }

    int getGetMappingPolicyForIdpCount() {
        getMappingPolicyForIdpCount.get()
    }

    int getGenerateTokenFromSamlResponseCount() {
        generateTokenFromSamlResponseCount.get()
    }

    /**
     * Reset all counts set to zero (initial state).
     */
    void resetCounts() {
        validateTokenCount.set(0)
        getGroupsCount.set(0)
        generateTokenCount.set(0)
        getEndpointsCount.set(0)
        getUserGlobalRolesCount.set(0)
        getIdpFromIssuerCount.set(0)
        getMappingPolicyForIdpCount.set(0)
        generateTokenFromSamlResponseCount.set(0)
    }

    /**
     * Reset all handlers to initial state.
     */
    void resetHandlers() {
        handler = this.&handleRequest
        validateTokenHandler = this.&validateToken
        getGroupsHandler = this.&getGroups
        generateTokenHandler = this.&generateToken
        getEndpointsHandler = this.&getEndpoints
        getUserGlobalRolesHandler = this.&getUserGlobalRoles
        getIdpFromIssuerHandler = createGetIdpFromIssuerHandler()
        getMappingPolicyForIdpHandler = createGetMappingPolicyForIdp()
        generateTokenFromSamlResponseHandler = this.&generateTokenFromSamlResponse
    }

    /**
     * At some points some of these fields values may be changed.
     * This function uses to reset to default state.
     */
    void resetDefaultParameters() {
        client_token = 'this-is-the-token'
        client_tenantid = 'this-is-the-tenant'
        client_tenantname = 'this-tenant-name'
        client_tenantid2 = 'this-is-the-nast-id'
        client_username = 'username'
        client_userid = 'user_12345'
        client_apikey = 'this-is-the-api-key'
        client_password = 'this-is-the-pwd'
        forbidden_apikey_or_pwd = 'this-key-pwd-results-in-forbidden'
        not_found_apikey_or_pwd = 'this-key-pwd-results-in-not-found'
        admin_token = 'this-is-the-admin-token'
        admin_tenant = 'this-is-the-admin-tenant'
        admin_username = 'admin_username'
        service_admin_role = 'service:admin-role1'
        endpointUrl = "localhost"
        region = "ORD"
        admin_userid = 67890
        sleeptime = 0
        contact_id = "${random.nextInt()}"
        contactIdJson = ""
        contactIdXml = ""
        additionalRolesXml = ""
        additionalRolesJson = ""
        impersonate_id = ""
        impersonate_name = ""
        validateTenant = null
        appendedflag = false
        isTokenValid = true
    }

    /**
     * HandleRequest handling all request from client to identity
     *  (we can still use the `handler' closure even if handleRequest is overridden in a derived class)
     * @param request
     * @return an instance of Response type
     */
    Response handleRequest(Request request) {
        def shouldReturnXml = request.headers.findAll('Accept').any { it.contains('application/xml') }

        /*
         * From http://docs.openstack.org/api/openstack-identity-service/2.0/content/
         *
         * POST
         * /v2.0/tokens
         * Authenticates and generates a token.
         *
         * GET
         * /v2.0/tokens/{tokenId}{?belongsTo}
         * tokenId : UUID - Required. The token ID.
         * Validates a token and confirms that it belongs to a specified tenant.
         *
         * GET
         * /v2.0/tokens/{tokenId}/endpoints
         * tokenId : UUID - Required. The token ID.
         * Lists the endpoints associated with a specified token.
         *
         * GET
         * /v2.0/users/{userId}/RAX-KSGRP
         * userId : String - The user ID.
         * X-Auth-Token : String - A valid authentication token for an administrative user.
         * List groups for a specified user.
         *
         * GET
         * /v2.0/users/{user_id}/roles
         * X-Auth-Token : String - A valid authentication token for an administrative user.
         * user_id : String - The user ID.
         * Lists global roles for a specified user. Excludes tenant roles.
         *
         * GET
         * /v2.0/RAX-AUTH/federation/identity-providers?issuer={issuer}
         * X-Auth-Token : String - A valid authentication token for a user that can read IDP info.
         * issuer : String - The issuer of the IDP that should be returned.
         * Returns a list with a single IDP whose issuer matches the requested value.
         *
         * GET
         * /v2.0/RAX-AUTH/federation/identity-providers/{idp_id}/mapping
         * X-Auth-Token : String - A valid authentication token for a user that can read IDP info.
         * idp_id : String - The internal ID representation of the IDP whose Mapping Policy should be returned.
         * The Mapping Policy for the requested IDP.
         *
         * POST
         * /v2.0/RAX-AUTH/federation/saml/auth
         * Authenticates using a saml:response and generates a token.
         *
         */

        def fullPath = request.path
        def method = request.method

        String path
        Map<String, String> queryParams

        // separate query and path
        if (fullPath.contains("?")) {
            int index = fullPath.indexOf("?")
            path = fullPath.substring(0, index)
            queryParams = fullPath.substring(index + 1).split("&").collectEntries { param ->
                param.split("=").collect { URLDecoder.decode(it, "UTF-8") }
            }
        } else {
            path = fullPath
            queryParams = [:]
        }

        if (path.startsWith("/v2.0/tokens")) {
            if (isGenerateTokenCallPath(path)) {
                if (method == "POST") {
                    generateTokenCount.incrementAndGet()
                    return generateTokenHandler(request, shouldReturnXml)
                } else {
                    return new Response(SC_METHOD_NOT_ALLOWED)
                }
            } else if (isGetEndpointsCallPath(path)) {
                if (method == "GET") {
                    getEndpointsCount.incrementAndGet()
                    def match = (path =~ PATH_REGEX_ENDPOINTS)
                    def tokenId = match[0][1]
                    return getEndpointsHandler(tokenId, request, shouldReturnXml)
                } else {
                    return new Response(SC_METHOD_NOT_ALLOWED)
                }
            } else if (isValidateTokenCallPath(path)) {
                if (method == 'GET') {
                    validateTokenCount.incrementAndGet()
                    def match = (path =~ PATH_REGEX_VALIDATE_TOKEN)
                    def tokenId = match[0][1]
                    return validateTokenHandler(tokenId, validateTenant, request, shouldReturnXml)
                } else {
                    return new Response(SC_METHOD_NOT_ALLOWED)
                }
            }
        } else if (path.startsWith("/v2.0/users/")) {
            if (isGetGroupsCallPath(path)) {
                if (method == "GET") {
                    getGroupsCount.incrementAndGet()
                    def match = (path =~ PATH_REGEX_GROUPS)
                    def userId = match[0][1]
                    return getGroupsHandler(userId, request, shouldReturnXml)
                } else {
                    return new Response(SC_METHOD_NOT_ALLOWED)
                }
            } else if (isGetUserGlobalRolesCallPath(path)) {
                if (method == "GET") {
                    getUserGlobalRolesCount.incrementAndGet()
                    def match = (path =~ PATH_REGEX_USER_GLOBAL_ROLES)
                    def userId = match[0][1]
                    return getUserGlobalRolesHandler(userId, request, shouldReturnXml)
                } else {
                    return new Response(SC_METHOD_NOT_ALLOWED)
                }
            }
        } else if (path.startsWith("/v2.0/RAX-AUTH/federation/")) {
            if (isSamlIdpIssuerCallPath(path)) {
                if (method == "GET") {
                    getIdpFromIssuerCount.incrementAndGet()
                    return getIdpFromIssuerHandler(queryParams.issuer, request)
                } else {
                    return new Response(SC_METHOD_NOT_ALLOWED)
                }
            } else if (isSamlIdpMappingPolicyCallPath(path)) {
                if (method == "GET") {
                    getMappingPolicyForIdpCount.incrementAndGet()
                    def idpId = (path =~ PATH_REGEX_SAML_MAPPING)[0][1]
                    return getMappingPolicyForIdpHandler(idpId, request)
                } else {
                    return new Response(SC_METHOD_NOT_ALLOWED)
                }
            } else if (isSamlAuthCallPath(path)) {
                if (method == "POST") {
                    generateTokenFromSamlResponseCount.incrementAndGet()
                    return generateTokenFromSamlResponseHandler(request, shouldReturnXml)
                } else {
                    return new Response(SC_METHOD_NOT_ALLOWED)
                }
            }
        }

        return new Response(SC_NOT_IMPLEMENTED)
    }

    static boolean isGetUserGlobalRolesCallPath(String path) {
        path ==~ PATH_REGEX_USER_GLOBAL_ROLES
    }

    static boolean isGetGroupsCallPath(String path) {
        path ==~ PATH_REGEX_GROUPS
    }

    static boolean isGetEndpointsCallPath(String path) {
        path ==~ PATH_REGEX_ENDPOINTS
    }

    static boolean isValidateTokenCallPath(String path) {
        path ==~ PATH_REGEX_VALIDATE_TOKEN
    }

    static boolean isGenerateTokenCallPath(String path) {
        path == "/v2.0/tokens"
    }

    static boolean isSamlIdpIssuerCallPath(String path) {
        path ==~ PATH_REGEX_SAML_IDP_ISSUER
    }

    static boolean isSamlIdpMappingPolicyCallPath(String path) {
        path ==~ PATH_REGEX_SAML_MAPPING
    }

    static boolean isSamlAuthCallPath(String path) {
        path.startsWith("/v2.0/RAX-AUTH/federation/saml/auth")
    }

    /**
     * Get token expired time as a string
     * @return
     */
    String getExpires() {
        if (this.tokenExpiresAt != null && this.tokenExpiresAt instanceof String) {
            this.tokenExpiresAt
        } else if (this.tokenExpiresAt instanceof DateTime) {
            DateTimeFormat.forPattern(DATE_FORMAT).withLocale(Locale.US).withZone(DateTimeZone.UTC).print(tokenExpiresAt)
        } else if (this.tokenExpiresAt) {
            this.tokenExpiresAt as String
        } else {
            new DateTime().plusDays(1)
        }
    }

    /**
     * Simulate response for get token call of identity v2
     * @param request
     * @param xml
     * @return an instance of response
     */
    Response generateToken(Request request, boolean xml) {
        // Since the SchemaFactory does not appear to import parent XSD's,
        // the validation is skipped for the API Key Credentials that are defined externally.
        if (xml && !(request.body.toString().contains("apiKeyCredentials"))) {
            try {
                final StreamSource sampleSource = new StreamSource(new ByteArrayInputStream(request.body.getBytes()))
                validator.validate(sampleSource)
            } catch (Exception e) {
                println("Admin token XSD validation error: " + e)
                return new Response(SC_BAD_REQUEST)
            }
        }

        def params = [:]

        def isTokenChecked = true
        // IF the body of the request should be evaluated to determine the validity of the Token, THEN ...
        // ELSE the just use the isTokenValid value.
        if (checkTokenValid) {
            // IF the body is a Client userName/apiKey request,
            // THEN return the Client token response;
            // ELSE /*IF the body is userName/passWord request*/,
            // THEN return the Admin token response.
            if (request.body.contains("username") &&
                    request.body.contains(client_username) &&
                    ((request.body.contains("apiKey") &&
                            request.body.contains(client_apikey)) ||
                            (request.body.contains("password") &&
                                    request.body.contains(client_password)))) {
                params = [
                        expires      : getExpires(),
                        userid       : client_userid,
                        username     : client_username,
                        tenantid     : client_tenantid,
                        tenantname   : client_tenantname,
                        tenantidtwo  : client_tenantid2,
                        token        : client_token,
                        serviceadmin : service_admin_role,
                        contactIdXml : contactIdXml,
                        contactIdJson: contactIdJson
                ]

                if (contact_id) {
                    params.contactIdXml = /rax-auth:contactId="$contact_id"/
                    params.contactIdJson = /"RAX-AUTH:contactId": "$contact_id",/
                }

            } else if (request.body.contains("username") &&
                    request.body.contains(admin_username) /*&&
                request.body.contains("password") &&
                request.body.contains(admin_password)*/) {
                params = [
                        expires      : getExpires(),
                        userid       : admin_userid,
                        username     : admin_username,
                        tenantid     : admin_tenant,
                        tenantidtwo  : admin_tenant,
                        token        : admin_token,
                        serviceadmin : service_admin_role,
                        contactIdXml : contactIdXml,
                        contactIdJson: contactIdJson
                ]
                if (contact_id) {
                    params.contactIdXml = /rax-auth:contactId="$contact_id"/
                    params.contactIdJson = /"RAX-AUTH:contactId": "$contact_id",/
                }
            } else {
                isTokenChecked = false
            }
        } else {
            params = [
                    expires      : getExpires(),
                    userid       : admin_userid,
                    username     : admin_username,
                    tenantid     : admin_tenant,
                    tenantidtwo  : admin_tenant,
                    token        : admin_token,
                    serviceadmin : service_admin_role,
                    contactIdXml : contactIdXml,
                    contactIdJson: contactIdJson
            ]
            if (contact_id) {
                params.contactIdXml = /rax-auth:contactId="$contact_id"/
                params.contactIdJson = /"RAX-AUTH:contactId" : "$contact_id",/
            }
        }

        def code
        def template
        def headers = [:]

        headers.put('Content-type', xml ? 'application/xml' : 'application/json')

        if (isTokenValid && isTokenChecked) {
            code = SC_OK
            template = xml ? identitySuccessXmlTemplate : identitySuccessJsonTemplate
        } else {
            //If the username or the apikey are longer than 120 characters, barf back a 400, bad request response
            //I have to parse the XML body of the request to mimic behavior in identity
            def auth = new XmlSlurper().parseText(request.body.toString())
            String username = auth.apiKeyCredentials['@username']
            String apikey = auth.apiKeyCredentials['@apiKey']
            String password = auth.passwordCredentials['@password']

            //Magic numbers are how large of a value identity will parse before giving back a 400 Bad Request
            if (apikey.length() > 100 || password.length() > 100 || username.length() > 100) {
                code = SC_BAD_REQUEST
            } else if (request.body.toString().contains(forbidden_apikey_or_pwd)) {
                code = SC_FORBIDDEN
            } else if (request.body.toString().contains(not_found_apikey_or_pwd)) {
                code = SC_NOT_FOUND
            } else {
                code = SC_UNAUTHORIZED
            }

            template = xml ? identityFailureXmlTemplate : identityFailureJsonTemplate
        }

        def body = templateEngine.createTemplate(template).make(params)
        if (sleeptime > 0) {
            sleep(sleeptime)
        }
        return new Response(code, null, headers, body)
    }

    /**
     * Simuate response for validateToken call of identity v2
     * @param tokenId
     * @param tenantId (if validateToken with belongsTo tenant)
     * @return an instance of response
     */
    Response validateToken(String tokenId, String tenantId = null, Request request, boolean xml) {
        def requestToken = tokenId
        def passedTenant = tenantId ?: client_tenantid

        def params = [
                expires        : getExpires(),
                userid         : client_userid,
                username       : client_username,
                tenantid       : passedTenant,
                tenantname     : client_tenantname,
                tenantidtwo    : client_tenantid2,
                token          : requestToken,
                serviceadmin   : service_admin_role,
                impersonateid  : impersonate_id,
                impersonatename: impersonate_name,
                contactIdXml   : contactIdXml,
                contactIdJson  : contactIdJson
        ]
        if (contact_id) {
            params.contactIdXml = /rax-auth:contactId="$contact_id"/
            params.contactIdJson = /"RAX-AUTH:contactId" : "$contact_id",/
        }

        def code
        def template
        def headers = [:]

        headers.put('Content-type', xml ? 'application/xml' : 'application/json')

        if (isTokenValid) {
            code = SC_OK
            if (xml) {
                if (tokenId == "rackerButts") {
                    template = rackerTokenXmlTemplate
                } else if (tokenId == "rackerSSO") {
                    template = rackerSuccessfulValidateRespXmlTemplate
                } else if (tokenId == "dedicatedUser") {
                    template = dedicatedUserSuccessfulRespXmlTemplate
                } else if (tokenId == "failureRacker") {
                    template = rackerTokenWithoutProperRoleXmlTemplate
                } else if (impersonate_id != "") {
                    template = successfulImpersonateValidateTokenXmlTemplate
                } else {
                    template = successfulValidateTokenXmlTemplate
                }
            } else {
                if (tokenId == "rackerSSO") {
                    template = rackerSuccessfulValidateRespJsonTemplate
                } else if (tokenId == "dedicatedUser") {
                    template = dedicatedUserSuccessfulRespJsonTemplate
                } else if (impersonate_id != "") {
                    template = successfulImpersonateValidateTokenJsonTemplate
                } else {
                    template = successfulValidateTokenJsonTemplate
                }
            }
        } else {
            code = SC_NOT_FOUND
            template = xml ? identityFailureXmlTemplate : identityFailureJsonTemplate
        }

        def body = templateEngine.createTemplate(template).make(params)
        if (sleeptime > 0) {
            sleep(sleeptime)
        }
        return new Response(code, null, headers, body)
    }

    /**
     * Simulate response list groups for user
     * @param userId
     * @param request
     * @param xml
     * @return
     */
    Response getGroups(String userId, Request request, boolean xml) {
        def requestUserId = userId
        def params = [
                expires     : getExpires(),
                userid      : requestUserId,
                username    : client_username,
                tenantid    : client_tenantid,
                tenantname  : client_tenantname,
                token       : request.getHeaders().getFirstValue("X-Auth-Token"),
                serviceadmin: service_admin_role
        ]

        def code
        def template
        def headers = [:]

        headers.put('Content-type', xml ? 'application/xml' : 'application/json')

        if (userId == client_userid.toString() || userId == (admin_userid as String)) {
            if (userId == "rackerSSOUsername" || service_admin_role.toLowerCase() == "racker") {
                code = SC_NOT_FOUND
                template = xml ? identityFailureXmlTemplate : identityFailureJsonTemplate
            } else {
                code = SC_OK
                template = xml ? groupsXmlTemplate : groupsJsonTemplate
            }
        } else {
            code = SC_INTERNAL_SERVER_ERROR
            template = xml ? identityFailureXmlTemplate : identityFailureJsonTemplate
        }

        def body = templateEngine.createTemplate(template).make(params)

        return new Response(code, null, headers, body)
    }

    /**
     * Simulate response get endpoints
     * @param tokenId
     * @param request
     * @param xml
     * @return
     */
    Response getEndpoints(String tokenId, Request request, boolean xml) {
        def template
        def headers = [:]

        if (xml) {
            headers.put('Content-type', 'application/xml')
            template = appendedflag ? this.identityEndpointXmlAppendedTemplate : this.identityEndpointXmlTemplate
        } else {
            headers.put('Content-type', 'application/json')
            template = appendedflag ? this.identityEndpointsJsonAppendedTemplate : this.identityEndpointsJsonTemplate
        }

        def params = [
                identityPort     : this.port,
                token            : request.getHeaders().getFirstValue("X-Auth-Token"),
                expires          : getExpires(),
                userid           : this.client_userid,
                username         : this.client_username,
                tenantid         : this.client_tenantid,
                originServicePort: this.originServicePort,
                endpointUrl      : this.endpointUrl,
                region           : this.region,
                contactIdXml     : this.contactIdXml,
                contactIdJson    : this.contactIdJson
        ]

        def body = templateEngine.createTemplate(template).make(params)
        return new Response(SC_OK, null, headers, body)
    }

    /**
     * Simulate response get user global roles
     * @param userId
     * @param request
     * @param xml
     * @return
     */
    Response getUserGlobalRoles(String userId, Request request, boolean xml) {
        def template
        def headers = [:]

        headers.put('Content-type', xml ? 'application/xml' : 'application/json')
        template = xml ? UserGlobalRolesXmlTemplate : UserGlobalRolesJsonTemplate

        def params = [
                addRolesXml : additionalRolesXml,
                addRolesJson: additionalRolesJson
        ]

        def body = templateEngine.createTemplate(template).make(params)
        return new Response(SC_OK, null, headers, body)
    }

    Closure<Response> createGetIdpFromIssuerHandler(Map values = [:]) {
        def headers = ['Content-type': 'application/json']

        return { String issuer, Request request ->
            if (admin_token != request.getHeaders().getFirstValue("X-Auth-Token") && !values.skipAuthCheck) {
                new Response(SC_UNAUTHORIZED, null, headers, UNAUTHORIZED_JSON)
            } else {
                def body = createIdpJsonWithValues([issuer: issuer] + values)

                new Response(SC_OK, null, headers, body)
            }
        }
    }

    Closure<Response> createGetMappingPolicyForIdp(Map values = [defaultMapping: true]) {
        def headers = ['Content-type': 'application/json']

        return { String idpId, Request request ->
            if (admin_token != request.getHeaders().getFirstValue("X-Auth-Token") && !values.skipAuthCheck) {
                new Response(SC_UNAUTHORIZED, null, headers, UNAUTHORIZED_JSON)
            } else {
                def mappingForIdpId = values.mappings?.get(idpId)

                if (mappingForIdpId) {
                    new Response(SC_OK, null, headers, mappingForIdpId)
                } else if (values.defaultMapping) {
                    new Response(SC_OK, null, headers, DEFAULT_MAPPING_POLICY)
                } else {
                    def body = createIdentityFaultJsonWithValues(
                            name: "itemNotFound",
                            code: SC_NOT_FOUND,
                            message: "Identity Provider with id/name: '$idpId' was not found.")
                    new Response(SC_NOT_FOUND, null, headers, body)
                }
            }
        }
    }

    Response generateTokenFromSamlResponse(Request request, boolean shouldReturnXml) {
        def samlResponse = xmlSlurper.parseText(request.body as String)
                .declareNamespace(saml2p: "urn:oasis:names:tc:SAML:2.0:protocol", saml2: "urn:oasis:names:tc:SAML:2.0:assertion")
        Map<String, List<String>> samlAttributes = samlResponse.'saml2:Assertion'[0].'saml2:AttributeStatement'.'saml2:Attribute'
                .collectEntries { [(it.@Name): it.'saml2:AttributeValue'*.text() as List] }

        def username = samlResponse.'saml2:Assertion'[0].'saml2:Subject'.'saml2::NameID'.text()
        def idpAuthBy = samlResponse.'saml2:Assertion'[0].'saml2:AuthnStatement'.'saml2:AuthnContext'.'saml2:AuthnContextClassRef'.text()

        def authBy = ["FEDERATION"]
        if (idpAuthBy == SAML_AUTH_BY_PASSWORD) {
            authBy << "PASSWORD"
        } else if (idpAuthBy == SAML_AUTH_BY_RSAKEY) {
            authBy << "RSAKEY"
        }

        def bodyBuilder = shouldReturnXml ? this.&createAccessXmlWithValues : this.&createAccessJsonWithValues
        def body = bodyBuilder(username: username, roles: samlAttributes.roles, authBy: authBy)
        def headers = ['Content-type': shouldReturnXml ? 'application/xml' : 'application/json']

        new Response(SC_OK, null, headers, body)
    }

    static String createIdpJsonWithValues(Map values = [:]) {
        def json = new JsonBuilder()

        json {
            'RAX-AUTH:identityProviders'([
                    {
                        name values.name ?: "External IDP"
                        federationType values.federationType ?: "DOMAIN"
                        approvedDomains values.approvedDomains ?: ["77366"]
                        description values.description ?: "An External IDP Description"
                        id values.id ?: "508daa5d406d41639c67860f25db29df"
                        issuer values.issuer ?: "http://idp.external.com"
                    }
            ])
        }

        json.toString()
    }

    static String createMappingJsonWithValues(Map values = [:]) {
        def json = new JsonBuilder()

        json {
            mapping {
                rules([
                        {
                            local {
                                user {
                                    domain values.domain ?: DEFAULT_MAPPING_VALUE
                                    name values.name ?: DEFAULT_MAPPING_VALUE
                                    email values.email ?: DEFAULT_MAPPING_VALUE
                                    roles values.roles ?: DEFAULT_MAPPING_VALUE
                                    expire values.expire ?: DEFAULT_MAPPING_VALUE
                                    if (values.userExtAttribs) {
                                        values.userExtAttribs.each { key, value ->
                                            "$key" value
                                        }
                                    }
                                }
                                if (values.local) {
                                    values.local.each { key, value ->
                                        "$key" value
                                    }
                                }
                            }
                            if (values.remote) {
                                remote values.remote
                            }
                        }
                ] + (values.rules ?: []))
                version "RAX-1"
            }
        }

        json.toString()
    }

    String createAccessJsonWithValues(Map values = [:]) {
        def token = values.token ?: client_token
        def expires = values.expires ?: getExpires()
        def tenantId = values.tenantId ?: client_tenantid
        def userId = values.userId ?: client_userid
        def username = values.username ?: client_username
        def roleNames = values.roles ?: ["identity:admin"]

        def json = new JsonBuilder()

        json {
            access {
                delegate.token {
                    id token
                    delegate.expires expires
                    tenant {
                        id tenantId
                        name tenantId
                    }
                    if (values.authBy) {
                        'RAX-AUTH:authenticatedBy' values.authBy
                    }
                }
                user {
                    id userId
                    name username
                    'RAX-AUTH:defaultRegion' "the-default-region"
                    roles roleNames.withIndex(1).collect { role, index ->
                        [name: role, id: index]
                    }
                }
                serviceCatalog([
                        {
                            name "cloudServersOpenStack"
                            type "compute"
                            endpoints([
                                    {
                                        publicURL "https://ord.servers.api.rackspacecloud.com/v2/$tenantId"
                                        delegate.region "ORD"
                                        delegate.tenantId tenantId
                                        versionId "2"
                                        versionInfo "https://ord.servers.api.rackspacecloud.com/v2"
                                        versionList "https://ord.servers.api.rackspacecloud.com/"
                                    }
                            ])
                        }
                ])
            }
        }

        json.toString()
    }

    String createAccessXmlWithValues(Map values = [:]) {
        def token = values.token ?: client_token
        def expires = values.expires ?: getExpires()
        def tenantId = values.tenantId ?: client_tenantid
        def userId = values.userId ?: client_userid
        def username = values.username ?: client_username
        def roleNames = values.roles ?: ["identity:admin"]

        // namespaces
        Map<String, String> rootElementAttributes = [:]
        rootElementAttributes.'xmlns' = "http://docs.openstack.org/identity/api/v2.0"
        rootElementAttributes.'xmlns:rax-auth' = "http://docs.rackspace.com/identity/api/ext/RAX-AUTH/v1.0"

        // set up Markup Builder
        def writer = new StringWriter()
        def xmlBuilder = new MarkupBuilder(writer)
        xmlBuilder.doubleQuotes = true
        xmlBuilder.mkp.xmlDeclaration(version: "1.0", encoding: "UTF-8")

        // build the XML
        xmlBuilder.access(rootElementAttributes) {
            delegate.token(id: token, expires: expires) {
                tenant(id: tenantId, name: tenantId)
                if (values.authBy) {
                    "rax-auth:authenticatedBy" {
                        values.authBy.each {
                            "rax-auth:credential"(it)
                        }
                    }
                }
            }
            user(id: userId, name: username, "rax-auth:defaultRegion": "the-default-region") {
                roles {
                    roleNames.withIndex(1).each { name, index ->
                        role(name: name, id: index)
                    }
                }
            }
            serviceCatalog {
                service(type: "compute", name: "cloudServersOpenStack") {
                    endpoint(region: "ORD",
                            tenantId: tenantId,
                            publicURL: "https://ord.servers.api.rackspacecloud.com/v2/$tenantId",
                            versionId: "2",
                            versionInfo: "https://ord.servers.api.rackspacecloud.com/v2",
                            versionList: "https://ord.servers.api.rackspacecloud.com/")
                }
            }
        }

        writer.toString()
    }

    static String createIdentityFaultJsonWithValues(Map values = [:]) {
        def name = values.name ?: "identityFault"
        def code = values.code ?: 500
        def message = values.message ?: "Internal server error."

        def json = new JsonBuilder()

        json {
            "$name" {
                delegate.code code
                delegate.message message
            }
        }

        json.toString()
    }

    static final String UNAUTHORIZED_JSON = createIdentityFaultJsonWithValues(
            name: "unauthorized",
            code: SC_UNAUTHORIZED,
            message: "No valid token provided. Please use the 'X-Auth-Token' header with a valid token.")

    // Successful generate token response in xml
    def identitySuccessXmlTemplate = """\
<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<access xmlns="http://docs.openstack.org/identity/api/v2.0"
        xmlns:os-ksadm="http://docs.openstack.org/identity/api/ext/OS-KSADM/v1.0"
        xmlns:os-ksec2="http://docs.openstack.org/identity/api/ext/OS-KSEC2/v1.0"
        xmlns:rax-ksqa="http://docs.rackspace.com/identity/api/ext/RAX-KSQA/v1.0"
        xmlns:rax-kskey="http://docs.rackspace.com/identity/api/ext/RAX-KSKEY/v1.0">
    <token id="\${token}"
           expires="\${expires}">
        <tenant id="\${tenantid}"
                name="\${tenantname}"/>
    </token>
    <user xmlns:rax-auth="http://docs.rackspace.com/identity/api/ext/RAX-AUTH/v1.0"
          id="\${userid}"
          name="\${username}"
          rax-auth:defaultRegion="the-default-region"
          \${contactIdXml}>
        <roles>
            <role id="684"
                  name="compute:default"
                  description="A Role that allows a user access to keystone Service methods"
                  serviceId="0000000000000000000000000000000000000001"
                  tenantId="\${tenantid}"/>
            <role id="5"
                  name="object-store:default"
                  description="A Role that allows a user access to keystone Service methods"
                  serviceId="0000000000000000000000000000000000000002"
                  tenantId="\${tenantidtwo}"/>
            <role id="6"
                  name="\${serviceadmin}"
                  description="A Role that allows a user access to keystone Service methods"
                  serviceId="0000000000000000000000000000000000000002"
                  tenantId="\${tenantid}"/>
        </roles>
    </user>
    <serviceCatalog>
        <service type="compute"
                 name="cloudServersOpenStack">
            <endpoint region="ORD"
                      tenantId="\${tenantid}"
                      publicURL="https://ord.servers.api.rackspacecloud.com/v2/\${tenantid}"
                      versionId="2"
                      versionInfo="https://ord.servers.api.rackspacecloud.com/v2"
                      versionList="https://ord.servers.api.rackspacecloud.com/"/>
            <endpoint region="DFW"
                      tenantId="\${tenantid}"
                      publicURL="https://dfw.servers.api.rackspacecloud.com/v2/\${tenantid}"
                      versionId="2"
                      versionInfo="https://dfw.servers.api.rackspacecloud.com/v2"
                      versionList="https://dfw.servers.api.rackspacecloud.com/"/>
        </service>
        <service type="rax:object-cdn"
                 name="cloudFilesCDN">
            <endpoint region="DFW"
                      tenantId="\${tenantidtwo}"
                      publicURL="https://cdn.stg.clouddrive.com/v1/\${tenantidtwo}"/>
            <endpoint region="ORD"
                      tenantId="\${tenantidtwo}"
                      publicURL="https://cdn.stg.clouddrive.com/v1/\${tenantidtwo}"/>
        </service>
        <service type="object-store"
                 name="cloudFiles">
            <endpoint region="ORD"
                      tenantId="\${tenantidtwo}"
                      publicURL="https://storage.stg.swift.racklabs.com/v1/\${tenantidtwo}"
                      internalURL="https://snet-storage.stg.swift.racklabs.com/v1/\${tenantidtwo}"/>
            <endpoint region="DFW"
                      tenantId="\${tenantidtwo}"
                      publicURL="https://storage.stg.swift.racklabs.com/v1/\${tenantidtwo}"
                      internalURL="https://snet-storage.stg.swift.racklabs.com/v1/\${tenantidtwo}"/>
        </service>
    </serviceCatalog>
</access>
"""

    // Successful generate token response in json
    def identitySuccessJsonTemplate = """\
{
   "access" : {
      "serviceCatalog" : [
         {
            "name": "cloudServersOpenStack",
            "type": "compute",
            "endpoints": [
            {
                "publicURL": "https://ord.servers.api.rackspacecloud.com/v2/\${tenantid}",
                "region": "ORD",
                "tenantId": "\${tenantid}",
                "versionId": "2",
                "versionInfo": "https://ord.servers.api.rackspacecloud.com/v2",
                "versionList": "https://ord.servers.api.rackspacecloud.com/"
            },
            {
                "publicURL": "https://dfw.servers.api.rackspacecloud.com/v2/\${tenantid}",
                "region": "DFW",
                "tenantId": "\${tenantid}",
                "versionId": "2",
                "versionInfo": "https://dfw.servers.api.rackspacecloud.com/v2",
                "versionList": "https://dfw.servers.api.rackspacecloud.com/"
            }
            ]
         },
         {
            "name" : "cloudFilesCDN",
            "type" : "rax:object-cdn",
            "endpoints" : [
               {
                  "publicURL" : "https://cdn.stg.clouddrive.com/v1/\${tenantidtwo}",
                  "tenantId" : "\${tenantidtwo}",
                  "region" : "DFW"
               },
               {
                  "publicURL" : "https://cdn.stg.clouddrive.com/v1/\${tenantidtwo}",
                  "tenantId" : "\${tenantidtwo}",
                  "region" : "ORD"
               }
            ]
         },
         {
            "name" : "cloudFiles",
            "type" : "object-store",
            "endpoints" : [
               {
                  "internalURL" : "https://snet-storage.stg.swift.racklabs.com/v1/\${tenantidtwo}",
                  "publicURL" : "https://storage.stg.swift.racklabs.com/v1/\${tenantidtwo}",
                  "tenantId" : "\${tenantidtwo}",
                  "region" : "ORD"
               },
               {
                  "internalURL" : "https://snet-storage.stg.swift.racklabs.com/v1/\${tenantidtwo}",
                  "publicURL" : "https://storage.stg.swift.racklabs.com/v1/\${tenantidtwo}",
                  "tenantId" : "\${tenantidtwo}",
                  "region" : "DFW"
               }
            ]
         }
      ],
      "user" : {
         "roles" : [
            {
               "tenantId" : "\${tenantid}",
               "name" : "compute:default",
               "id" : "684",
               "description" : "A Role that allows a user access to keystone Service methods"
            },
            {
               "tenantId" : "\${tenantidtwo}",
               "name" : "object-store:default",
               "id" : "5",
               "description" : "A Role that allows a user access to keystone Service methods"
            },
            {
               "name" : "identity:admin",
               "id" : "1",
               "description" : "Admin Role."
            }
         ],
         "RAX-AUTH:defaultRegion" : "the-default-region",
         "name" : "\${username}",
         "id" : "\${userid}"
      },
      "token" : {
         "tenant" : {
            "name" : "\${tenantid}",
            "id" : "\${tenantid}"
         },
         "id" : "\${token}",
         "expires" : "\${expires}"
      }
   }
}
"""

    // Successful validate token response in xml
    def successfulValidateTokenXmlTemplate = """\
<?xml version="1.0" encoding="UTF-8"?>
<access
    xmlns:os-ksadm="http://docs.openstack.org/identity/api/ext/OS-KSADM/v1.0"
    xmlns="http://docs.openstack.org/identity/api/v2.0">
    <token id="\${token}"
        expires="\${expires}">
        <tenant id="\${tenantid}" name="\${tenantname}" />
    </token>
    <user
        xmlns:rax-auth="http://docs.rackspace.com/identity/api/ext/RAX-AUTH/v1.0"
        id="\${userid}" username="\${username}" rax-auth:defaultRegion="DFW">
        <roles xmlns="http://docs.openstack.org/identity/api/v2.0">
            <role id="123" name="compute:admin" />
            <role id="234" name="object-store:admin" />
        </roles>
    </user>
</access>
"""

    // Successful impersonate validate token response in xml
    def successfulImpersonateValidateTokenXmlTemplate = """\
<?xml version="1.0" encoding="UTF-8"?>
<access
    xmlns:os-ksadm="http://docs.openstack.org/identity/api/ext/OS-KSADM/v1.0"
    xmlns="http://docs.openstack.org/identity/api/v2.0">
    <token id="\${token}"
        expires="\${expires}">
        <tenant id="\${tenantid}" name="\${tenantname}" />
    </token>
    <user
        xmlns:rax-auth="http://docs.rackspace.com/identity/api/ext/RAX-AUTH/v1.0"
        id="\${userid}" username="\${username}" rax-auth:defaultRegion="DFW">
        <roles xmlns="http://docs.openstack.org/identity/api/v2.0">
            <role id="123" name="compute:admin" />
            <role id="234" name="object-store:admin" />
        </roles>
    </user>
    <RAX-AUTH:impersonator id="\${impersonateid}"
        username="\${impersonatename}">
        <roles xmlns="http://docs.openstack.org/identity/api/v2.0">
            <role id="123" name="Racker" />
            <role id="234" name="object-store:admin" />
        </roles>
    </RAX-AUTH:impersonator>
</access>
"""

    // Successful validate token response in json
    def successfulValidateTokenJsonTemplate = """\
{
    "access":{
        "token":{
            "id":"\${token}",
            "expires":"\${expires}",
            "tenant":{
                "id": "\${tenantid}",
                "name": "\${tenantname}"
            },
            "RAX-AUTH:authenticatedBy": [
                "PASSWORD",
                "PASSCODE"
            ]
        },
        "user":{
            "RAX-AUTH:defaultRegion": "DFW",
            \${contactIdJson}
            "id":"\${userid}",
            "name":"\${username}",
            "roles":[{
                    "tenantId" : "\${tenantid}",
                    "id":"123",
                    "name":"compute:admin"
                },
                {
                    "tenantId" : "\${tenantidtwo}",
                    "id":"234",
                    "name":"object-store:admin"
                },
                {
                    "id":"345",
                    "name":"\${serviceadmin}"
                }
            ]
        }
    }
}
"""

    // Successful validate token response in json
    def successfulImpersonateValidateTokenJsonTemplate = """\
{
    "access":{
        "token":{
            "id":"\${token}",
            "expires":"\${expires}",
            "tenant":{
                "id": "\${tenantid}",
                "name": "\${tenantname}"
            }
        },
        "user":{
            "RAX-AUTH:defaultRegion": "DFW",
            \${contactIdJson}
            "id":"\${userid}",
            "name":"\${username}",
            "roles":[{
                    "id":"123",
                    "name":"compute:admin"
                },
                {
                    "tenantId" : "\${tenantid}",
                    "id":"234",
                    "name":"object-store:admin"
                },
                {
                    "id":"345",
                    "name":"\${serviceadmin}"
                }
            ]
        },
        "RAX-AUTH:impersonator":{
            "id":"\${impersonateid}",
            "name":"\${impersonatename}",
            "roles":[{
                       "id":"123",
                       "name":"Racker"
                     },
                     {
                        "id":"234",
                        "name":"object-store:admin"
                     }
           ]
       }
    }
}
"""

    // Failure Response for validate token in json
    def identityFailureJsonTemplate = """\
{
   "itemNotFound" : {
      "message" : "Invalid Token, not found.",
      "code" : 404
   }
}
"""

    // Failure Response for validate token in xml
    def identityFailureXmlTemplate = """\
<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<itemNotFound xmlns="http://docs.openstack.org/identity/api/v2.0"
              xmlns:ns2="http://docs.openstack.org/identity/api/ext/OS-KSADM/v1.0"
              code="404">
  <message>Invalid Token, not found.</message>
</itemNotFound>
"""

    // TODO: Replace this with builder
    def groupsJsonTemplate = """\
{
  "RAX-KSGRP:groups": [
    {
        "id": "0",
        "description": "Default Limits",
        "name": "Default"
    }
  ]
}
"""

    // TODO: Replace this with builder
    def groupsXmlTemplate = """\
<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<groups xmlns="http://docs.rackspace.com/identity/api/ext/RAX-KSGRP/v1.0">
    <group id="0" name="Default">
        <description>Default Limits</description>
    </group>
</groups>
"""

    // TODO: Replace this with builder
    def identityEndpointsJsonTemplate = """\
{
    "endpoints_links": [
        {
            "href": "http://localhost:\${identityPort}/tokens/\${token}/endpoints?'marker=5&limit=10'",
            "rel": "next"
        }
    ],
    "endpoints": [
        {
            "internalURL": "http://\${endpointUrl}:\${originServicePort}/v1/AUTH_1",
            "name": "swift",
            "adminURL": "http://\${endpointUrl}:\${originServicePort}/",
            "region": "\${region}",
            "tenantId": "\${tenantid}",
            "type": "object-store",
            "id": 1,
            "publicURL": "http://\${endpointUrl}:\${originServicePort}/"
        },
        {
            "internalURL": "http://\${endpointUrl}:\${originServicePort}/",
            "name": "nova_compat",
            "adminURL": "http://\${endpointUrl}:\${originServicePort}/",
            "region": "\${region}",
            "tenantId": "\${tenantid}",
            "type": "compute",
            "id": 2,
            "publicURL": "http://\${endpointUrl}:\${originServicePort}/"
        },
        {
            "internalURL": "http://\${endpointUrl}:\${originServicePort}/v1",
            "name": "OpenStackService",
            "adminURL": "http://\${endpointUrl}:\${originServicePort}/v1",
            "region": "\${region}",
            "tenantId": "\${tenantid}",
            "type": "service",
            "id": 3,
            "version": "1",
            "publicURL": "http://\${endpointUrl}:\${originServicePort}/v1"
        }
    ]
}"""

    // TODO: Replace this with builder
    def identityEndpointsJsonAppendedTemplate = """\
{
    "endpoints_links": [
        {
            "href": "http://localhost:\${identityPort}/tokens/\${token}/endpoints?'marker=5&limit=10'",
            "rel": "next"
        }
    ],
    "endpoints": [
        {
            "internalURL": "http://\${endpointUrl}:\${originServicePort}/v1/AUTH_1",
            "name": "swift",
            "adminURL": "http://\${endpointUrl}:\${originServicePort}/",
            "region": "\${region}",
            "tenantId": "\${tenantid}",
            "type": "object-store",
            "id": 1,
            "publicURL": "http://\${endpointUrl}:\${originServicePort}/"
        },
        {
            "internalURL": "http://\${endpointUrl}:\${originServicePort}/",
            "name": "nova_compat",
            "adminURL": "http://\${endpointUrl}:\${originServicePort}/",
            "region": "\${region}",
            "tenantId": "\${tenantid}",
            "type": "compute",
            "id": 2,
            "publicURL": "http://\${endpointUrl}:\${originServicePort}/"
        },
        {
            "internalURL": "http://\${endpointUrl}:\${originServicePort}/v1",
            "name": "OpenStackService",
            "adminURL": "http://\${endpointUrl}:\${originServicePort}/v1",
            "region": "\${region}",
            "tenantId": "\${tenantid}",
            "type": "service",
            "id": 3,
            "version": "1",
            "publicURL": "http://\${endpointUrl}:\${originServicePort}/v1"
        },
        {
            "internalURL": "http://\${endpointUrl}:\${originServicePort}/v1",
            "name": "OpenStackService",
            "adminURL": "http://\${endpointUrl}:\${originServicePort}/v1",
            "region": "\${region}",
            "tenantId": "\${tenantid}",
            "type": "service",
            "id": 3,
            "version": "1",
            "publicURL": "http://\${endpointUrl}:\${originServicePort}/v1/appended/\${tenantid}"
        }
    ]
}"""

    // TODO: Replace this with builder
    def identityEndpointXmlTemplate = """\
<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<endpoints xmlns="http://docs.openstack.org/identity/api/v2.0"
           xmlns:ns2="http://www.w3.org/2005/Atom"
           xmlns:os-ksadm="http://docs.openstack.org/identity/api/ext/OS-KSADM/v1.0"
           xmlns:rax-ksqa="http://docs.rackspace.com/identity/api/ext/RAX-KSQA/v1.0"
           xmlns:rax-kskey="http://docs.rackspace.com/identity/api/ext/RAX-KSKEY/v1.0"
           xmlns:os-ksec2="http://docs.openstack.org/identity/api/ext/OS-KSEC2/v1.0"
           xmlns:rax-auth="http://docs.rackspace.com/identity/api/ext/RAX-AUTH/v1.0">
  <endpoint id="1"
            type="object-store"
            name="swift"
            region="\${region}"
            publicURL="http://\${endpointUrl}:\${originServicePort}/\${tenant}"
            internalURL="http://\${endpointUrl}:\${originServicePort}/\${tenant}"
            adminURL="http://\${endpointUrl}:\${originServicePort}/\${tenant}"
            tenantId="\${tenant}"/>
  <endpoint id="2"
            type="compute"
            name="nova_compat"
            region="\${region}"
            publicURL="http://\${endpointUrl}:\${originServicePort}/\${tenant}"
            internalURL="http://\${endpointUrl}:\${originServicePort}/\${tenant}"
            adminURL="http://\${endpointUrl}:\${originServicePort}/\${tenant}"
            tenantId="\${tenant}"/>
    <endpoint id="3"
            type="service"
            name="OpenStackService"
            region="\${region}"
            publicURL="http://\${endpointUrl}:\${originServicePort}/\${tenant}"
            internalURL="http://\${endpointUrl}:\${originServicePort}/\${tenant}"
            adminURL="http://\${endpointUrl}:\${originServicePort}/\${tenant}"
            tenantId="\${tenant}"/>
</endpoints>"""

    // TODO: Replace this with builder
    def identityEndpointXmlAppendedTemplate = """\
<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<endpoints xmlns="http://docs.openstack.org/identity/api/v2.0"
           xmlns:ns2="http://www.w3.org/2005/Atom"
           xmlns:os-ksadm="http://docs.openstack.org/identity/api/ext/OS-KSADM/v1.0"
           xmlns:rax-ksqa="http://docs.rackspace.com/identity/api/ext/RAX-KSQA/v1.0"
           xmlns:rax-kskey="http://docs.rackspace.com/identity/api/ext/RAX-KSKEY/v1.0"
           xmlns:os-ksec2="http://docs.openstack.org/identity/api/ext/OS-KSEC2/v1.0"
           xmlns:rax-auth="http://docs.rackspace.com/identity/api/ext/RAX-AUTH/v1.0">
  <endpoint id="2"
            type="compute"
            name="nova_compat"
            region="\${region}"
            publicURL="http://\${endpointUrl}:\${originServicePort}/v1/\${tenant}"
            internalURL="http://\${endpointUrl}:\${originServicePort}/v1/\${tenant}"
            adminURL="http://\${endpointUrl}:\${originServicePort}/v1/\${tenant}"
            tenantId="\${tenant}"/>
   <endpoint id="3"
            type="service"
            name="OpenStackService"
            region="\${region}"
            publicURL="http://\${endpointUrl}:\${originServicePort}/v1/appended/\${tenant}"
            internalURL="http://\${endpointUrl}:\${originServicePort}/v1/\${tenant}"
            adminURL="http://\${endpointUrl}:\${originServicePort}/v1/\${tenant}"
            tenantId="\${tenant}"/>
</endpoints>"""

    // TODO: Replace this with builder
    def rackerTokenXmlTemplate = """\
<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<access xmlns="http://docs.openstack.org/identity/api/v2.0"
    xmlns:ns2="http://www.w3.org/2005/Atom"
    xmlns:os-ksadm="http://docs.openstack.org/identity/api/ext/OS-KSADM/v1.0"
    xmlns:rax-ksqa="http://docs.rackspace.com/identity/api/ext/RAX-KSQA/v1.0"
    xmlns:rax-kskey="http://docs.rackspace.com/identity/api/ext/RAX-KSKEY/v1.0"
    xmlns:os-ksec2="http://docs.openstack.org/identity/api/ext/OS-KSEC2/v1.0"
    xmlns:rax-auth="http://docs.rackspace.com/identity/api/ext/RAX-AUTH/v1.0">
    <token id="\${token}"
           expires="\${expires}">
        <tenant id="\${tenantid}" name="\${tenantid}"/>
        <rax-auth:authenticatedBy>
            <rax-auth:credential>PASSWORD</rax-auth:credential>
        </rax-auth:authenticatedBy>
    </token>
    <user id="\${userid}" name="\${username}" rax-auth:defaultRegion="DFW" \${contactIdXml}>
        <roles>
            <role id="9" name="Racker"
                description="Defines a user as being a Racker"
                serviceId="18e7a7032733486cd32f472d7bd58f709ac0d221"/>
            <role id="5" name="object-store:default"
                description="Role to access keystone service"
                serviceId="18e7a7032733486cd32f472d7bd58f709ac0d221"
                tenantId="\${tenantidtwo}"/>
            <role id="6" name="compute:default"
                description="Role to access keystone service"
                serviceId="18e7a7032733486cd32f472d7bd58f709ac0d221"
                tenantId="\${tenantid}"/>
            <role id="3" name="identity:user-admin"
                description="User Admin Role"
                serviceId="18e7a7032733486cd32f472d7bd58f709ac0d221"/>
            <role name="dl_RackUSA"/>
            <role name="dl_RackGlobal"/>
            <role name="dl_cloudblock"/>
            <role name="dl_US Managers"/>
            <role name="DL_USManagers"/>
        </roles>
    </user>
</access>
"""

    def rackerTokenWithoutProperRoleXmlTemplate = """\
<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<access xmlns="http://docs.openstack.org/identity/api/v2.0"
    xmlns:ns2="http://www.w3.org/2005/Atom"
    xmlns:os-ksadm="http://docs.openstack.org/identity/api/ext/OS-KSADM/v1.0"
    xmlns:rax-ksqa="http://docs.rackspace.com/identity/api/ext/RAX-KSQA/v1.0"
    xmlns:rax-kskey="http://docs.rackspace.com/identity/api/ext/RAX-KSKEY/v1.0"
    xmlns:os-ksec2="http://docs.openstack.org/identity/api/ext/OS-KSEC2/v1.0"
    xmlns:rax-auth="http://docs.rackspace.com/identity/api/ext/RAX-AUTH/v1.0">
    <token id="\${token}"
           expires="\${expires}">
        <tenant id="\${tenantid}" name="\${tenantname}"/>
        <rax-auth:authenticatedBy>
            <rax-auth:credential>PASSWORD</rax-auth:credential>
        </rax-auth:authenticatedBy>
    </token>
    <user id="rackerUsername">
        <roles>
            <role name="dl_RackUSA"/>
            <role name="dl_RackGlobal"/>
            <role name="dl_cloudblock"/>
            <role name="dl_US Managers"/>
            <role name="DL_USManagers"/>
        </roles>
    </user>
</access>
"""

    def rackerSuccessfulValidateRespXmlTemplate = """\
<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<access xmlns="http://docs.openstack.org/identity/api/v2.0"
    xmlns:ns2="http://www.w3.org/2005/Atom"
    xmlns:os-ksadm="http://docs.openstack.org/identity/api/ext/OS-KSADM/v1.0"
    xmlns:rax-ksqa="http://docs.rackspace.com/identity/api/ext/RAX-KSQA/v1.0"
    xmlns:rax-kskey="http://docs.rackspace.com/identity/api/ext/RAX-KSKEY/v1.0"
    xmlns:os-ksec2="http://docs.openstack.org/identity/api/ext/OS-KSEC2/v1.0"
    xmlns:rax-auth="http://docs.rackspace.com/identity/api/ext/RAX-AUTH/v1.0">
    <token id="\${token}"
        expires="\${expires}"/>
    <user id="rackerSSOUsername">
        <roles>
            <role id="9" name="\${serviceadmin}"
                description="Defines a user as being a Racker"
                serviceId="18e7a7032733486cd32f472d7bd58f709ac0d221"/>
            <role id="100" name="dl_RackUSA" />
            <role name="dl_RackGlobal"/>
            <role name="dl_cloudblock"/>
            <role name="dl_US Managers"/>
            <role name="DL_USManagers"/>
        </roles>
    </user>
</access>
"""

    def rackerSuccessfulValidateRespJsonTemplate = """\
{
  "access": {
    "token": {
      "expires": "\${expires}",
      "id": "\${token}"
    },
    "user": {
      "RAX-AUTH:defaultRegion": "",
      "roles": [
        {
          "name": "\${serviceadmin}",
          "description": "Defines a user as being a Racker",
          "id": "9",
          "serviceId": "18e7a7032733486cd32f472d7bd58f709ac0d221"
        },
        {
          "name": "test_repose",
          "id" : "100",
          "description" : "Defines a user a repose dev",
          "serviceId": "18e7a7032733486cd32f472d7bd58f709ac0d221"
        }
      ],
      "id": "rackerSSOUsername"
    }
  }
}
"""

    def dedicatedUserSuccessfulRespXmlTemplate = """\
<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<access xmlns:atom="http://www.w3.org/2005/Atom"
        xmlns:rax-auth="http://docs.rackspace.com/identity/api/ext/RAX-AUTH/v1.0"
        xmlns="http://docs.openstack.org/identity/api/v2.0"
        xmlns:ns4="http://docs.rackspace.com/identity/api/ext/RAX-KSGRP/v1.0"
        xmlns:rax-ksqa="http://docs.rackspace.com/identity/api/ext/RAX-KSQA/v1.0"
        xmlns:os-ksadm="http://docs.openstack.org/identity/api/ext/OS-KSADM/v1.0"
        xmlns:rax-kskey="http://docs.rackspace.com/identity/api/ext/RAX-KSKEY/v1.0"
        xmlns:os-ksec2="http://docs.openstack.org/identity/api/ext/OS-KSEC2/v1.0">
    <token id="\${token}" expires="\${expires}">
        <rax-auth:authenticatedBy>
            <rax-auth:credential>PASSWORD</rax-auth:credential>
        </rax-auth:authenticatedBy>
    </token>
    <user id="dedicatedUser" name="dedicated_29502_1099363" rax-auth:defaultRegion="ORD" \${contactIdXml}>
        <roles>
            <role id="10015582"
                  name="monitoring:admin"
                  description="Monitoring Admin Role for Account User"
                  serviceId="bde1268ebabeeabb70a0e702a4626977c331d5c4"
                  tenantId="\${tenantid}" rax-auth:propagate="false"/>
            <role id="16"
                  name="dedicated:default"
                  description="a role that allows a user access to dedicated service methods"
                  serviceId="bde1268ebabeeabb70a0e702a4626977c331d5c4"
                  tenantId="\${tenantid}"
                  rax-auth:propagate="true"/>
            <role id="2"
                  name="identity:default"
                  description="Default Role."
                  serviceId="bde1268ebabeeabb70a0e702a4626977c331d5c4"
                  rax-auth:propagate="false"/>
        </roles>
    </user>
</access>
"""

    def dedicatedUserSuccessfulRespJsonTemplate = """\
{
  "access": {
    "token": {
        "id": "\${token}",
        "expires": "\${expires}",
        "RAX-AUTH:authenticatedBy": [
            "PASSWORD"
        ]
    },
    "user": {
      "id": "dedicatedUser",
      "roles": [
        {
          "tenantId" : "\${tenantid}",
          "id": "10015582",
          "serviceId": "bde1268ebabeeabb70a0e702a4626977c331d5c4",
          "description": "Monitoring Admin Role for Account User",
          "name": "monitoring:admin"
        },
        {
          "tenantId" : "\${tenantid}",
          "id": "16",
          "serviceId": "bde1268ebabeeabb70a0e702a4626977c331d5c4",
          "description": "a role that allows a user access to dedicated service methods",
          "name": "dedicated:default"
        },
        {
          "id": "2",
          "serviceId": "bde1268ebabeeabb70a0e702a4626977c331d5c4",
          "description": "Default Role.",
          "name": "identity:default"
        }
      ],
      \${contactIdJson}
      "name": "dedicated_29502_1099363",
      "RAX-AUTH:defaultRegion": "ORD"
    }
  }
}
"""

    def UserGlobalRolesXmlTemplate = """\
<?xml version="1.0" encoding="UTF-8"?>
  <roles
    xmlns:atom="http://www.w3.org/2005/Atom"
    xmlns:rax-auth="http://docs.rackspace.com/identity/api/ext/RAX-AUTH/v1.0"
    xmlns="http://docs.openstack.org/identity/api/v2.0"
    xmlns:ns4="http://docs.rackspace.com/identity/api/ext/RAX-KSGRP/v1.0"
    xmlns:rax-ksqa="http://docs.rackspace.com/identity/api/ext/RAX-KSQA/v1.0"
    xmlns:os-ksadm="http://docs.openstack.org/identity/api/ext/OS-KSADM/v1.0"
    xmlns:rax-kskey="http://docs.rackspace.com/identity/api/ext/RAX-KSKEY/v1.0"
    xmlns:os-ksec2="http://docs.openstack.org/identity/api/ext/OS-KSEC2/v1.0">
    <role id="9" name="Racker"
        description="Defines a user as being a Racker"
        serviceId="18e7a7032733486cd32f472d7bd58f709ac0d221" rax-auth:propagate="true"/>
    <role id="5" name="object-store:default"
        description="Role to access keystone service"
        serviceId="18e7a7032733486cd32f472d7bd58f709ac0d221"/>
    <role id="6" name="compute:default"
        description="Role to access keystone service"
        serviceId="18e7a7032733486cd32f472d7bd58f709ac0d221""/>
    <role id="3" name="identity:user-admin"
        description="User Admin Role"
        serviceId="18e7a7032733486cd32f472d7bd58f709ac0d221"/>
    \${addRolesXml}
</roles>
"""

    def UserGlobalRolesJsonTemplate = """\
{
    "roles": [
        {
            "description": "Defines a user as being Racker",
            "id": "9",
            "name": "Racker",
            "serviceId": "18e7a7032733486cd32f472d7bd58f709ac0d221"
        },
        {
            "description": "Role to access keystone service",
            "id": "5",
            "name": "object-store:default",
            "serviceId": "18e7a7032733486cd32f472d7bd58f709ac0d221"
        },
        {
            "description": "Role to access keystone service",
            "id": "6",
            "name": "compute:default",
            "serviceId": "18e7a7032733486cd32f472d7bd58f709ac0d221"
        },
        {
            "description": "Admin role for database service",
            "id": "3",
            "name": "User Admin Role",
            "serviceId": "18e7a7032733486cd32f472d7bd58f709ac0d221"
        },
        \${addRolesJson}
"""

    static final String IDP_NO_RESULTS = """\
{
    "RAX-AUTH:identityProviders": []
}"""

    static final String DEFAULT_MAPPING_VALUE = "{D}"

    static final String DEFAULT_MAPPING_POLICY = """\
{
    "mapping": {
        "version": "RAX-1",
        "description": "Default mapping policy",
        "rules": [
            {
                "local": {
                    "user": {
                        "domain": "{D}",
                        "email": "{D}",
                        "expire": "{D}",
                        "name": "{D}",
                        "roles": "{D}"
                    }
                }
            }
        ]
    }
}"""
}
