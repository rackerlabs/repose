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

    int validateTokenCount = 0;
    int getGroupsCount = 0;
    int generateTokenCount = 0;
    int getEndpointsCount = 0;
    int getUserGlobalRolesCount = 0;

    void resetCounts() {

        validateTokenCount = 0;
        getGroupsCount = 0;
        generateTokenCount = 0;
        getEndpointsCount = 0;
        getUserGlobalRolesCount = 0;
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
    def client_username = 'username';
    def client_userid = 12345;
    def admin_token = 'this-is-the-admin-token';
    def admin_tenant = 'this-is-the-admin-tenant';
    def admin_username = 'admin_username';
    def admin_userid = 67890;
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

        def params = [:]

        // default response code and message
        def template
        def headers = [:]
        def code = 200
        def message = 'OK'
        if (xml) {
            template = identitySuccessXmlTemplate
            headers.put('Content-type', 'application/xml')
        } else {
            template = identitySuccessJsonTemplate
            headers.put('Content-type', 'application/json')
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

        def match

        if (path.startsWith("/tokens")) {

            if (path == "/tokens") {
                if (method == "POST") {

                    generateTokenCount++

                    return generateTokenHandler(request, xml);

                } else {
                    return new Response(405)
                }
            }

            match = (path =~ /\/tokens\/([^\/]+)(\?belongsTo)?/)
            if (match) {
                if (method == 'GET') {

                    validateTokenCount++

                    def tokenId = match[0][1]
                    return validateTokenHandler(tokenId, request, xml)

                } else {
                    return new Response(405)
                }
            }

            match = (path ==~ /\/tokens\/([^\/]+)\/endpoints/)
            if (match) {
                if (method == "GET") {

                    getEndpointsCount++

                    def tokenId = match[0][1]
                    return getEndpointsHandler(tokenId, request, xml)

                } else {
                    return new Response(405)
                }
            }

        } else if (path.startsWith("/users/")) {

            match = (path =~ /\/users\/([^\/]+)\/RAX-KSGRP/)
            if (match) {
                if (method =="GET") {

                    getGroupsCount++

                    def userId = match[0][1]
                    return getGroupsHandler(userId, request, xml)

                } else {
                    return new Response(405)
                }
            }

            match = (path =~ /\/users\/([^\/]+)\/roles/)
            if (match) {
                if (method =="GET") {

                    getUserGlobalRolesCount++

                    def userId = match[0][1]
                    return getUserGlobalRoles(userId, request, xml)

                } else {
                    return new Response(405)
                }
            }
        }

        return new Response(501);
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

        def params = [
                expires: getExpires(),
                userid: client_userid,
                username: client_username,
                tenant: client_tenant,
                token: request_token
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
                template = identitySuccessXmlTemplate
            } else {
                template = identitySuccessJsonTemplate
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
                expires: getExpires(),
                userid: client_userid,
                username: client_username,
                tenant: client_tenant,
                token: request.getHeaders().getFirstValue("X-Auth-Token")

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

        try {

            final StreamSource sampleSource = new StreamSource(new ByteArrayInputStream(request.body.getBytes()));
            validator.validate(sampleSource);

        } catch (Exception e) {

            println("Admin token XSD validation error: " + e);
            return new Response(400);
        }

        def params = [
                expires: getExpires(),
                userid: admin_userid,
                username: admin_username,
                tenant: admin_tenant,
                token: admin_token
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
                template = identitySuccessXmlTemplate
            } else {
                template = identitySuccessJsonTemplate
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

    Response getEndpoints(String tokenId, Request request, boolean xml) {

        def code;
        def template;
        def headers = [:];

        if (xml) {
            headers.put('Content-type', 'application/xml')
            template = this.identityEndpointXmlTemplate;
        } else {
            headers.put('Content-type', 'application/json')
            template = this.identityEndpointJsonTemplate;
        }

        def params = [
                'identityPort': this.port,
                token: request.getHeaders().getFirstValue("X-Auth-Token"),
                'expires': getExpires(),
                'userid': this.client_userid,
                'username': this.client_username,
                'tenant': this.client_tenant,
                'originServicePort': this.originServicePort,
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

    def groupsXmlTemplate =
        """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<groups xmlns="http://docs.rackspace.com/identity/api/ext/RAX-KSGRP/v1.0">
    <group id="0" name="Default">
        <description>Default Limits</description>
    </group>
</groups>
"""

    def identityFailureJsonTemplate =
        """{
   "itemNotFound" : {
      "message" : "Invalid Token, not found.",
      "code" : 404
   }
}
"""

    def identityFailureXmlTemplate =
        """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<itemNotFound xmlns="http://docs.openstack.org/identity/api/v2.0"
              xmlns:ns2="http://docs.openstack.org/identity/api/ext/OS-KSADM/v1.0"
              code="404">
  <message>Invalid Token, not found.</message>
</itemNotFound>
"""

    def identitySuccessJsonTemplate =
        """{
   "access" : {
      "serviceCatalog" : [
         {
            "name" : "cloudFilesCDN",
            "type" : "rax:object-cdn",
            "endpoints" : [
               {
                  "publicURL" : "https://cdn.stg.clouddrive.com/v1/\${tenant}",
                  "tenantId" : "\${tenant}",
                  "region" : "DFW"
               },
               {
                  "publicURL" : "https://cdn.stg.clouddrive.com/v1/\${tenant}",
                  "tenantId" : "\${tenant}",
                  "region" : "ORD"
               }
            ]
         },
         {
            "name" : "cloudFiles",
            "type" : "object-store",
            "endpoints" : [
               {
                  "internalURL" : "https://snet-storage.stg.swift.racklabs.com/v1/\${tenant}",
                  "publicURL" : "https://storage.stg.swift.racklabs.com/v1/\${tenant}",
                  "tenantId" : "\${tenant}",
                  "region" : "ORD"
               },
               {
                  "internalURL" : "https://snet-storage.stg.swift.racklabs.com/v1/\${tenant}",
                  "publicURL" : "https://storage.stg.swift.racklabs.com/v1/\${tenant}",
                  "tenantId" : "\${tenant}",
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
                  tenantId="12345"/>
            <role id="5"
                  name="object-store:default"
                  description="A Role that allows a user access to keystone Service methods"
                  serviceId="0000000000000000000000000000000000000002"
                  tenantId="12345"/>
        </roles>
    </user>
    <serviceCatalog>
        <service type="rax:object-cdn"
                 name="cloudFilesCDN">
            <endpoint region="DFW"
                      tenantId="\${tenant}"
                      publicURL="https://cdn.stg.clouddrive.com/v1/\${tenant}"/>
            <endpoint region="ORD"
                      tenantId="\${tenant}"
                      publicURL="https://cdn.stg.clouddrive.com/v1/\${tenant}"/>
        </service>
        <service type="object-store"
                 name="cloudFiles">
            <endpoint region="ORD"
                      tenantId="\${tenant}"
                      publicURL="https://storage.stg.swift.racklabs.com/v1/\${tenant}"
                      internalURL="https://snet-storage.stg.swift.racklabs.com/v1/\${tenant}"/>
            <endpoint region="DFW"
                      tenantId="\${tenant}"
                      publicURL="https://storage.stg.swift.racklabs.com/v1/\${tenant}"
                      internalURL="https://snet-storage.stg.swift.racklabs.com/v1/\${tenant}"/>
        </service>
    </serviceCatalog>
</access>
"""

    def identitySuccessXmlWithServiceAdminTemplate =
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
                  tenantId="12345"/>
            <role id="6"
                  name="service:admin"
                  description="A Role that allows a user to auth without belongsto"
                  serviceId="0000000000000000000000000000000000000003"
                  tenantId="12345"/>
            <role id="5"
                  name="object-store:default"
                  description="A Role that allows a user access to keystone Service methods"
                  serviceId="0000000000000000000000000000000000000002"
                  tenantId="12345"/>
        </roles>
    </user>
    <serviceCatalog>
        <service type="rax:object-cdn"
                 name="cloudFilesCDN">
            <endpoint region="DFW"
                      tenantId="\${tenant}"
                      publicURL="https://cdn.stg.clouddrive.com/v1/\${tenant}"/>
            <endpoint region="ORD"
                      tenantId="\${tenant}"
                      publicURL="https://cdn.stg.clouddrive.com/v1/\${tenant}"/>
        </service>
        <service type="object-store"
                 name="cloudFiles">
            <endpoint region="ORD"
                      tenantId="\${tenant}"
                      publicURL="https://storage.stg.swift.racklabs.com/v1/\${tenant}"
                      internalURL="https://snet-storage.stg.swift.racklabs.com/v1/\${tenant}"/>
            <endpoint region="DFW"
                      tenantId="\${tenant}"
                      publicURL="https://storage.stg.swift.racklabs.com/v1/\${tenant}"
                      internalURL="https://snet-storage.stg.swift.racklabs.com/v1/\${tenant}"/>
        </service>
    </serviceCatalog>
</access>
"""

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
            publicURL="http://localhost:\${originServicePort}/\${tenant}"
            internalURL="http://localhost:\${originServicePort}/\${tenant}"
            adminURL="http://localhost:\${originServicePort}/\${tenant}"
            tenantId="\${tenant}"/>
  <endpoint id="2"
            type="compute"
            name="nova_compat"
            region="RegionOne"
            publicURL="http://localhost:\${originServicePort}/\${tenant}"
            internalURL="http://localhost:\${originServicePort}/\${tenant}"
            adminURL="http://localhost:\${originServicePort}/\${tenant}"
            tenantId="\${tenant}"/>
</endpoints>"""

    def getUserGlobalRolesJsonTemplate =
        """{
    "roles":[{
            "id":"123",
            "name":"compute:admin",
            "description":"Nova Administrator"
        }
    ],
    "roles_links":[]
}"""

    def getUserGlobalRolesXmlTemplate =
        """<?xml version="1.0" encoding="UTF-8"?>
<roles xmlns="http://docs.openstack.org/identity/api/v2.0">
    <role id="123" name="Admin" description="All Access" />
    <role id="234" name="Guest" description="Guest Access" />
</roles>"""

}
