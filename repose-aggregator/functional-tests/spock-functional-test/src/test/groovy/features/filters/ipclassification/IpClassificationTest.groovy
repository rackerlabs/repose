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
package features.filters.ipclassification

import framework.ReposeValveTest
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.Handling
import org.rackspace.deproxy.MessageChain

class IpClassificationTest extends ReposeValveTest {

    def setupSpec() {
        deproxy = new Deproxy()
        def params = properties.defaultTemplateParams
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/filters/ipclassification", params)
        repose.start()

        //Just set up a simple endpoint
        deproxy.addEndpoint(properties.targetPort, 'origin service')
    }

    def cleanupSpec() {
        if (deproxy) {
            deproxy.shutdown()
        }

        if (repose) {
            repose.stop()
        }
    }

    def "classifying a request by its IP"() {
        when: "Request is sent through repose"
        def mc = deproxy.makeRequest(url: reposeEndpoint, method: 'get')
        def sentRequest = ((MessageChain) mc).handlings[0]

        then: "Repose will send x-pp-group with the configured value"
        mc.handlings.size() == 1

        //TODO: do we want this?
        //and: "Repose will send x-pp-user based on requestor ip"
        ((Handling) sentRequest).request.getHeaders().findAll("x-pp-user").size() == 1
        def user = sentRequest.request.headers.getFirstValue("x-pp-user")
        //The possible IPv4 addresses
        user == "127.0.0.1;q=0.4" || user == "0:0:0:0:0:0:0:1;q=0.4" || user == "::1;q=0.4"

        and: "Repose will send x-pp-groups with the configured value"
        ((Handling) sentRequest).request.headers.getFirstValue("x-pp-groups").equalsIgnoreCase("local-group;q=0.4")
    }

    def "verify classifying with other config"() {
        def params = properties.defaultTemplateParams
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/filters/ipclassification", params)
        repose.configurationProvider.applyConfigs("features/filters/ipclassification/otherconfig", params)
        sleep(10000)

        when: "Request is sent through repose"
        def mc = deproxy.makeRequest(url: reposeEndpoint, method: 'get')
        def sentRequest = ((MessageChain) mc).handlings[0]

        then: "Repose will send x-pp-group with the configured value"
        mc.handlings.size() == 1

        ((Handling) sentRequest).request.getHeaders().findAll("x-pp-user").size() == 1
        def user = sentRequest.request.headers.getFirstValue("x-pp-user")
        //The possible IPv4 addresses
        user == "127.0.0.1;q=0.7" || user == "0:0:0:0:0:0:0:1;q=0.7" || user == "::1;q=0.7"

        and: "Repose will send x-pp-groups with the configured value"
        ((Handling) sentRequest).request.headers.getFirstValue("x-pp-groups").equalsIgnoreCase("local-group;q=0.5")
    }

    def "classifying with multi-match config only get the 1st match"() {
        def params = properties.defaultTemplateParams
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/filters/ipclassification", params)
        repose.configurationProvider.applyConfigs("features/filters/ipclassification/multigroupsmatch", params)
        sleep(10000)

        when: "Request is sent through repose"
        def mc = deproxy.makeRequest(url: reposeEndpoint, method: 'get')
        def sentRequest = ((MessageChain) mc).handlings[0]

        then: "Repose will send x-pp-group with the configured value"
        mc.handlings.size() == 1

        ((Handling) sentRequest).request.getHeaders().findAll("x-pp-user").size() == 1
        def user = sentRequest.request.headers.getFirstValue("x-pp-user")
        //The possible IPv4 addresses
        user == "127.0.0.1;q=0.7" || user == "0:0:0:0:0:0:0:1;q=0.7" || user == "::1;q=0.7"

        and: "Repose will send x-pp-groups with the configured value"
        ((Handling) sentRequest).request.headers.getFirstValue("x-pp-groups").equalsIgnoreCase("match-all-group;q=0.5")
        !((Handling) sentRequest).request.headers.getFirstValue("x-pp-groups").equalsIgnoreCase("local-group;q=0.5")
        !((Handling) sentRequest).request.headers.getFirstValue("x-pp-groups").equalsIgnoreCase("local-lan-ip;q=0.5")
    }
}
