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
package features.filters.uristripper

import org.junit.experimental.categories.Category
import org.openrepose.framework.test.ReposeValveTest
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.Handling
import org.rackspace.deproxy.MessageChain
import org.rackspace.deproxy.Response
import scaffold.category.Filters
import spock.lang.Unroll

@Category(Filters)
class UriStripperLocationRewriteTest extends ReposeValveTest {

    def static String tenantId = "105620"
    def static originServiceEndpoint

    def setupSpec() {
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort)

        def params = properties.getDefaultTemplateParams()
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/filters/uristripper/common", params)
        repose.configurationProvider.applyConfigs("features/filters/uristripper/locationrewrite", params)
        repose.start()
        waitUntilReadyToServiceRequests()
        originServiceEndpoint = "${properties.targetHostname}:${properties.targetPort}"

    }

    def "when removing tenant id from request"() {

        when: "Request is sent through repose"
        def messageChain = deproxy.makeRequest(url: reposeEndpoint + "/v1/${tenantId}/path/to/resource")
        def sentRequest = ((MessageChain) messageChain).getHandlings()[0]

        then: "Repose will send uri without tenant id"
        !((Handling) sentRequest).request.path.contains(tenantId)

    }

    @Unroll("Location header is modified when path: #requestPath location: #locationheader contains tenant: #containsTenant")
    def "when putting back tenant id to Location Header within the Response"() {

        given:
        def resp = { request -> return new Response(301, "Moved Permanently", ["Location": locationheader]) }

        when: "Request is sent through repose"
        def response = deproxy.makeRequest(url: reposeEndpoint + "/v1/${tenantId}/path/to/resource", defaultHandler: resp)
        def sentRequest = ((MessageChain) response).getHandlings()[0]

        then: "Repose will put back the tenant id in the location header"
        response.receivedResponse.headers.getFirstValue("Location").contains(tenantId) == containsTenant

        and: "Repose will send uri without tenant id"
        !((Handling) sentRequest).request.path.contains(tenantId)

        where:
        requestPath                               | locationheader                                                              | containsTenant
        "/v1/${tenantId}/path/to/resource"        | "http://${originServiceEndpoint}/v1/path/to/resource"                       | true
        "/v1/${tenantId}/path/to/resource"        | "http://${originServiceEndpoint}/v2/path/to/resource"                       | true
        "/v1/${tenantId}/path/to/resource"        | "http://${originServiceEndpoint}/v1/path/resource"                          | true
        "/v1/${tenantId}/path/to/resource"        | "http://${originServiceEndpoint}/v1/path/to/resource?a=b"                   | true
        "/v1/${tenantId}/path/to/resource"        | "http://${originServiceEndpoint}/v1/path/to/resource?a=b,c,d,e"             | true
        "/v1/${tenantId}/path/to/resource"        | "http://${originServiceEndpoint}/v2/path/to/resource?a=b"                   | true
        "/v1/${tenantId}/path/to/resource"        | "http://${originServiceEndpoint}/v1/path/resource?a=b"                      | true
        "/v1/${tenantId}/path/to/resource"        | "http://${originServiceEndpoint}/v1/////path////to////resource"             | true
        "/v1////${tenantId}/path/to///resource"   | "http://${originServiceEndpoint}/v1/path/to/resource"                       | true

        "/v1/${tenantId}/path/////to////resource" | "/v1/path/to/resource"                                                      | true
        "/v1/${tenantId}/path/to/resource"        | "/v1/path/to/resource"                                                      | true
        "/v1/${tenantId}/path/to/resource"        | "/v2/path/to/resource"                                                      | true
        "/v1/${tenantId}/path/to/resource"        | "/v1/path/resource"                                                         | true
        "/v1/${tenantId}/path/to/resource"        | "/v1/path/to/resource?a=b"                                                  | true
        "/v1/${tenantId}/path/to/resource"        | "/v1/path/to/resource?a=b,c,d,e,f"                                          | true
        "/v1/${tenantId}/path/to/resource"        | "/v2/path/to/resource?a=b"                                                  | true
        "/v1/${tenantId}/path/to/resource"        | "/v1/path/resource?a=b"                                                     | true

        "/v1/${tenantId}/path/to/resource"        | "http://${originServiceEndpoint}/no/relation/resource"                      | false

        "/v1/${tenantId}/path/to/resource"        | "httdfjklsajfkdsfp://${originServiceEndpoint}/v1/path/to/resource/#\$%^&*(" | false


    }
}
