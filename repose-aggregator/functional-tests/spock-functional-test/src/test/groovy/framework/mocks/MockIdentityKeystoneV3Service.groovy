package framework.mocks

import groovy.text.SimpleTemplateEngine
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
class MockIdentityKeystoneV3Service {
    public MockIdentityKeystoneV3Service(int identityPort, int originServicePort) {

        resetHandlers()

        this.port = identityPort
        this.originServicePort = originServicePort

        SchemaFactory factory = SchemaFactory.newInstance("http://www.w3.org/XML/XMLSchema/v1.1");

        factory.setFeature("http://apache.org/xml/features/validation/cta-full-xpath-checking", true);
        Schema schema = factory.newSchema(
                new StreamSource(MockIdentityService.class.getResourceAsStream("/schema/openstack/credentials.xsd")));


        this.validator = schema.newValidator();
    }

    int port
    int originServicePort

    final String DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss'Z'";
    boolean isTokenValid = true;

    protected AtomicInteger _validateTokenCount = new AtomicInteger(0);
    protected AtomicInteger _getGroupsCount = new AtomicInteger(0);
    protected AtomicInteger _generateTokenCount = new AtomicInteger(0);
    protected AtomicInteger _getEndpointsCount = new AtomicInteger(0);
    protected AtomicInteger _getUserGlobalRolesCount = new AtomicInteger(0);

    void resetCounts() {

        _validateTokenCount.set(0)
        _getGroupsCount.set(0)
        _generateTokenCount.set(0)
        _getEndpointsCount.set(0)
        _getUserGlobalRolesCount.set(0)
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

    public int getGetUserGlobalRolesCount() {
        return _getUserGlobalRolesCount.get()

    }

    /*
     * The tokenExpiresAt field determines when the token expires. Consumers of
     * this class should set to a particular DateTime (for example, to test
     * some aspect of expiration dates), or leave it null to default to now
     * plus one day.
     *
     */
    def tokenExpiresAt = null;

    void resetHandlers() {

        handler = this.&handleRequest
        validateTokenHandler = this.&validateToken
        getGroupsHandler = this.&getGroups
        generateTokenHandler = this.&generateToken
        getEndpointsHandler = this.&getEndpoints
        getUserGlobalRolesHandler = this.&getUserGlobalRoles
    }

    Closure<Response> validateTokenHandler
    Closure<Response> getGroupsHandler
    Closure<Response> generateTokenHandler
    Closure<Response> getEndpointsHandler
    Closure<Response> getUserGlobalRolesHandler

    def client_token = 'this-is-the-token';
    def client_domain = 'this-is-the-domain';
    def client_username = 'username';
    def client_userid = 12345;
    def admin_token = 'this-is-the-admin-token';
    def admin_tenant = 'this-is-the-admin-tenant';
    def admin_username = 'admin_username';
    def service_admin_role = 'service:admin-role1';
    def endpointUrl = "localhost"
    def admin_userid = 67890;
    Validator validator;

    def templateEngine = new SimpleTemplateEngine();

    def handler = { Request request -> return handleRequest(request) }

    Response handleRequest(Request request) {
        def json = false

        for (value in request.headers.findAll('Accept')) {
            if (value.contains('application/json')) {
                json = true
                break
            }
        }
        def path = request.path
        def method = request.method

        String nonQueryPath;
        String query;

        if (path.contains("?")) {
            int index = path.indexOf("?")
            query = path.substring(index + 1)
            nonQueryPath = path.substring(0, index)
        } else {
            query = null
            nonQueryPath = path
        }

        if (isTokenCallPath(nonQueryPath)) {
            if (isGenerateTokenCallPath(nonQueryPath)) {
                if (method == "POST") {
                    _generateTokenCount.incrementAndGet()
                    return generateTokenHandler(request, json);
                } else {
                    return new Response(405)
                }
            }

            if (isGetEndpointsCallPath(nonQueryPath)) {
                if (method == "GET") {
                    _getEndpointsCount.incrementAndGet()
                    def match = (nonQueryPath =~ getEndpointsCallPathRegex)
                    def tokenId = match[0][1]
                    return getEndpointsHandler(tokenId, request, json)
                } else {
                    return new Response(405)
                }
            }

            if (isValidateTokenCallPath(nonQueryPath)) {
                // TODO: 'belongsTo' in query string
                if (method == 'GET') {
                    _validateTokenCount.incrementAndGet()
                    def match = (nonQueryPath =~ validateTokenCallPathRegex)
                    def tokenId = match[0][1]
                    return validateTokenHandler(tokenId, request, json)
                } else {
                    return new Response(405)
                }
            }

        } else if (nonQueryPath.startsWith("/users/")) {

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

            if (isGetUserRolesOnDomainCallPath(nonQueryPath)) {
                if (method == "GET") {
                    _getUserGlobalRolesCount.incrementAndGet()
                    def match = (nonQueryPath =~ getUserRolesOnDomainCallPathRegex)
                    def domainId = match[0][1]
                    def userId = match[0][2]
                    return getUserRolesOnDomain(domainId, userId, request)
                } else {
                    return new Response(405)
                }
            }

            if (isGetUserProjectsCallPath(nonQueryPath)){
                if (method == "GET") {
                    _getUserProjectsCount.incrementAndGet()
                    def match = (nonQueryPath =~ getGroupsCallPathRegex)
                    def userId = match[0][1]
                    return getProjectsHandler(userId, request)
                } else {
                    return new Response(405)
                }
            }
        }

        return new Response(501);
    }

    static final String getUserRolesOnDomainCallPathRegex = /^\/domain\/([^\/]+)\/users\/([^\/]+)\/roles/
    static final String getGroupsCallPathRegex = /^\/users\/([^\/]+)\/groups/
    static final String getEndpointsCallPathRegex = /^\/tokens\/([^\/]+)\/endpoints/
    static final String validateTokenCallPathRegex = /^\/tokens\/([^\/]+)\/?$/
    static final String getProjectsCallPathRegex = /^\\/users\\/([^\\/]+)\\/projects/

    public static boolean isGetUserRolesOnDomainCallPath(String nonQueryPath) {
        return nonQueryPath ==~ getUserRolesOnDomainCallPathRegex
    }

    public static boolean isGetGroupsCallPath(String nonQueryPath) {
        return nonQueryPath ==~ getGroupsCallPathRegex
    }

    public static boolean isGetProjectsCallPath(String nonQueryPath) {
        return nonQueryPath ==~ getProjectsCallPathRegex
    }

    public static boolean isValidateTokenCallPath(String nonQueryPath) {
        return nonQueryPath ==~ validateTokenCallPathRegex
    }

    public static boolean isGetEndpointsCallPath(String nonQueryPath) {
        return nonQueryPath ==~ getEndpointsCallPathRegex
    }

    public static boolean isGenerateTokenCallPath(String nonQueryPath) {
        return nonQueryPath == "/tokens"
    }

    public static boolean isTokenCallPath(String nonQueryPath) {
        return nonQueryPath.startsWith("/tokens")
    }

    //get user groups
    Response getGroups(String userId, Request request) {

        def params = [
                expires     : getExpires(),
                userid      : client_userid,
                username    : client_username,
                domainid    : client_domain,
                token       : request.getHeaders().getFirstValue("X-Auth-Token"),
                serviceadmin: service_admin_role

        ]

        def template;
        def headers = [:];

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
                username    : client_username,
                token       : request.getHeaders().getFirstValue("X-Auth-Token"),
                serviceadmin: service_admin_role

        ]

        def template;
        def headers = [:];

        headers.put('Content-type', 'application/json')
        template = getUserProjectsJsonTemplate

        def body = templateEngine.createTemplate(template).make(params)

        return new Response(200, null, headers, body)
    }

    //Get user roles on domain
    Response getUserRolesOnDomain(String domainId, String userId, Request request) {

        def template;
        def headers = [:];

        headers.put('Content-type', 'application/json')
        template = this.getUserRolesOnDomainJsonTemplate;


        def params = [:];

        def body = templateEngine.createTemplate(template).make(params);
        return new Response(200, null, headers, body);
    }

    // token is in header and not part of response data
    def identitySuccessRespHeader = ['content-type': 'application/json',
                                    'X-Subject-Token': ${token}]

    // successful authenticate response /v3/auth/tokens?nocatalog
    def identitySuccessJsonRespTemplate = """
    {
        "token": {
            "expires_at": "\${expires}",
            "issued_at": "2013-02-27T16:30:59.999999Z",
            "methods": [
                "password"
            ],
            "user": {
                "domain": {
                    "id": "\${domainId}",
                    "links": {
                        "self": "http://identity:35357/v3/domains/\${domainId}"
                    },
                    "name": "example.com"
                },
                "id": "\${userid}",
                "links": {
                    "self": "http://identity:35357/v3/users/\${userid}"
                },
                "name": "Joe"
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
                            "id": "39dc322ce86c4111b4f06c2eeae0841b",isGetUserRolesOnDomainCallPath
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
                    "id": "\${domainId}",
                    "links": {
                        "self": "http://identity:35357/v3/domains/\${domainId}"
                    },
                    "name": "example.com"
                },
                "id": "\${projectId}",
                "links": {
                    "self": "http://identity:35357/v3/projects/\${projectId}"
                },
                "name": "project-x"
            },
            "roles": [
                {
                    "id": "76e72a",
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
                    "id": "\${domainId}",
                    "links": {
                        "self": "http://identity:35357/v3/domains/\${domainId}"
                    },
                    "name": "example.com"
                },
                "id": "\${userid}",
                "links": {
                    "self": "http://identity:35357/v3/users/\${userid}"
                },
                "name": "Joe"
            }
        }
    }
    """

    def getUserGroupsJsonTemplate = """
    {
        "groups": [
            {
                "description": "Developers cleared for work on all general projects"
                "domain_id": "\${domainId}",
                "id": "ea167b",
                "links": {
                    "self": "https://identity:35357/v3/groups/ea167b"
                },
                "name": "Developers"
            },
            {
                "description": "Developers cleared for work on secret projects"
                "domain_id": "\${domainId}",
                "id": "a62db1",
                "links": {
                    "self": "https://identity:35357/v3/groups/a62db1"
                },
                "name": "Secure Developers"
            }
        ],
        "links": {
            "self": "http://identity:35357/v3/users/9fe1d3/groups",
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
            "self": "http://identity:35357/v3/domains/--domain_id--/users/--user_id--/roles",
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
                "domain_id": "\${domainId}",
                "enabled": true,
                "id": "263fd9",
                "links": {
                    "self": "https://identity:35357/v3/projects/263fd9"
                },
                "name": "Test Group"
            },
            {
                "domain_id": "\${domainId}",
                "enabled": true,
                "id": "50ef01",
                "links": {
                    "self": "https://identity:35357/v3/projects/50ef01"
                },
                "name": "Build Group"
            }
        ],
        "links": {
            "self": "https://identity:35357/v3/users/9fe1d3/projects",
            "previous": null,
            "next": null
        }
    }
    """
}