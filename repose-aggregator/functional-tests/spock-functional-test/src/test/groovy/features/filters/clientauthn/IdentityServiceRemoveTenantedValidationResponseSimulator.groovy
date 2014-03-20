package features.filters.clientauthn

import framework.mocks.MockIdentityService
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
class IdentityServiceRemoveTenantedValidationResponseSimulator {

    final String DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss'Z'";
    boolean ok = true;
    boolean adminOk = true;
    int validateTokenCount = 0;
    int groupsCount = 0;
    int adminTokenCount = 0;
    int endpointsCount = 0;



    /*
     * The tokenExpiresAt field determines when the token expires. Consumers of
     * this class should set to a particular DateTime (for example, to test
     * some aspect of expiration dates), or leave it null to default to now
     * plus one day.
     *
     */
    def tokenExpiresAt = null;

    int errorCode;
    boolean isGetAdminTokenBroken = false;
    boolean isGetGroupsBroken = false;
    boolean isValidateClientTokenBroken = false;
    boolean isGetEndpointsBroken = false;
    boolean isTenantMatch = false;
    boolean doesTenantHaveAdminRoles = false;


    def port = 12200
    def originServicePort = 10001

    def client_token = 'this-is-the-token';
    def client_tenant = 'this-is-the-tenant';
    def client_username = 'username';
    def client_userid = 12345;
    def admin_token = 'this-is-the-admin-token';
    def admin_tenant = 'this-is-the-admin-tenant';
    def admin_username = 'admin_username';
    def admin_userid = 67890;

    def templateEngine = new SimpleTemplateEngine();


    def handler = { Request request ->
        def xml = false

        request.headers.findAll('Accept').each { values ->
            if (values.contains('application/xml')) {
                xml = true
            }
        }

        def params = [:]

        // default response code and message
        def template
        def headers = ['Connection': 'close']
        def code = 200
        def message = 'OK'

        if (request.method == "POST") {
            return handleGetAdminTokenCall(request);
        } else if (request.method == "GET" && request.path.endsWith("endpoints")) {
            return handleEndpointsCall(request);
        } else if (request.method == "GET" && request.path.contains("tokens")) {
            return handleValidateTokenCall(request);
        } else if (request.method == "GET") {
            return handleGroupsCall(request);
        } else {
            throw new UnsupportedOperationException('Unknown request: %r' % request)
        }
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

    Response handleValidateTokenCall(Request request) {
        validateTokenCount += 1

        if (this.isValidateClientTokenBroken) {
            return new Response(this.errorCode);
        }

        def path = request.getPath()
        def request_token = path.substring(path.lastIndexOf("/")+1)

        def params = [
                expires: getExpires(),
                userid: client_userid,
                username: client_username,
                tenant: client_tenant,
                token: request_token
        ];

        return handleTokenCallBase(request, params, ok);
    }

    Response handleTokenCallBase(Request request, params, isAuthed) {

        def xml = false

        request.headers.findAll('Accept').each { values ->
            if (values.contains('application/xml')) {
                xml = true
            }
        }

        def code;
        def template;
        def headers = ['Connection': 'close'];

        if (xml) {
            headers.put('Content-type', 'application/xml')
        } else {
            headers.put('Content-type', 'application/json')
        }

        if (isAuthed) {
            code = 200;
            if (xml) {
                if(doesTenantHaveAdminRoles && isTenantMatch)
                    template = identitySuccessXmlWithServiceAdminTemplate
                else if (!doesTenantHaveAdminRoles && isTenantMatch)
                    template = identitySuccessXmlWithoutServiceAdminTemplate
                else if (doesTenantHaveAdminRoles && !isTenantMatch)
                    template = identitySuccessXmlWithServiceAdminDifferentTenantTemplate
                else
                    template = identitySuccessXmlWithoutServiceAdminDifferentTenantTemplate
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

    Response handleGroupsCall(Request request) {
        groupsCount += 1

        if (this.isGetGroupsBroken) {
            return new Response(this.errorCode);
        }

        def xml = false

        request.headers.findAll('Accept').each { values ->
            if (values.contains('application/xml')) {
                xml = true
            }
        }

        def params = [
                expires: getExpires(),
                userid: client_userid,
                username: client_username,
                tenant: client_tenant,
                token: request.getHeaders().getFirstValue("X-Auth-Token")

        ]

        def template;
        def headers = ['Connection': 'close'];

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

    Response handleGetAdminTokenCall(Request request) {
        adminTokenCount += 1

        SchemaFactory factory = SchemaFactory.newInstance("http://www.w3.org/XML/XMLSchema/v1.1");

        factory.setFeature("http://apache.org/xml/features/validation/cta-full-xpath-checking", true);
        Schema schema = factory.newSchema(
                new StreamSource(MockIdentityService.class.getResourceAsStream("/schema/openstack/credentials.xsd")));


        Validator validator= schema.newValidator();

        try{
            final StreamSource sampleSource = new StreamSource(new ByteArrayInputStream(request.body.getBytes()));

                validator.validate(sampleSource);

        }catch(Exception e){
            println("Admin token XSD validation error: " +e);
            return new Response(this.errorCode);
        }

        if (this.isGetAdminTokenBroken) {
            return new Response(this.errorCode);
        }

        def params = [
                expires: getExpires(),
                userid: admin_userid,
                username: admin_username,
                tenant: admin_tenant,
                token: admin_token
        ];

        return handleTokenCallBase(request, params, adminOk);
    }

    Response handleEndpointsCall(Request request) {
        endpointsCount += 1;

        if (this.isGetEndpointsBroken) {
            return new Response(this.errorCode);
        }

        def xml = false

        request.headers.findAll('Accept').each { values ->
            if (values.contains('application/xml')) {
                xml = true
            }
        }

        def code;
        def template;
        def headers = ['Connection': 'close'];

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

    def identitySuccessXmlWithoutServiceAdminTemplate =
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
                  tenantId="\${tenant}"/>
            <role id="6"
                  name="service:admin-role1"
                  description="A Role that allows a user to auth without belongsto"
                  serviceId="0000000000000000000000000000000000000003"
                  tenantId="\${tenant}"/>
            <role id="67"
                  name="service:admin-role2"
                  description="A Role that allows a user to auth without belongsto"
                  serviceId="0000000000000000000000000000000000000003"
                  tenantId="\${tenant}"/>
            <role id="5"
                  name="object-store:default"
                  description="A Role that allows a user access to keystone Service methods"
                  serviceId="0000000000000000000000000000000000000002"
                  tenantId="\${tenant}"/>
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

    def identitySuccessXmlWithServiceAdminDifferentTenantTemplate =
        """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<access xmlns="http://docs.openstack.org/identity/api/v2.0"
        xmlns:os-ksadm="http://docs.openstack.org/identity/api/ext/OS-KSADM/v1.0"
        xmlns:os-ksec2="http://docs.openstack.org/identity/api/ext/OS-KSEC2/v1.0"
        xmlns:rax-ksqa="http://docs.rackspace.com/identity/api/ext/RAX-KSQA/v1.0"
        xmlns:rax-kskey="http://docs.rackspace.com/identity/api/ext/RAX-KSKEY/v1.0">
    <token id="9999999"
           expires="\${expires}">
        <tenant id="9999999"
                name="9999999"/>
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
                  tenantId="999999"/>
            <role id="6"
                  name="service:admin-role1"
                  description="A Role that allows a user to auth without belongsto"
                  serviceId="0000000000000000000000000000000000000003"
                  tenantId="999999"/>
            <role id="67"
                  name="service:admin-role2"
                  description="A Role that allows a user to auth without belongsto"
                  serviceId="0000000000000000000000000000000000000003"
                  tenantId="999999"/>
            <role id="5"
                  name="object-store:default"
                  description="A Role that allows a user access to keystone Service methods"
                  serviceId="0000000000000000000000000000000000000002"
                  tenantId="999999"/>
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

    def identitySuccessXmlWithoutServiceAdminDifferentTenantTemplate =
        """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<access xmlns="http://docs.openstack.org/identity/api/v2.0"
        xmlns:os-ksadm="http://docs.openstack.org/identity/api/ext/OS-KSADM/v1.0"
        xmlns:os-ksec2="http://docs.openstack.org/identity/api/ext/OS-KSEC2/v1.0"
        xmlns:rax-ksqa="http://docs.rackspace.com/identity/api/ext/RAX-KSQA/v1.0"
        xmlns:rax-kskey="http://docs.rackspace.com/identity/api/ext/RAX-KSKEY/v1.0">
    <token id="9999999"
           expires="\${expires}">
        <tenant id="9999999"
                name="9999999"/>
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
                  tenantId="999999"/>
            <role id="5"
                  name="object-store:default"
                  description="A Role that allows a user access to keystone Service methods"
                  serviceId="0000000000000000000000000000000000000002"
                  tenantId="999999"/>
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

}
