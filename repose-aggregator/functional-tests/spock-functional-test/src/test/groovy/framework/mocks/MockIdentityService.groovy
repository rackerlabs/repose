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
 * Simulates responses from an Identity Service
 */
class MockIdentityService {

    public MockIdentityService(int identityPort, int originServicePort) {

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
    boolean checkTokenValid = false;

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
    def client_tenant = 'this-is-the-tenant';
    def client_tenant_file = 'this-is-the-nast-id'
    def client_username = 'username';
    def client_userid = 12345;
    def client_apikey = 'this-is-the-api-key';
    def admin_token = 'this-is-the-admin-token';
    def admin_tenant = 'this-is-the-admin-tenant'
    def admin_username = 'admin_username';
    def service_admin_role = 'service:admin-role1';
    def endpointUrl = "localhost"
    def admin_userid = 67890;
    def impersonate_id = ""
    def impersonate_name = ""
    Validator validator;

    def templateEngine = new SimpleTemplateEngine();

    def handler = { Request request -> return handleRequest(request) }

    // we can still use the `handler' closure even if handleRequest is overridden in a derived class
    Response handleRequest(Request request) {
        def xml = false

        for (value in request.headers.findAll('Accept')) {
            if (value.contains('application/xml')) {
                xml = true
                break
            }
        }

        /*
         * From http://docs.openstack.org/api/openstack-identity-service/2.0/content/
         *
         * POST
         * v2.0/tokens
         * Authenticates and generates a token.
         *
         * GET
         * v2.0/tokens/{tokenId}{?belongsTo}
         * tokenId : UUID - Required. The token ID.
         * Validates a token and confirms that it belongs to a specified tenant.
         *
         * GET
         * v2.0/tokens/{tokenId}/endpoints
         * tokenId : UUID - Required. The token ID.
         * Lists the endpoints associated with a specified token.
         *
         * GET
         * v2.0/users/{userId}/RAX-KSGRP
         * userId : String - The user ID.
         * X-Auth-Token : String - A valid authentication token for an administrative user.
         * List groups for a specified user.
         *
         * GET
         * users/{user_id}/roles
         * X-Auth-Token : String - A valid authentication token for an administrative user.
         * user_id : String - The user ID.
         * Lists global roles for a specified user. Excludes tenant roles.
         *
         *
         */

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
                    return generateTokenHandler(request, xml);
                } else {
                    return new Response(405)
                }
            }

            if (isGetEndpointsCallPath(nonQueryPath)) {
                if (method == "GET") {
                    _getEndpointsCount.incrementAndGet()
                    def match = (nonQueryPath =~ getEndpointsCallPathRegex)
                    def tokenId = match[0][1]
                    return getEndpointsHandler(tokenId, request, xml)
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
                    return validateTokenHandler(tokenId, request, xml)
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
                    return getGroupsHandler(userId, request, xml)
                } else {
                    return new Response(405)
                }
            }

            if (isGetUserGlobalRolesCallPath(nonQueryPath)) {
                if (method == "GET") {
                    _getUserGlobalRolesCount.incrementAndGet()
                    def match = (nonQueryPath =~ getUserGlobalRolesCallPathRegex)
                    def userId = match[0][1]
                    return getUserGlobalRoles(userId, request, xml)
                } else {
                    return new Response(405)
                }
            }
        }

        return new Response(501);
    }

    static final String getUserGlobalRolesCallPathRegex = /^\/users\/([^\/]+)\/roles/
    static final String getGroupsCallPathRegex = /^\/users\/([^\/]+)\/RAX-KSGRP/
    static final String getEndpointsCallPathRegex = /^\/tokens\/([^\/]+)\/endpoints/
    static final String validateTokenCallPathRegex = /^\/tokens\/([^\/]+)\/?$/

    public static boolean isGetUserGlobalRolesCallPath(String nonQueryPath) {
        return nonQueryPath ==~ getUserGlobalRolesCallPathRegex
    }

    public static boolean isGetGroupsCallPath(String nonQueryPath) {
        return nonQueryPath ==~ getGroupsCallPathRegex
    }

    public static boolean isGetEndpointsCallPath(String nonQueryPath) {
        return nonQueryPath ==~ getEndpointsCallPathRegex
    }

    public static boolean isValidateTokenCallPath(String nonQueryPath) {
        return nonQueryPath ==~ validateTokenCallPathRegex
    }

    public static boolean isGenerateTokenCallPath(String nonQueryPath) {
        return nonQueryPath == "/tokens"
    }

    public static boolean isTokenCallPath(String nonQueryPath) {
        return nonQueryPath.startsWith("/tokens")
    }


    String getExpires() {

        if (this.tokenExpiresAt != null && this.tokenExpiresAt instanceof String) {

            return this.tokenExpiresAt;

        } else if (this.tokenExpiresAt instanceof DateTime) {

            DateTimeFormatter fmt = DateTimeFormat.forPattern(DATE_FORMAT).withLocale(Locale.US).withZone(DateTimeZone.UTC);
            return fmt.print(tokenExpiresAt)

        } else if (this.tokenExpiresAt) {

            return this.tokenExpiresAt.toString();

        } else {

            def now = new DateTime()
            def nowPlusOneDay = now.plusDays(1)
            return nowPlusOneDay;
        }
    }

    Response validateToken(String tokenId, Request request, boolean xml) {
        def path = request.getPath()
        def request_token = tokenId
        def impersonateid = impersonate_id

        def params = [
                expires     : getExpires(),
                userid      : client_userid,
                username    : client_username,
                tenant      : client_tenant,
                tenanttwo   : client_tenant_file,
                token       : request_token,
                serviceadmin: service_admin_role,
                impersonateid: impersonate_id,
                impersonatename: impersonate_name
        ];

        def code;
        def template;
        def headers = [:];

        if (xml) {
            headers.put('Content-type', 'application/xml')
        } else {
            headers.put('Content-type', 'application/json')
        }

        if (isTokenValid) {
            code = 200;
            if (xml) {
                if (tokenId == "rackerButts") {
                    template = rackerTokenXmlTemplate
                } else if (tokenId == "failureRacker") {
                    template = rackerTokenWithoutProperRoleXmlTemplate
                } else if (impersonateid != "") {
                    template = identityImpersonateSuccessfulXmlTemplate
                } else {
                    template = identitySuccessXmlTemplate
                }
            } else {
                if (impersonateid != "") {
                    template = identityImpersonateSuccessfulJsonTemplate
                } else {
                    template = identitySuccessJsonTemplate
                }
            }
        } else {
            code = 404
            if (xml) {
                template = identityFailureXmlTemplate
            } else {
                template = identityFailureJsonTemplate
            }
        }

        def body = templateEngine.createTemplate(template).make(params)

        return new Response(code, null, headers, body)
    }

    Response getGroups(String userId, Request request, boolean xml) {

        def params = [
                expires     : getExpires(),
                userid      : client_userid,
                username    : client_username,
                tenant      : client_tenant,
                token       : request.getHeaders().getFirstValue("X-Auth-Token"),
                serviceadmin: service_admin_role

        ]

        def template;
        def headers = [:];

        if (xml) {
            headers.put('Content-type', 'application/xml')
        } else {
            headers.put('Content-type', 'application/json')
        }

        if (xml) {
            template = groupsXmlTemplate
        } else {
            template = groupsJsonTemplate
        }

        def body = templateEngine.createTemplate(template).make(params)

        return new Response(200, null, headers, body)
    }

    Response generateToken(Request request, boolean xml) {

        // Since the SchemaFactory does not appear to import parent XSD's,
        // the validation is skipped for the API Key Credentials that are defined externally.
        if(xml && !(request.body.toString().contains("apiKeyCredentials"))) {
            try {
                final StreamSource sampleSource = new StreamSource(new ByteArrayInputStream(request.body.getBytes()));
                validator.validate(sampleSource);
            } catch (Exception e) {
                println("Admin token XSD validation error: " + e);
                return new Response(400);
            }
        }

        def params

        def isTokenChecked = true
        // IF the body of the request should be evaluated to determine the validity of the Token, THEN ...
        // ELSE the just use the isTokenValid value.
        if(checkTokenValid) {
            // IF the body is a Client userName/apiKey request,
            // THEN return the Client token response;
            // ELSE /*IF the body is userName/passWord request*/,
            // THEN return the Admin token response.
            if (request.body.contains("username") &&
                    request.body.contains(client_username) &&
                    request.body.contains("apiKey") &&
                    request.body.contains(client_apikey)) {
                params = [
                        expires     : getExpires(),
                        userid      : client_userid,
                        username    : client_username,
                        tenant      : client_tenant,
                        tenanttwo   : client_tenant,
                        token       : client_token,
                        serviceadmin: service_admin_role
                ];
            } else if (request.body.contains("username") &&
                    request.body.contains(admin_username) /*&&
                request.body.contains("password") &&
                request.body.contains(admin_password)*/) {
                params = [
                        expires     : getExpires(),
                        userid      : admin_userid,
                        username    : admin_username,
                        tenant      : admin_tenant,
                        tenanttwo   : admin_tenant,
                        token       : admin_token,
                        serviceadmin: service_admin_role
                ];
            } else {
                isTokenChecked = false
            }
        } else {
            params = [
                    expires     : getExpires(),
                    userid      : admin_userid,
                    username    : admin_username,
                    tenant      : admin_tenant,
                    tenanttwo   : admin_tenant,
                    token       : admin_token,
                    serviceadmin: service_admin_role
            ];
        }

        def code;
        def template;
        def headers = [:];

        if (xml) {
            headers.put('Content-type', 'application/xml')
        } else {
            headers.put('Content-type', 'application/json')
        }

        if (isTokenValid && isTokenChecked) {
            code = 200;
            if (xml) {
                template = identitySuccessXmlTemplate
            } else {
                template = identitySuccessJsonTemplate
            }
        } else {
            code = 401
            if (xml) {
                template = identityFailureXmlTemplate
            } else {
                template = identityFailureJsonTemplate
            }
        }

        def body = templateEngine.createTemplate(template).make(params)

        return new Response(code, null, headers, body)
    }

    Response getEndpoints(String tokenId, Request request, boolean xml) {

        def code;
        def template;
        def headers = [:];

        if (xml) {
            headers.put('Content-type', 'application/xml')
            template = this.identityEndpointXmlTemplate
        } else {
            headers.put('Content-type', 'application/json')
            template = this.identityEndpointJsonTemplate
        }

        def params = [
                'identityPort'     : this.port,
                token              : request.getHeaders().getFirstValue("X-Auth-Token"),
                'expires'          : getExpires(),
                'userid'           : this.client_userid,
                'username'         : this.client_username,
                'tenant'           : this.client_tenant,
                'originServicePort': this.originServicePort,
                'endpointUrl'      : this.endpointUrl
        ];

        def body = templateEngine.createTemplate(template).make(params);
        return new Response(200, null, headers, body);
    }

    Response getUserGlobalRoles(String userId, Request request, boolean xml) {

        def template;
        def headers = [:];

        if (xml) {
            headers.put('Content-type', 'application/xml')
            template = this.getUserGlobalRolesXmlTemplate;
        } else {
            headers.put('Content-type', 'application/json')
            template = this.getUserGlobalRolesJsonTemplate;
        }

        def params = [:];

        def body = templateEngine.createTemplate(template).make(params);
        return new Response(200, null, headers, body);
    }


    // TODO: Replace this with builder
    def groupsJsonTemplate =
            """{
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
    def groupsXmlTemplate =
            """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<groups xmlns="http://docs.rackspace.com/identity/api/ext/RAX-KSGRP/v1.0">
    <group id="0" name="Default">
        <description>Default Limits</description>
    </group>
</groups>
"""

    // TODO: Replace this with builder
    def identityFailureJsonTemplate =
            """{
   "itemNotFound" : {
      "message" : "Invalid Token, not found.",
      "code" : 404
   }
}
"""

    // TODO: Replace this with builder
    def identityFailureXmlTemplate =
            """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<itemNotFound xmlns="http://docs.openstack.org/identity/api/v2.0"
              xmlns:ns2="http://docs.openstack.org/identity/api/ext/OS-KSADM/v1.0"
              code="404">
  <message>Invalid Token, not found.</message>
</itemNotFound>
"""

    // TODO: Replace this with builder
    def identitySuccessJsonTemplate =
            """{
   "access" : {
      "serviceCatalog" : [
         {
            "name": "cloudServersOpenStack",
            "type": "compute",
            "endpoints": [
            {
                "publicURL": "https://ord.servers.api.rackspacecloud.com/v2/\${tenant}",
                "region": "ORD",
                "tenantId": "\${tenant}",
                "versionId": "2",
                "versionInfo": "https://ord.servers.api.rackspacecloud.com/v2",
                "versionList": "https://ord.servers.api.rackspacecloud.com/"
            },
            {
                "publicURL": "https://dfw.servers.api.rackspacecloud.com/v2/\${tenant}",
                "region": "DFW",
                "tenantId": "\${tenant}",
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
                  "publicURL" : "https://cdn.stg.clouddrive.com/v1/\${tenanttwo}",
                  "tenantId" : "\${tenanttwo}",
                  "region" : "DFW"
               },
               {
                  "publicURL" : "https://cdn.stg.clouddrive.com/v1/\${tenanttwo}",
                  "tenantId" : "\${tenanttwo}",
                  "region" : "ORD"
               }
            ]
         },
         {
            "name" : "cloudFiles",
            "type" : "object-store",
            "endpoints" : [
               {
                  "internalURL" : "https://snet-storage.stg.swift.racklabs.com/v1/\${tenanttwo}",
                  "publicURL" : "https://storage.stg.swift.racklabs.com/v1/\${tenanttwo}",
                  "tenantId" : "\${tenanttwo}",
                  "region" : "ORD"
               },
               {
                  "internalURL" : "https://snet-storage.stg.swift.racklabs.com/v1/\${tenanttwo}",
                  "publicURL" : "https://storage.stg.swift.racklabs.com/v1/\${tenanttwo}",
                  "tenantId" : "\${tenanttwo}",
                  "region" : "DFW"
               }
            ]
         }
      ],
      "user" : {
         "roles" : [
            {
               "tenantId" : "\${tenant}",
               "name" : "compute:default",
               "id" : "684",
               "description" : "A Role that allows a user access to keystone Service methods"
            },
            {
               "tenantId" : "\${tenanttwo}",
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
            "name" : "\${tenant}",
            "id" : "\${tenant}"
         },
         "id" : "\${token}",
         "expires" : "\${expires}"
      }
   }
}
"""

    // TODO: Replace this with builder
    def identitySuccessXmlTemplate =
            """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<access xmlns="http://docs.openstack.org/identity/api/v2.0"
        xmlns:os-ksadm="http://docs.openstack.org/identity/api/ext/OS-KSADM/v1.0"
        xmlns:os-ksec2="http://docs.openstack.org/identity/api/ext/OS-KSEC2/v1.0"
        xmlns:rax-ksqa="http://docs.rackspace.com/identity/api/ext/RAX-KSQA/v1.0"
        xmlns:rax-kskey="http://docs.rackspace.com/identity/api/ext/RAX-KSKEY/v1.0">
    <token id="\${token}"
           expires="\${expires}">
        <tenant id="\${tenant}"
                name="\${tenant}"/>
    </token>
    <user xmlns:rax-auth="http://docs.rackspace.com/identity/api/ext/RAX-AUTH/v1.0"
          id="\${userid}"
          name="\${username}"
          rax-auth:defaultRegion="the-default-region">
        <roles>
            <role id="684"
                  name="compute:default"
                  description="A Role that allows a user access to keystone Service methods"
                  serviceId="0000000000000000000000000000000000000001"
                  tenantId="\${tenant}"/>
            <role id="5"
                  name="object-store:default"
                  description="A Role that allows a user access to keystone Service methods"
                  serviceId="0000000000000000000000000000000000000002"
                  tenantId="\${tenanttwo}"/>
            <role id="6"
                  name="\${serviceadmin}"
                  description="A Role that allows a user access to keystone Service methods"
                  serviceId="0000000000000000000000000000000000000002"
                  tenantId="\${tenant}"/>
        </roles>
    </user>
    <serviceCatalog>
        <service type="compute"
                 name="cloudServersOpenStack">
            <endpoint region="ORD"
                      tenantId="\${tenant}"
                      publicURL="https://ord.servers.api.rackspacecloud.com/v2/\${tenant}"
                      versionId="2"
                      versionInfo="https://ord.servers.api.rackspacecloud.com/v2"
                      versionList="https://ord.servers.api.rackspacecloud.com/"/>
            <endpoint region="DFW"
                      tenantId="\${tenant}"
                      publicURL="https://dfw.servers.api.rackspacecloud.com/v2/\${tenant}"
                      versionId="2"
                      versionInfo="https://dfw.servers.api.rackspacecloud.com/v2"
                      versionList="https://dfw.servers.api.rackspacecloud.com/"/>
        </service>
        <service type="rax:object-cdn"
                 name="cloudFilesCDN">
            <endpoint region="DFW"
                      tenantId="\${tenanttwo}"
                      publicURL="https://cdn.stg.clouddrive.com/v1/\${tenanttwo}"/>
            <endpoint region="ORD"
                      tenantId="\${tenanttwo}"
                      publicURL="https://cdn.stg.clouddrive.com/v1/\${tenanttwo}"/>
        </service>
        <service type="object-store"
                 name="cloudFiles">
            <endpoint region="ORD"
                      tenantId="\${tenanttwo}"
                      publicURL="https://storage.stg.swift.racklabs.com/v1/\${tenanttwo}"
                      internalURL="https://snet-storage.stg.swift.racklabs.com/v1/\${tenanttwo}"/>
            <endpoint region="DFW"
                      tenantId="\${tenanttwo}"
                      publicURL="https://storage.stg.swift.racklabs.com/v1/\${tenanttwo}"
                      internalURL="https://snet-storage.stg.swift.racklabs.com/v1/\${tenanttwo}"/>
        </service>
    </serviceCatalog>
</access>
"""

    // TODO: Replace this with builder
    def identityEndpointsJsonTemplate =
            """{
    "endpoints_links": [
        {
            "href": "http://localhost:\${identityPort}/tokens/\${token}/endpoints?'marker=5&limit=10'",
            "rel": "next"
        }
    ],
    "endpoints": [
        {
            "internalURL": "http://localhost:\${originServicePort}/v1/AUTH_1",
            "name": "swift",
            "adminURL": "http://localhost:\${originServicePort}/",
            "region": "RegionOne",
            "tenantId": 1,
            "type": "object-store",
            "id": 1,
            "publicURL": "http://localhost:\${originServicePort}/"
        },
        {
            "internalURL": "http://localhost:\${originServicePort}/",
            "name": "nova_compat",
            "adminURL": "http://localhost:\${originServicePort}/",
            "region": "RegionOne",
            "tenantId": 1,
            "type": "compute",
            "id": 2,
            "publicURL": "http://localhost:\${originServicePort}/"
        }
    ]
}"""

    // TODO: Replace this with builder
    def identityEndpointXmlTemplate =
            """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
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
            region="RegionOne"
            publicURL="http://\${endpointUrl}:\${originServicePort}/\${tenant}"
            internalURL="http://\${endpointUrl}:\${originServicePort}/\${tenant}"
            adminURL="http://\${endpointUrl}:\${originServicePort}/\${tenant}"
            tenantId="\${tenant}"/>
  <endpoint id="2"
            type="compute"
            name="nova_compat"
            region="RegionOne"
            publicURL="http://\${endpointUrl}:\${originServicePort}/\${tenant}"
            internalURL="http://\${endpointUrl}:\${originServicePort}/\${tenant}"
            adminURL="http://\${endpointUrl}:\${originServicePort}/\${tenant}"
            tenantId="\${tenant}"/>
</endpoints>"""

    // TODO: Replace this with builder
    // TODO: Replace this with builder

    def rackerTokenXmlTemplate =
            """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<access xmlns="http://docs.openstack.org/identity/api/v2.0"
    xmlns:ns2="http://www.w3.org/2005/Atom"
    xmlns:os-ksadm="http://docs.openstack.org/identity/api/ext/OS-KSADM/v1.0"
    xmlns:rax-ksqa="http://docs.rackspace.com/identity/api/ext/RAX-KSQA/v1.0"
    xmlns:rax-kskey="http://docs.rackspace.com/identity/api/ext/RAX-KSKEY/v1.0"
    xmlns:os-ksec2="http://docs.openstack.org/identity/api/ext/OS-KSEC2/v1.0"
    xmlns:rax-auth="http://docs.rackspace.com/identity/api/ext/RAX-AUTH/v1.0">
    <token id="\${token}"
           expires="\${expires}">
        <tenant id="\${tenant}" name="\${tenant}"/>
        <rax-auth:authenticatedBy>
            <rax-auth:credential>PASSWORD</rax-auth:credential>
        </rax-auth:authenticatedBy>
    </token>
    <user id="\${userid}" name="\${username}" rax-auth:defaultRegion="DFW">
        <roles>
            <role id="9" name="Racker"
                description="Defines a user as being a Racker"
                serviceId="18e7a7032733486cd32f472d7bd58f709ac0d221"/>
            <role id="5" name="object-store:default"
                description="Role to access keystone service"
                serviceId="18e7a7032733486cd32f472d7bd58f709ac0d221"
                tenantId="\${tenanttwo}"/>
            <role id="6" name="compute:default"
                description="Role to access keystone service"
                serviceId="18e7a7032733486cd32f472d7bd58f709ac0d221"
                tenantId="\${tenant}"/>
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

    def rackerTokenWithoutProperRoleXmlTemplate =
            """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<access xmlns="http://docs.openstack.org/identity/api/v2.0"
    xmlns:ns2="http://www.w3.org/2005/Atom"
    xmlns:os-ksadm="http://docs.openstack.org/identity/api/ext/OS-KSADM/v1.0"
    xmlns:rax-ksqa="http://docs.rackspace.com/identity/api/ext/RAX-KSQA/v1.0"
    xmlns:rax-kskey="http://docs.rackspace.com/identity/api/ext/RAX-KSKEY/v1.0"
    xmlns:os-ksec2="http://docs.openstack.org/identity/api/ext/OS-KSEC2/v1.0"
    xmlns:rax-auth="http://docs.rackspace.com/identity/api/ext/RAX-AUTH/v1.0">
    <token id="\${token}"
           expires="\${expires}">
        <tenant id="\${tenant}" name="\${tenant}"/>
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

    def identityImpersonateSuccessfulXmlTemplate = """
<?xml version="1.0" encoding="UTF-8"?>
<access xmlns="http://docs.openstack.org/identity/api/v2.0" xmlns:RAX-AUTH="http://docs.rackspace.com/identity/api/ext/RAX-AUTH/v1.0">
    <token id="\${token}" expires="\${expires}">
        <tenant id="\${tenant}" name="\${tenant}"/>
    </token>
    <user id="\${userid}" name="\${username}">
        <roles xmlns="http://docs.openstack.org/identity/api/v2.0">
            <role id="123" name="compute:admin" />
            <role id="234" name="object-store:admin" />
        </roles>
    </user>
    <RAX-AUTH:impersonator id="\${impersonateid}" username="\${impersonatename}">
        <roles xmlns="http://docs.openstack.org/identity/api/v2.0">
            <role id="123" name="Racker" />
            <role id="234" name="object-store:admin" />
        </roles>
    </RAX-AUTH:impersonator>
</access>
"""
    def identityImpersonateSuccessfulJsonTemplate = """
{"access":{
    "token":{
        "id":"\${token}"
        "expires":"\${expires}",
        "tenant":{
            "id": "\${tenant}",
            "name": "\${tenant}"
        }
    },

    "user":{
        "id":"\${userid}",
        "name":"\${tenant}",
        "roles":[
        {
            "id":"123",
            "name":"compute:admin"
        },
        {
            "id":"234",
            "name":"object-store:admin",
        }
        ]
    },

    "RAX-AUTH:impersonator":{
        "id":"\${impersonateid}",
        "name":"\${impersonatename}",
        "roles":[
            {"id":"123",
            "name":"Racker"},
            {"id":"234",
            "name":"object-store:admin"}
        ]
    }
 }
}
    """
}
