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
import scaffold.category.Bug
import scaffold.category.Filters

@Category(Filters)
class CaptureGroupsTest extends ReposeValveTest {

    def setupSpec() {

        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort)

        def params = properties.getDefaultTemplateParams()
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/filters/ratelimiting/capturegroups", params)
        repose.start(killOthersBeforeStarting: false,
                waitOnJmxAfterStarting: false)
        repose.waitForNon500FromUrl(reposeEndpoint)
    }

    def "Requests to urls with different captured values should go into separate buckets"() {

        given:

        def mc
        String url1 = "$reposeEndpoint/servers/abc/instances/123"
        String url2 = "$reposeEndpoint/servers/abc/instances/456"
        String url3 = "$reposeEndpoint/servers/def/instances/123"
        String url4 = "$reposeEndpoint/servers/def/instances/456"
        def headers = ['X-PP-User': 'user1', 'X-PP-Groups': 'group']


        when: "we make one request to the first url"
        mc = deproxy.makeRequest(url: url1, headers: headers)
        then: "it should make it to the origin service"
        mc.receivedResponse.code == "200"
        mc.handlings.size() == 1

        when: "we make a second request to the first url"
        mc = deproxy.makeRequest(url: url1, headers: headers)
        then: "it should make it to the origin service"
        mc.receivedResponse.code == "200"
        mc.handlings.size() == 1

        when: "we make a third request to the first url"
        mc = deproxy.makeRequest(url: url1, headers: headers)
        then: "it should be blocked"
        mc.receivedResponse.code == "413"
        mc.handlings.size() == 0



        when: "we make one request to the second url"
        mc = deproxy.makeRequest(url: url2, headers: headers)
        then: "it should make it to the origin service"
        mc.receivedResponse.code == "200"
        mc.handlings.size() == 1

        when: "we make a second request to the second url"
        mc = deproxy.makeRequest(url: url2, headers: headers)
        then: "it should make it to the origin service"
        mc.receivedResponse.code == "200"
        mc.handlings.size() == 1

        when: "we make a third request to the second url"
        mc = deproxy.makeRequest(url: url2, headers: headers)
        then: "it should be blocked"
        mc.receivedResponse.code == "413"
        mc.handlings.size() == 0



        when: "we make one request to the third url"
        mc = deproxy.makeRequest(url: url3, headers: headers)
        then: "it should make it to the origin service"
        mc.receivedResponse.code == "200"
        mc.handlings.size() == 1

        when: "we make a second request to the third url"
        mc = deproxy.makeRequest(url: url3, headers: headers)
        then: "it should make it to the origin service"
        mc.receivedResponse.code == "200"
        mc.handlings.size() == 1

        when: "we make a third request to the third url"
        mc = deproxy.makeRequest(url: url3, headers: headers)
        then: "it should be blocked"
        mc.receivedResponse.code == "413"
        mc.handlings.size() == 0



        when: "we make one request to the fourth url"
        mc = deproxy.makeRequest(url: url4, headers: headers)
        then: "it should make it to the origin service"
        mc.receivedResponse.code == "200"
        mc.handlings.size() == 1

        when: "we make a second request to the fourth url"
        mc = deproxy.makeRequest(url: url4, headers: headers)
        then: "it should make it to the origin service"
        mc.receivedResponse.code == "200"
        mc.handlings.size() == 1

        when: "we make a third request to the fourth url"
        mc = deproxy.makeRequest(url: url4, headers: headers)
        then: "it should be blocked"
        mc.receivedResponse.code == "413"
        mc.handlings.size() == 0
    }

    def "Captured values should make no difference when concatenated"() {

        given:

        // the uri-regex is "/servers/(.+)/instances/(.+)"
        // if requests have different values for the captured part of the path,
        // they should be considered separately, and not affect each other,
        // even if they have the same combined string value when appended

        def mc
        String url1 = "$reposeEndpoint/servers/abc/instances/def"    // abc + def = abcdef
        String url2 = "$reposeEndpoint/servers/abcde/instances/f"    // abcde + f = abcdef
        def headers = ['X-PP-User': 'user2', 'X-PP-Groups': 'group']


        when: "we make one request to the first url"
        mc = deproxy.makeRequest(url: url1, headers: headers)

        then: "it should make it to the origin service"
        mc.receivedResponse.code == "200"
        mc.handlings.size() == 1


        when: "we make a second request to the first url"
        mc = deproxy.makeRequest(url: url1, headers: headers)

        then: "it should make it to the origin service"
        mc.receivedResponse.code == "200"
        mc.handlings.size() == 1


        when: "we make one request to the second url"
        mc = deproxy.makeRequest(url: url2, headers: headers)

        then: "it should make it to the origin service"
        mc.receivedResponse.code == "200"
        mc.handlings.size() == 1


        when: "we make a second request to the second url"
        mc = deproxy.makeRequest(url: url2, headers: headers)

        then:
        mc.receivedResponse.code == "200"
        mc.handlings.size() == 1


        when: "we make a third request to the first url"
        mc = deproxy.makeRequest(url: url1, headers: headers)

        then: "it should be blocked"
        mc.receivedResponse.code == "413"
        mc.handlings.size() == 0


        when: "we make a third request to the second url"
        mc = deproxy.makeRequest(url: url2, headers: headers)

        then: "it should be blocked"
        mc.receivedResponse.code == "413"
        mc.handlings.size() == 0
    }

    def "Order of captured values should make no difference when concatenated"() {

        given:

        def mc
        String url1 = "$reposeEndpoint/servers/abc/instances/def"
        String url2 = "$reposeEndpoint/servers/def/instances/abc"
        def headers = ['X-PP-User': 'user8', 'X-PP-Groups': 'group']


        when: "we make one request to the first url"
        mc = deproxy.makeRequest(url: url1, headers: headers)

        then: "it should make it to the origin service"
        mc.receivedResponse.code == "200"
        mc.handlings.size() == 1


        when: "we make a second request to the first url"
        mc = deproxy.makeRequest(url: url1, headers: headers)

        then: "it should make it to the origin service"
        mc.receivedResponse.code == "200"
        mc.handlings.size() == 1


        when: "we make one request to the second url"
        mc = deproxy.makeRequest(url: url2, headers: headers)

        then: "it should make it to the origin service"
        mc.receivedResponse.code == "200"
        mc.handlings.size() == 1


        when: "we make a second request to the second url"
        mc = deproxy.makeRequest(url: url2, headers: headers)

        then:
        mc.receivedResponse.code == "200"
        mc.handlings.size() == 1


        when: "we make a third request to the first url"
        mc = deproxy.makeRequest(url: url1, headers: headers)

        then: "it should be blocked"
        mc.receivedResponse.code == "413"
        mc.handlings.size() == 0


        when: "we make a third request to the second url"
        mc = deproxy.makeRequest(url: url2, headers: headers)

        then: "it should be blocked"
        mc.receivedResponse.code == "413"
        mc.handlings.size() == 0
    }

    def "Separate users should have separate buckets"() {

        given:

        def mc
        String url = "$reposeEndpoint/servers/abc/instances/123"
        def headers1 = ['X-PP-User': 'user3', 'X-PP-Groups': 'group']
        def headers2 = ['X-PP-User': 'user4', 'X-PP-Groups': 'group']


        when: "we make one request as the first user"
        mc = deproxy.makeRequest(url: url, headers: headers1)
        then: "it should make it to the origin service"
        mc.receivedResponse.code == "200"
        mc.handlings.size() == 1

        when: "we make a second request as the first user"
        mc = deproxy.makeRequest(url: url, headers: headers1)
        then: "it should make it to the origin service"
        mc.receivedResponse.code == "200"
        mc.handlings.size() == 1

        when: "we make a third request as the first user"
        mc = deproxy.makeRequest(url: url, headers: headers1)
        then: "it should be blocked"
        mc.receivedResponse.code == "413"
        mc.handlings.size() == 0



        when: "we make one request as the second user"
        mc = deproxy.makeRequest(url: url, headers: headers2)
        then: "it should make it to the origin service"
        mc.receivedResponse.code == "200"
        mc.handlings.size() == 1

        when: "we make a second request as the second user"
        mc = deproxy.makeRequest(url: url, headers: headers2)
        then: "it should make it to the origin service"
        mc.receivedResponse.code == "200"
        mc.handlings.size() == 1

        when: "we make a third request as the second user"
        mc = deproxy.makeRequest(url: url, headers: headers2)
        then: "it should be blocked"
        mc.receivedResponse.code == "413"
        mc.handlings.size() == 0
    }

    def "A pattern with no capture groups () should use the pattern as the key"() {

        given:

        def mc
        String url1 = "$reposeEndpoint/objects/abc/things/123"
        String url2 = "$reposeEndpoint/objects/def/things/456"
        def headers = ['X-PP-User': 'user5', 'X-PP-Groups': 'no-captures']


        when: "we make one request to the first url"
        mc = deproxy.makeRequest(url: url1, headers: headers)
        then: "it should make it to the origin service"
        mc.receivedResponse.code == "200"
        mc.handlings.size() == 1

        when: "we make a second request to the first url"
        mc = deproxy.makeRequest(url: url1, headers: headers)
        then: "it should make it to the origin service"
        mc.receivedResponse.code == "200"
        mc.handlings.size() == 1

        when: "we make a third request to the first url"
        mc = deproxy.makeRequest(url: url1, headers: headers)
        then: "it should be blocked"
        mc.receivedResponse.code == "413"
        mc.handlings.size() == 0



        when: "we make one request to the second url"
        mc = deproxy.makeRequest(url: url2, headers: headers)
        then: "it should be block as well"
        mc.receivedResponse.code == "413"
        mc.handlings.size() == 0
    }

    def "Limits in separate <limit>s should get separate buckets"() {

        given:

        def mc
        String url1 = "$reposeEndpoint/v1/abc/servers"
        String url2 = "$reposeEndpoint/v2/abc/images"
        String url3 = "$reposeEndpoint/v1/def/servers"
        String url4 = "$reposeEndpoint/v2/def/images"
        def headers = ['X-PP-User': 'user7', 'X-PP-Groups': 'separate-limits']


        when: "we make one request to the first url"
        mc = deproxy.makeRequest(url: url1, headers: headers)
        then: "it should make it to the origin service"
        mc.receivedResponse.code == "200"
        mc.handlings.size() == 1

        when: "we make a second request to the first url"
        mc = deproxy.makeRequest(url: url1, headers: headers)
        then: "it should make it to the origin service"
        mc.receivedResponse.code == "200"
        mc.handlings.size() == 1

        when: "we make a third request to the first url"
        mc = deproxy.makeRequest(url: url1, headers: headers)
        then: "it should be blocked"
        mc.receivedResponse.code == "413"
        mc.handlings.size() == 0



        when: "we make one request to the second url"
        mc = deproxy.makeRequest(url: url2, headers: headers)
        then: "it should make it to the origin service"
        mc.receivedResponse.code == "200"
        mc.handlings.size() == 1

        when: "we make a second request to the second url"
        mc = deproxy.makeRequest(url: url2, headers: headers)
        then: "it should make it to the origin service"
        mc.receivedResponse.code == "200"
        mc.handlings.size() == 1

        when: "we make a third request to the second url"
        mc = deproxy.makeRequest(url: url2, headers: headers)
        then: "it should be blocked"
        mc.receivedResponse.code == "413"
        mc.handlings.size() == 0



        when: "we make one request to the third url"
        mc = deproxy.makeRequest(url: url3, headers: headers)
        then: "it should make it to the origin service"
        mc.receivedResponse.code == "200"
        mc.handlings.size() == 1

        when: "we make a second request to the third url"
        mc = deproxy.makeRequest(url: url3, headers: headers)
        then: "it should make it to the origin service"
        mc.receivedResponse.code == "200"
        mc.handlings.size() == 1

        when: "we make a third request to the third url"
        mc = deproxy.makeRequest(url: url3, headers: headers)
        then: "it should be blocked"
        mc.receivedResponse.code == "413"
        mc.handlings.size() == 0



        when: "we make one request to the fourth url"
        mc = deproxy.makeRequest(url: url4, headers: headers)
        then: "it should make it to the origin service"
        mc.receivedResponse.code == "200"
        mc.handlings.size() == 1

        when: "we make a second request to the fourth url"
        mc = deproxy.makeRequest(url: url4, headers: headers)
        then: "it should make it to the origin service"
        mc.receivedResponse.code == "200"
        mc.handlings.size() == 1

        when: "we make a third request to the fourth url"
        mc = deproxy.makeRequest(url: url4, headers: headers)
        then: "it should be blocked"
        mc.receivedResponse.code == "413"
        mc.handlings.size() == 0
    }

    @Category(Bug)
    def "Requests to an url and a percent-encoded equivalent form of that url should go into the same bucket"() {

        // DEFECT: D-16373
        // rfc 3986 § 2.3: "URIs that differ in the replacement of an
        // unreserved character with its corresponding percent-encoded
        // US-ASCII octet are equivalent: they identify the same resource."

        given:

        def mc
        String url1 = "$reposeEndpoint/servers/abc/instances/123"
        String url2 = "$reposeEndpoint/servers/abc/instances/%31%32%33"
        def headers = ['X-PP-User': 'user9', 'X-PP-Groups': 'group']


        when: "we make one request to the first url"
        mc = deproxy.makeRequest(url: url1, headers: headers)
        then: "it should make it to the origin service"
        mc.receivedResponse.code == "200"
        mc.handlings.size() == 1

        when: "we make a second request to the first url"
        mc = deproxy.makeRequest(url: url1, headers: headers)
        then: "it should make it to the origin service"
        mc.receivedResponse.code == "200"
        mc.handlings.size() == 1

        when: "we make a third request to the first url"
        mc = deproxy.makeRequest(url: url1, headers: headers)
        then: "it should be blocked"
        mc.receivedResponse.code == "413"
        mc.handlings.size() == 0



        when: "we make one request to the second url"
        mc = deproxy.makeRequest(url: url2, headers: headers)
        then: "it should be blocked"
        mc.receivedResponse.code == "413"
        mc.handlings.size() == 0


    }

    @Category(Bug)
    def "Capitalization of hex digits in percent-encoded octets should be treated as equivalent"() {

        // DEFECT: D-16373
        // rfc 3986 § 2.1: "The uppercase hexadecimal digits 'A' through 'F'
        // are equivalent to the lowercase digits 'a' through 'f',
        // respectively.  If two URIs differ only in the case of hexadecimal
        // digits used in percent-encoded octets, they are equivalent."

        given:

        def mc
        String url1 = "$reposeEndpoint/servers/%6a%6b%6c/instances/123"  //  /servers/jkl/instances/123
        String url2 = "$reposeEndpoint/servers/%6A%6B%6C/instances/123"
        def headers = ['X-PP-User': 'user10', 'X-PP-Groups': 'group']


        when: "we make one request to the first url"
        mc = deproxy.makeRequest(url: url1, headers: headers)
        then: "it should make it to the origin service"
        mc.receivedResponse.code == "200"
        mc.handlings.size() == 1

        when: "we make a second request to the first url"
        mc = deproxy.makeRequest(url: url1, headers: headers)
        then: "it should make it to the origin service"
        mc.receivedResponse.code == "200"
        mc.handlings.size() == 1

        when: "we make a third request to the first url"
        mc = deproxy.makeRequest(url: url1, headers: headers)
        then: "it should be blocked"
        mc.receivedResponse.code == "413"
        mc.handlings.size() == 0



        when: "we make one request to the second url"
        mc = deproxy.makeRequest(url: url2, headers: headers)
        then: "it should be blocked"
        mc.receivedResponse.code == "413"
        mc.handlings.size() == 0

    }
}
