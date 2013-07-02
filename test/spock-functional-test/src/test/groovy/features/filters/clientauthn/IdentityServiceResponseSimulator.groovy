package features.filters.clientauthn

import groovy.text.SimpleTemplateEngine
import org.joda.time.DateTime
import org.rackspace.gdeproxy.Request
import org.rackspace.gdeproxy.Response

/**
 * Simulates responses from an Identity Service
 */
class IdentityServiceResponseSimulator {

    final String DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss'Z'"
    boolean ok = true
    int validateTokenCount = 0
    int ttlDurationInDays = 1

    def client_token = 'this-is-the-token'
    def client_tenant = 'this-is-the-tenant'
    def client_username = 'username'
    def client_userid = 12345
    def admin_token = 'this-is-the-admin-token'
    def admin_tenant = 'this-is-the-admin-tenant'
    def admin_username = 'admin_username'
    def admin_userid = 67890

    def templateEngine = new SimpleTemplateEngine()


    def handler = { Request request ->
        def xml = false

        request.headers.findAll('Accept').each { values ->
            if (values.contains('application/xml')) {
                xml = true
            }
        }

        def now = new DateTime()
        def nowPlusTTL = now.plusDays(ttlDurationInDays)

        def params = [:]

        // default response code and message
        def template
        def headers = ['Connection': 'close']
        def code = 200
        def message = 'OK'
        if (xml) {
            template = identitySuccessXmlTemplate
            headers.put('Content-type', 'application/xml')
        } else {
            template = identitySuccessJsonTemplate
            headers.put('Content-type', 'application/json')
        }

        switch (request.method) {

            case "GET":
                if (request.path.contains("tokens")) {
                    validateTokenCount += 1

                    params = [
                            expires: nowPlusTTL.toString(DATE_FORMAT),
                            userid: client_userid,
                            username: client_username,
                            tenant: client_tenant,
                            token: client_token
                    ]

                    if (!ok) {
                        code = 404
                        message = 'Not Found'
                        if (xml)
                            template = identityFailureXmlTemplate
                        else
                            template = identityFailureJsonTemplate
                    }
                } else {
                    if (xml)
                        template = groupsXmlTemplate
                    else
                        template = groupsJsonTemplate
                }
                break
            case "POST":
                params = [
                        expires: nowPlusTTL.toString(DATE_FORMAT),
                        userid: admin_userid,
                        username: admin_username,
                        tenant: admin_tenant,
                        token: admin_token
                ]
                break
            default:
                throw new UnsupportedOperationException('Unknown request: %r' % request)
        }

        def body = templateEngine.createTemplate(template).make(params)

        println body
        return new Response(code, message, headers, body)

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
         "RAX-AUTH:defaultRegion" : "",
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
          rax-auth:defaultRegion="">
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
}
