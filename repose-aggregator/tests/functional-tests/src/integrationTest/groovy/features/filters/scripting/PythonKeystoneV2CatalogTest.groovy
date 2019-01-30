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
package features.filters.scripting

import groovy.text.SimpleTemplateEngine
import org.junit.experimental.categories.Category
import org.openrepose.commons.utils.http.PowerApiHeader
import org.openrepose.framework.test.ReposeValveTest
import org.openrepose.framework.test.mocks.MockIdentityV2Service
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain
import org.rackspace.deproxy.Request
import org.rackspace.deproxy.Response
import scaffold.category.Filters

import javax.servlet.http.HttpServletResponse

@Category(Filters)
class PythonKeystoneV2CatalogTest extends ReposeValveTest {
    static tenantId = "mytenant"

    static MockIdentityV2Service fakeIdentityV2Service
    static def templateEngine = new SimpleTemplateEngine()

    def setupSpec() {
        deproxy = new Deproxy()

        def params = properties.getDefaultTemplateParams()
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/filters/scripting/python/keystonev2catalog", params)

        deproxy.addEndpoint(properties.targetPort, 'origin service')

        fakeIdentityV2Service = new MockIdentityV2Service(params.identityPort, params.targetPort)
        deproxy.addEndpoint(params.identityPort, 'identity service', null, fakeIdentityV2Service.handler)

        repose.start()
        repose.waitForNon500FromUrl(reposeEndpoint)
    }

    def setup() {
        fakeIdentityV2Service.resetDefaultParameters()
    }

    def "the scripting filter can be used to read the keystone v2 service catalog header"() {
        given: "Identity is going to return the necessary endpoints to be validated by the Scripting filter"
        fakeIdentityV2Service.with {
            client_token = UUID.randomUUID().toString()
            client_tenantid = tenantId
            client_tenantname = "mytenantname"
            client_userid = "12345"
            getEndpointsHandler = createEndpointsHandlerWith(goodEndpoints)
        }
        def headers = ['X-Auth-Token': fakeIdentityV2Service.client_token]

        when: "the request is sent"
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint + "/servers/test", method: "GET", headers: headers)

        then: "the request successfully makes it to the origin service"
        mc.receivedResponse.code as Integer == HttpServletResponse.SC_OK
        mc.handlings.size() == 1

        and: "the Keystone v2 filter adds the X-Catalog header to the request"
        mc.handlings[0].request.headers.findAll(PowerApiHeader.X_CATALOG).size() == 1

        and: "the Scripting filter adds the X-Endpoint-Name header to the request and response with the expected values"
        mc.handlings[0].request.headers.findAll("X-Endpoint-Name") == ["cloudMonitoring, cloudServersOpenStack"]
        mc.receivedResponse.headers.findAll("X-Endpoint-Name") == ["cloudMonitoring, cloudServersOpenStack"]
    }

    def "the scripting filter can be used to reject requests based on the contents of the keystone v2 service catalog header"() {
        given: "Identity is going to return inadequate endpoints which will not be validated by the Scripting filter"
        fakeIdentityV2Service.with {
            client_token = UUID.randomUUID().toString()
            client_tenantid = tenantId
            client_tenantname = "mytenantname"
            client_userid = "12345"
            getEndpointsHandler = createEndpointsHandlerWith(inadequateEndpoints)
        }
        def headers = ['X-Auth-Token': fakeIdentityV2Service.client_token]

        when: "the request is sent"
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint + "/servers/test", method: "GET", headers: headers)

        then: "the request is rejected"
        mc.receivedResponse.code as Integer == HttpServletResponse.SC_FORBIDDEN
        mc.receivedResponse.body == "User did not have the required endpoints"

        and: "does not make it to the origin service"
        mc.handlings.isEmpty()
    }

    Closure<Response> createEndpointsHandlerWith(String template) {
        { String tokenId, Request request ->
            def headers = ["Content-type": "application/json"]
            def params = [endpointUrl: reposeEndpoint, tenantId: tenantId]
            def body = templateEngine.createTemplate(template).make(params)

            new Response(200, null, headers, body)
        }
    }

    static String goodEndpoints = """\
        |{
        |    "endpoints": [
        |        {
        |            "id": 1,
        |            "name": "cloudMonitoring",
        |            "publicURL": "\${endpointUrl}/v1.0/\${tenantId}",
        |            "tenantId": "\${tenantId}",
        |            "type": "rax:monitor"
        |        },
        |        {
        |            "id": 2,
        |            "name": "cloudServersOpenStack",
        |            "publicURL": "\${endpointUrl}/v2/\${tenantId}",
        |            "region": "DFW",
        |            "tenantId": "\${tenantId}",
        |            "type": "compute",
        |            "versionId": "2",
        |            "versionInfo": "\${endpointUrl}/v2",
        |            "versionList": "\${endpointUrl}/"
        |        }
        |    ]
        |}""".stripMargin()

    static String inadequateEndpoints = """\
        |{
        |    "endpoints": [
        |        {
        |            "id": 1,
        |            "name": "cloudMonitoring",
        |            "publicURL": "\${endpointUrl}/v1.0/\${tenantId}",
        |            "tenantId": "\${tenantId}",
        |            "type": "rax:monitor"
        |        },
        |        {
        |            "id": 3,
        |            "name": "autoscale",
        |            "publicURL": "\${endpointUrl}/v1.0/\${tenantId}",
        |            "region": "SYD",
        |            "tenantId": "\${tenantId}",
        |            "type": "rax:autoscale"
        |        }
        |    ]
        |}""".stripMargin()
}
