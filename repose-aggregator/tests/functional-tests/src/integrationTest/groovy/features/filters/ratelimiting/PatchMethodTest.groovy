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
package features.filters.ratelimiting

import org.junit.experimental.categories.Category
import org.openrepose.framework.test.ReposeValveTest
import org.rackspace.deproxy.Deproxy
import scaffold.category.Filters

@Category(Filters)
class PatchMethodTest extends ReposeValveTest {

    def setupSpec() {
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort)
    }

    def setup() {
        def params = properties.getDefaultTemplateParams()
        repose.configurationProvider.cleanConfigDirectory()
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/filters/ratelimiting/oneNode", params)
        repose.start(killOthersBeforeStarting: false,
                waitOnJmxAfterStarting: false)
        repose.waitForNon500FromUrl(properties.reposeEndpoint)
    }

    def "PATCH requests should be limited by limit-groups marked as 'PATCH'"() {

        given:
        def mc
        String url = "$reposeEndpoint/patchmethod/resource"
        def headers = ['X-PP-User': 'user', 'X-PP-Groups': 'patchmethod']


        when: "we make some PATCH requests"
        mc = deproxy.makeRequest(method: "PATCH", url: url, headers: headers)

        then: "they should all come out ok"
        mc.receivedResponse.code == "200"
        mc.handlings.size() == 1

        when:
        mc = deproxy.makeRequest(method: "PATCH", url: url, headers: headers)
        then:
        mc.receivedResponse.code == "200"
        mc.handlings.size() == 1

        when:
        mc = deproxy.makeRequest(method: "PATCH", url: url, headers: headers)
        then:
        mc.receivedResponse.code == "200"
        mc.handlings.size() == 1

        when:
        mc = deproxy.makeRequest(method: "PATCH", url: url, headers: headers)
        then:
        mc.receivedResponse.code == "200"
        mc.handlings.size() == 1




        when: "we make the final request that goes over the limit"
        mc = deproxy.makeRequest(method: "PATCH", url: url, headers: headers)

        then: "Repose should return an error and not forward the request to the origin service"
        mc.receivedResponse.code == "413"
        mc.handlings.size() == 0
    }

    def "PATCH requests should apply to limit-groups marked as 'ALL'"() {

        given:
        def mc
        String url = "$reposeEndpoint/allmethods/resource"
        def headers = ['X-PP-User': 'user', 'X-PP-Groups': 'allmethods']

        when: "we make some requests with mixed methods"
        mc = deproxy.makeRequest(method: "GET", url: url, headers: headers)

        then: "they should all come out ok"
        mc.receivedResponse.code == "200"
        mc.handlings.size() == 1

        when:
        mc = deproxy.makeRequest(method: "PATCH", url: url, headers: headers)
        then:
        mc.receivedResponse.code == "200"
        mc.handlings.size() == 1

        when:
        mc = deproxy.makeRequest(method: "GET", url: url, headers: headers)
        then:
        mc.receivedResponse.code == "200"
        mc.handlings.size() == 1

        when:
        mc = deproxy.makeRequest(method: "PATCH", url: url, headers: headers)
        then:
        mc.receivedResponse.code == "200"
        mc.handlings.size() == 1




        when: "we make the final request that goes over the limit"
        mc = deproxy.makeRequest(method: "GET", url: url, headers: headers)

        then: "Repose should return an error and not forward the request to the origin service"
        mc.receivedResponse.code == "413"
        mc.handlings.size() == 0
    }


    def "PATCH requests from one user shouldn't affect another user"() {

        given:
        def mc
        String url = "$reposeEndpoint/patchmethod/resource"
        def headers1 = ['X-PP-User': 'user1', 'X-PP-Groups': 'patchmethod']
        def headers2 = ['X-PP-User': 'user2', 'X-PP-Groups': 'patchmethod']


        when: "we make some PATCH requests"
        mc = deproxy.makeRequest(method: "PATCH", url: url, headers: headers1)

        then: "they should all come out ok"
        mc.receivedResponse.code == "200"
        mc.handlings.size() == 1

        when:
        mc = deproxy.makeRequest(method: "PATCH", url: url, headers: headers1)
        then:
        mc.receivedResponse.code == "200"
        mc.handlings.size() == 1

        when:
        mc = deproxy.makeRequest(method: "PATCH", url: url, headers: headers1)
        then:
        mc.receivedResponse.code == "200"
        mc.handlings.size() == 1

        when:
        mc = deproxy.makeRequest(method: "PATCH", url: url, headers: headers1)
        then:
        mc.receivedResponse.code == "200"
        mc.handlings.size() == 1




        when: "we make a separate request as another user"
        mc = deproxy.makeRequest(method: "PATCH", url: url, headers: headers2)

        then: "Repose should let the request through"
        mc.receivedResponse.code == "200"
        mc.handlings.size() == 1
    }

    def "PATCH requests should allow all headers and body to pass through"() {

        given:
        def mc
        String url = "$reposeEndpoint/patchmethod/resource"
        def headers1 = ['X-PP-User': 'user1', 'X-PP-Groups': 'patchmethod', 'random-header': 'testtest']

        when: "we make a PATCH request"
        mc = deproxy.makeRequest(method: "PATCH", url: url, headers: headers1, requestBody: "My Content Body")

        then: "the request headers and body should be passed to the origin service"
        mc.handlings.size() == 1
        mc.handlings[0].request.body == "My Content Body"
        mc.handlings[0].request.method == "PATCH"
        mc.handlings[0].request.headers.contains("X-PP-User")
        mc.handlings[0].request.headers.contains("random-header")
    }

    def cleanup() {
        repose?.stop()
    }
}
