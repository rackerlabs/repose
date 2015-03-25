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

import framework.ReposeConfigurationProvider
import framework.ReposeValveLauncher
import framework.TestProperties
import org.rackspace.deproxy.Deproxy
import spock.lang.Specification

class NoCaptureGroupsTest extends Specification {

    static Deproxy deproxy

    static TestProperties properties
    static ReposeConfigurationProvider reposeConfigProvider
    static ReposeValveLauncher repose

    def setupSpec() {

        properties = new TestProperties()
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort)

        reposeConfigProvider = new ReposeConfigurationProvider(properties.configDirectory, properties.configTemplates)

        def params = properties.getDefaultTemplateParams()
        reposeConfigProvider.cleanConfigDirectory()
        reposeConfigProvider.applyConfigs("common", params)
        reposeConfigProvider.applyConfigs("features/filters/ratelimiting/nocapturegroups", params)
        repose = new ReposeValveLauncher(
                reposeConfigProvider,
                properties.reposeJar,
                properties.reposeEndpoint,
                properties.configDirectory,
                properties.reposePort
        )
        repose.enableDebug()
        repose.start(killOthersBeforeStarting: false,
                waitOnJmxAfterStarting: false)
        repose.waitForNon500FromUrl(properties.reposeEndpoint)
    }

    def "Urls that match the same pattern should go in the same bucket"() {

        given:

        def mc
        String url1 = "${properties.reposeEndpoint}/objects/abc/things/123"
        String url2 = "${properties.reposeEndpoint}/objects/def/things/456"
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

    def "Capture groups () should make no difference"() {

        given:

        def mc
        String url1 = "${properties.reposeEndpoint}/servers/abc/instances/123"
        String url2 = "${properties.reposeEndpoint}/servers/def/instances/456"
        def headers = ['X-PP-User': 'user5', 'X-PP-Groups': 'captures']


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


    def cleanupSpec() {

        if (repose) {
            repose.stop()
        }

        if (deproxy) {
            deproxy.shutdown()
        }
    }
}
