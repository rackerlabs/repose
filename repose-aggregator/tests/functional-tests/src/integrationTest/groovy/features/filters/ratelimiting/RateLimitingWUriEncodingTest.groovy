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
import spock.lang.Unroll

/**
 * Created by jennyvo on 6/25/14.
 */
@Category(Filters)
class RateLimitingWUriEncodingTest extends ReposeValveTest {
    def setupSpec() {
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort)

        def params = properties.getDefaultTemplateParams()
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/filters/ratelimiting/uriencoding", params)
        repose.enableDebug()
        repose.start(killOthersBeforeStarting: false,
                waitOnJmxAfterStarting: false)
        repose.waitForNon500FromUrl(properties.reposeEndpoint)
    }

    @Unroll("Request with uri #uri should be applied the same limit group #group")
    def "Requests with encoded url should be decode before applying RL"() {

        given:

        def mc
        def headers = ['X-PP-User': 'user1', 'X-PP-Groups': group]


        when: "we make one request to the url"
        mc = deproxy.makeRequest(url: "${properties.reposeEndpoint}" + uri, headers: headers)
        then: "it should rate limit and get correct respcode"
        mc.receivedResponse.code == respcode
        if (respcode == "200")
            mc.handlings.size() == 1

        where:
        uri                                   | respcode | group
        "/servers/abc/instances/123"          | "200"    | "test1"
        "/servers/abc/instances/%31%32%33"    | "200"    | "test1"
        "/servers/abc/instances%2F123"        | "200"    | "test1"
        "/servers/abc/instances/123"          | "413"    | "test1"
        "/objects/jkl/things/123"             | "200"    | "test2"
        "/objects/%6a%6b%6c/things/%31%32%33" | "200"    | "test2"
        "/objects/%6A%6B%6C/things/%31%32%33" | "200"    | "test2"
        "/objects/jkl/things/123"             | "413"    | "test2"
    }

    @Unroll("Request with uri #uri and method #method should be applied the same limit group #group")
    def "Requests with encoded url should be decode before applying RL uri and method"() {

        given:

        def mc
        def headers = ['X-PP-User': 'user1', 'X-PP-Groups': group]


        when: "we make one request to the url"
        mc = deproxy.makeRequest(url: "${properties.reposeEndpoint}" + uri, method: method, headers: headers)
        then: "it should rate limit and get correct respcode"
        mc.receivedResponse.code == respcode
        if (respcode == "200")
            mc.handlings.size() == 1

        where:
        uri                                | respcode | method  | group
        "/method/jkl/test/123"             | "200"    | "GET"   | "test3"
        "/method/%6a%6b%6c/test/%31%32%33" | "200"    | "GET"   | "test3"
        "/method/%6A%6B%6C/test/%31%32%33" | "200"    | "GET"   | "test3"
        "/method/jkl/test/123"             | "200"    | "PATCH" | "test3"
        "/method/%6a%6b%6c/test/%31%32%33" | "413"    | "GET"   | "test3"
        "/method/%6A%6B%6C/test/123"       | "200"    | "PATCH" | "test3"
    }
}

