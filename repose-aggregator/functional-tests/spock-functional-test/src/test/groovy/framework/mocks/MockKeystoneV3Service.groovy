package framework.mocks

import groovy.text.SimpleTemplateEngine
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import org.joda.time.format.DateTimeFormat
import org.joda.time.format.DateTimeFormatter
import org.rackspace.deproxy.Request
import org.rackspace.deproxy.Response

import javax.xml.transform.stream.StreamSource
import javax.xml.validation.Schema
import javax.xml.validation.SchemaFactory
import javax.xml.validation.Validator
import java.util.concurrent.atomic.AtomicInteger

/**
 * Created by jennyvo on 8/8/14
 * Simulates responses from an Identity Service for Keystone V3.
 */
class MockKeystoneV3Service {

    public MockKeystoneV3Service(int identityPort) {

        resetHandlers()

        this.port = identityPort

        SchemaFactory factory = SchemaFactory.newInstance("http://www.w3.org/XML/XMLSchema/v1.1")

        factory.setFeature("http://apache.org/xml/features/validation/cta-full-xpath-checking", true)
        Schema schema = factory.newSchema(
                new StreamSource(MockKeystoneV3Service.class.getResourceAsStream("/schema/openstack/credentials.xsd")))


        this.validator = schema.newValidator()
    }

    int port

    final String DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss'Z'"
    boolean isTokenValid = true

    protected AtomicInteger _validateTokenCount = new AtomicInteger(0)
    protected AtomicInteger _generateTokenCount = new AtomicInteger(0)
    protected AtomicInteger _getGroupsCount = new AtomicInteger(0)
    protected AtomicInteger _getProjectsCount = new AtomicInteger(0)
    protected AtomicInteger _getEndpointsCount = new AtomicInteger(0)
    protected AtomicInteger _getUserRolesOnDomainCount = new AtomicInteger(0)

    void resetCounts() {

        _validateTokenCount.set(0)
        _getGroupsCount.set(0)
        _getProjectsCount.set(0)
        _generateTokenCount.set(0)
        _getEndpointsCount.set(0)
        _getUserRolesOnDomainCount.set(0)
    }

    public int getValidateTokenCount() {
        return _validateTokenCount.get()
    }

    public int getGetGroupsCount() {
        return _getGroupsCount.get()

    }

    public int getGenerateTokenCount() {
        return _generateTokenCount.get()

    }

    public int getGetEndpointsCount() {
        return _getEndpointsCount.get()

    }

    public int getGetUserRolesOnDomainCount() {
        return _getUserRolesOnDomainCount.get()

    }

    /*
     * The tokenExpiresAt field determines when the token expires. Consumers of
     * this class should set to a particular DateTime (for example, to test
     * some aspect of expiration dates), or leave it null to default to now
     * plus one day.
     *
     */
    def tokenExpiresAt = null

    void resetHandlers() {

        handler = this.&handleRequest
        validateTokenHandler = this.&validateToken
        getGroupsHandler = this.&getGroups
        getProjectsHandler = this.&getProjects
        generateTokenHandler = this.&generateToken
        getUserRolesOnDomainHandler = this.&getUserRolesOnDomain
        //getEndpointsHandler = this.&getEndpoints
    }

    Closure<Response> validateTokenHandler
    Closure<Response> getGroupsHandler
    Closure<Response> getProjectsHandler
    Closure<Response> generateTokenHandler
    //Closure<Response> getEndpointsHandler
    Closure<Response> getUserRolesOnDomainHandler

    def client_token = 'this-is-the-token'
    def client_domainid = 123456
    def client_domainname = 'this-is-the-domain'
    def client_username = 'username'
    def client_userid = 12345
    def client_projectid = 1234567
    def client_projectname = "this-is-the-project"
    def admin_domainid = 'this-is-the-admin-domain'
    def admin_domainname = 'example.com'
    def admin_token = 'this-is-the-admin-token'
    def admin_project = 'this-is-the-admin-project'
    def admin_username = 'admin_username'
    def service_admin_role = 'service:admin-role1'
    def endpointUrl = "localhost"
    def admin_userid = 67890
    Validator validator

    def templateEngine = new SimpleTemplateEngine()

    def handler = { Request request -> return handleRequest(request) }

    Response handleRequest(Request request) {
        /*
         * From http://developer.openstack.org/api-ref-identity-v3.html
         *
         * POST
         * /v3/auth/tokens
         * Authenticates and generates a token.
         *
         * GET
         * /v3/auth/tokens
         * Validates a specified token.
         *
         * GET
         * /v3/users/{userId}/groups
         * get user groups
         *
         * GET
         * /v3/users/{userId}/projects
         * get user projects
         *
         * GET
         * /v3/domain/{domainId}/users/{userId/roles
         * get user roles on domain
         *
         * GET
         * /v3/project/{projectId}/users/{userId/roles
         * get user roles on project
         *
         */

        def path = request.path
        def method = request.method

        String nonQueryPath
        String query

        if (path.contains("?")) {
            int index = path.indexOf("?")
            query = path.substring(index + 1)
            nonQueryPath = path.substring(0, index)
        } else {
            query = null
            nonQueryPath = path
        }

        if (isTokenCallPath(nonQueryPath)) {
            if (method == "POST") {
                _generateTokenCount.incrementAndGet()
                return generateTokenHandler(request)
            } else if (method == 'GET') {
                _validateTokenCount.incrementAndGet()
                def tokenId = request.getHeaders().getFirstValue("X-Subject-Token")
                return validateTokenHandler(tokenId, request)
            } else {
                return new Response(405)
            }

        } else if (nonQueryPath.startsWith("/v3/users/")) {

            if (isGetGroupsCallPath(nonQueryPath)) {
                if (method == "GET") {
                    _getGroupsCount.incrementAndGet()
                    def match = (nonQueryPath =~ getGroupsCallPathRegex)
                    def userId = match[0][1]
                    return getGroupsHandler(userId, request)
                } else {
                    return new Response(405)
                }
            }

            if (isGetUserProjectsCallPath(nonQueryPath)){
                if (method == "GET") {
                    _getProjectsCount.incrementAndGet()
                    def match = (nonQueryPath =~ getGroupsCallPathRegex)
                    def userId = match[0][1]
                    return getProjectsHandler(userId, request)
                } else {
                    return new Response(405)
                }
            }

        } else if (nonQueryPath.startsWith("/v3/domain/")) {

            if (isGetUserRolesOnDomainCallPath(nonQueryPath)) {
                if (method == "GET") {
                    _getUserRolesOnDomainCount.incrementAndGet()
                    def match = (nonQueryPath =~ getUserRolesOnDomainCallPathRegex)
                    def domainId = match[0][1]
                    def userId = match[0][2]
                    return getUserRolesOnDomain(domainId, userId, request)
                } else {
                    return new Response(405)
                }
            }

        } else if (nonQueryPath.startsWith("/v3/project/")) {

            if (isGetUserRolesOnProjectCallPath(nonQueryPath)) {
                if (method == "GET") {
                    _getUserRolesOnDomainCount.incrementAndGet()
                    def match = (nonQueryPath =~ getUserRolesOnProjectCallPathRegex)
                    def projectId = match[0][1]
                    def userId = match[0][2]
                    return getUserRolesOnDomain(projectId, userId, request)
                } else {
                    return new Response(405)
                }
            }
        }
        return new Response(501)
    }

    static final String getUserRolesOnDomainCallPathRegex = /^\/domain\/([^\/]+)\/users\/([^\/]+)\/roles/
    static final String getGroupsCallPathRegex = /^\/users\/([^\/]+)\/groups/
    //static final String getEndpointsCallPathRegex = /^\/tokens\/([^\/]+)\/endpoints/
    static final String getProjectsCallPathRegex = /^\\/users\\/([^\\/]+)\\/projects/
    static final String getUserRolesOnProjectCallPathRegex = /^\/project\/([^\/]+)\/users\/([^\/]+)\/roles/

    public static boolean isGetUserRolesOnDomainCallPath(String nonQueryPath) {
        return nonQueryPath ==~ getUserRolesOnDomainCallPathRegex
    }

    public static boolean isGetGroupsCallPath(String nonQueryPath) {
        return nonQueryPath ==~ getGroupsCallPathRegex
    }

    public static boolean isGetUserProjectsCallPath(String nonQueryPath) {
        return nonQueryPath ==~ getProjectsCallPathRegex
    }

    public static boolean isGetUserRolesOnProjectCallPath(String nonQueryPath) {
        return nonQueryPath ==~ getUserRolesOnProjectCallPathRegex
    }

    public static boolean isTokenCallPath(String nonQueryPath) {
        return nonQueryPath.startsWith("/v3/auth/tokens")
    }

    String getIssued() {
        return new DateTime()
    }
    String getExpires() {
        if (this.tokenExpiresAt != null && this.tokenExpiresAt instanceof String) {
            return this.tokenExpiresAt
        } else if (this.tokenExpiresAt instanceof DateTime) {
            DateTimeFormatter fmt = DateTimeFormat.forPattern(DATE_FORMAT).withLocale(Locale.US).withZone(DateTimeZone.UTC)
            return fmt.print(tokenExpiresAt)
        } else if (this.tokenExpiresAt) {
            return this.tokenExpiresAt.toString()
        } else {
            def now = new DateTime()
            def nowPlusOneDay = now.plusDays(1)
            return nowPlusOneDay
        }
    }
    Response validateToken(String tokenId, Request request) {
        def path = request.getPath()
        def request_token = tokenId

        def params = [
                expires     : getExpires(),
                issued      : getIssued(),
                userid      : client_userid,
                username    : client_username,
                projectid   : client_projectid,
                projectname : client_projectname,
                domainid    : client_domainid,
                domainname  : client_domainname,
                serviceadmin: service_admin_role
        ]
        def code
        def template
        def headers = [:]
        headers.put('Content-type', 'application/json')
        headers.put('X-Subject-Token', request_token)

        if (isTokenValid) {
            code = 200
            template = identitySuccessJsonFullRespTemplate

        } else {
            template = identityFailureJsonRespTemplate
        }

        def body = templateEngine.createTemplate(template).make(params)

        return new Response(code, null, headers, body)
    }

    Response generateToken(Request request) {
        try {

            // TODO: Validate what we need is present in the JSON request
        } catch (Exception e) {

            println("Admin token XSD validation error: " + e)
            return new Response(400)
        }

        def params = [
                expires     : getExpires(),
                issued      : getIssued(),
                userid      : client_userid,
                username    : client_username,
                projectid   : client_projectid,
                projectname : client_projectname,
                domainid    : client_domainid,
                domainname  : client_domainname,
                serviceadmin: service_admin_role
        ]


        def code
        def template
        def headers = [:]

        headers.put('Content-type', 'application/json')

        if (isTokenValid) {
            code = 200;
            template = identitySuccessJsonFullRespTemplate
            headers.put('X-Subject-Token', admin_token)
        } else {
            code = 404
            template = identityFailureAuthJsonRespTemplate
        }

        def body = templateEngine.createTemplate(template).make(params)

        return new Response(code, null, headers, body)
    }

    //get user groups
    Response getGroups(String userId, Request request) {

        def params = [
                expires     : getExpires(),
                issued      : getIssued(),
                userid      : client_userid,
                username    : client_username,
                domainid    : client_domainid,
                domainname  : client_domainname,
                token       : request.getHeaders().getFirstValue("X-Auth-Token"),
                serviceadmin: service_admin_role

        ]

        def template
        def headers = [:]

        headers.put('Content-type', 'application/json')
        template = getUserGroupsJsonTemplate

        def body = templateEngine.createTemplate(template).make(params)

        return new Response(200, null, headers, body)
    }

    //get user groups
    Response getProjects(String userId, Request request) {

        def params = [
                expires     : getExpires(),
                userid      : client_userid,
                domainid    : client_domainid,
                token       : request.getHeaders().getFirstValue("X-Auth-Token"),
                serviceadmin: service_admin_role

        ]

        def template
        def headers = [:]

        headers.put('Content-type', 'application/json')
        template = getUserProjectsJsonTemplate

        def body = templateEngine.createTemplate(template).make(params)

        return new Response(200, null, headers, body)
    }

    //Get user roles on domain
    Response getUserRolesOnDomain(String domainId, String userId, Request request) {
        def params = [
                userid      : client_userid,
                domainid    : client_domainid,
                token       : request.getHeaders().getFirstValue("X-Auth-Token"),
                serviceadmin: service_admin_role
        ]
        def template
        def headers = [:]

        headers.put('Content-type', 'application/json')
        template = this.getUserRolesOnDomainJsonTemplate

        def body = templateEngine.createTemplate(template).make(params)
        return new Response(200, null, headers, body)
    }

    // successful authenticate response /v3/auth/tokens?nocatalog
    def identitySuccessJsonRespTemplate = """
    {
        "token": {
            "expires_at": "\${expires}",
            "issued_at": "\${issued}",
            "methods": [
                "password"
            ],
            "user": {
                "domain": {
                    "id": "\${domainid}",
                    "links": {
                        "self": "http://identity:35357/v3/domains/\${domainid}"
                    },
                    "name": "example.com"
                },
                "id": "\${userid}",
                "links": {
                    "self": "http://identity:35357/v3/users/\${userid}"
                },
                "name": "\${username}"
            }
        }
    }
    """
    // this is full response with service catalog /v3/auth/tokens
    def identitySuccessJsonFullRespTemplate = """
    {
        "token": {
            "catalog": [
                {
                    "endpoints": [
                        {
                            "id": "39dc322ce86c4111b4f06c2eeae0841b",
                            "interface": "public",
                            "region": "RegionOne",
                            "url": "http://localhost:5000"
                        },
                        {
                            "id": "ec642f27474842e78bf059f6c48f4e99",
                            "interface": "internal",
                            "region": "RegionOne",
                            "url": "http://localhost:5000"
                        },
                        {
                            "id": "c609fc430175452290b62a4242e8a7e8",
                            "interface": "admin",
                            "region": "RegionOne",
                            "url": "http://localhost:35357"
                        }
                    ],
                    "id": "4363ae44bdf34a3981fde3b823cb9aa2",
                    "type": "identity",
                    "name": "keystone"
                }
            ],
            "expires_at": "\${expires}",
            "issued_at": "2013-02-27T16:30:59.999999Z",
            "methods": [
                "password"
            ],
            "project": {
                "domain": {
                    "id": "\${domainid}",
                    "links": {
                        "self": "http://identity:35357/v3/domains/\${domainid}"
                    },
                    "name": "\${domainname}"
                },
                "id": "\${projectid}",
                "links": {
                    "self": "http://identity:35357/v3/projects/\${projectid}"
                },
                "name": "\${projectname}"
            },
            "roles": [
                {
                    "id": "\${serviceadmin}",
                    "links": {
                        "self": "http://identity:35357/v3/roles/76e72a"
                    },
                    "name": "admin"
                },
                {
                    "id": "f4f392",
                    "links": {
                        "self": "http://identity:35357/v3/roles/f4f392"
                    },
                    "name": "member"
                }
            ],
            "user": {
                "domain": {
                    "id": "\${domainid}",
                    "links": {
                        "self": "http://identity:35357/v3/domains/\${domainid}"
                    },
                    "name": "\${domainname}"
                },
                "id": "\${userid}",
                "links": {
                    "self": "http://identity:35357/v3/users/\${userid}"
                },
                "name": "\${username}"
            }
        }
    }
    """
    //identity failure authenticate response template
    def identityFailureAuthJsonRespTemplate = """
    {
        "error": {
            "code": 401,
            "identity": {
                "methods": [
                    "password",
                    "token",
                    "challenge-response"
                ]
            },
            "message": "Need to authenticate with one or more supported methods",
            "title": "Not Authorized"
        }
    }
    """

    //identity failure response template
    //TODO: double checkout is this is correct resp
    def identityFailureJsonRespTemplate = """
    {
       "itemNotFound" : {
          "message" : "Invalid Token, not found.",
          "code" : 404
       }
    }
    """

    //user group resp template
    def getUserGroupsJsonTemplate = """
    {
        "groups": [
            {
                "description": "Developers cleared for work on all general projects"
                "domain_id": "\${domainid}",
                "id": "ea167b",
                "links": {
                    "self": "https://identity:35357/v3/groups/ea167b"
                },
                "name": "Developers"
            },
            {
                "description": "Developers cleared for work on secret projects"
                "domain_id": "\${domainid}",
                "id": "a62db1",
                "links": {
                    "self": "https://identity:35357/v3/groups/a62db1"
                },
                "name": "Secure Developers"
            }
        ],
        "links": {
            "self": "http://identity:35357/v3/users/\${userid}/groups",
            "previous": null,
            "next": null
        }
    }
    """
    //user roles on domain
    def getUserRolesOnDomainJsonTemplate = """
    {
        "roles": [
            {
                "id": "--role-id--",
                "links": {
                    "self": "http://identity:35357/v3/roles/--role-id--"
                },
                "name": "--role-name--",
            },
            {
                "id": "--role-id--",
                "links": {
                    "self": "http://identity:35357/v3/roles/--role-id--"
                },
                "name": "--role-name--"
            }
        ],
        "links": {
            "self": "http://identity:35357/v3/domains/\${domainid}/users/\${userid}/roles",
            "previous": null,
            "next": null
        }
    }
    """

    //get user projects
    def getUserProjectsJsonTemplate = """
    {
        "projects": [
            {
                "domain_id": "\${domainid}",
                "enabled": true,
                "id": "263fd9",
                "links": {
                    "self": "https://identity:35357/v3/projects/263fd9"
                },
                "name": "Test Group"
            },
            {
                "domain_id": "\${domainid}",
                "enabled": true,
                "id": "50ef01",
                "links": {
                    "self": "https://identity:35357/v3/projects/50ef01"
                },
                "name": "Build Group"
            }
        ],
        "links": {
            "self": "https://identity:35357/v3/users/\${userid}/projects",
            "previous": null,
            "next": null
        }
    }
    """

    //get user roles on project
    def getUserRolesOnProjectTemplate = """
    {
        "roles": [
            {
                "id": "123454",
                "links": {
                    "self": "http://identity:35357/v3/roles/123454"
                },
                "name": "--role-name--",
            },
            {
                "id": "123455",
                "links": {
                    "self": "http://identity:35357/v3/roles/123455"
                },
                "name": "--role-name--"
            }
        ],
        "links": {
            "self": "http://identity:35357/v3/projects/\${projectid}/users/\${userid}/roles",
            "previous": null,
            "next": null
        }
    }
    """
}