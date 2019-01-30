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
package features.filters.ipuser

import org.junit.experimental.categories.Category
import org.openrepose.framework.test.ReposeValveTest
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.Handling
import org.rackspace.deproxy.MessageChain
import scaffold.category.Filters

import java.util.concurrent.TimeUnit

import static javax.servlet.http.HttpServletResponse.SC_SERVICE_UNAVAILABLE

@Category(Filters)
class IpUserTest extends ReposeValveTest {

    def setupSpec() {
        deproxy = new Deproxy()
        //Just set up a simple endpoint
        deproxy.addEndpoint(properties.targetPort, 'origin service')
    }

    def cleanup() {
        if (repose) {
            repose.stop()
        }
    }

    def "verify classifying with other config"() {
        def params = properties.defaultTemplateParams
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/filters/ipuser", params)
        repose.configurationProvider.applyConfigs("features/filters/ipuser/otherconfig", params)
        repose.start()

        when: "Request is sent through repose"
        def mc = deproxy.makeRequest(url: reposeEndpoint, method: 'get')
        def sentRequest = ((MessageChain) mc).handlings[0]

        then: "Repose will send x-pp-group with the configured value"
        mc.handlings.size() == 1

        and: "Repose will send x-pp-user based on requestor ip"
        ((Handling) sentRequest).request.getHeaders().findAll("x-pp-user").size() == 1
        def user = sentRequest.request.headers.getFirstValue("x-pp-user")
        // The possible IPv4/6 addresses.
        user == "127.0.0.1;q=0.7" || user == "0:0:0:0:0:0:0:1;q=0.7" || user == "::1;q=0.7"

        and: "Repose will send x-pp-groups with the configured value"
        ((Handling) sentRequest).request.headers.getFirstValue("x-pp-groups").equalsIgnoreCase("local-group;q=0.5")
    }

    def "classifying with multi-match config only get the 1st match"() {
        def params = properties.defaultTemplateParams
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/filters/ipuser", params)
        repose.configurationProvider.applyConfigs("features/filters/ipuser/multigroupsmatch", params)
        repose.start()

        when: "Request is sent through repose"
        def mc = deproxy.makeRequest(url: reposeEndpoint, method: 'get')
        def sentRequest = ((MessageChain) mc).handlings[0]

        then: "Repose will send x-pp-group with the configured value"
        mc.handlings.size() == 1

        and: "Repose will send x-pp-user based on requestor ip"
        ((Handling) sentRequest).request.getHeaders().findAll("x-pp-user").size() == 1
        def user = sentRequest.request.headers.getFirstValue("x-pp-user")
        // The possible IPv4/6 addresses.
        user == "127.0.0.1;q=0.7" || user == "0:0:0:0:0:0:0:1;q=0.7" || user == "::1;q=0.7"

        and: "Repose will send x-pp-groups with the configured value"
        def group = ((Handling) sentRequest).request.headers.findAll("x-pp-groups")
        group.contains("match-all-group;q=0.6")
        !group.contains("local-group;q=0.6")
        !group.contains("local-lan-ip;q=0.6")
    }
}
