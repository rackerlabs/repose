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
package features.core.classloader

import org.openrepose.framework.test.PortFinder
import org.openrepose.framework.test.ReposeConfigurationProvider
import org.openrepose.framework.test.ReposeValveLauncher
import org.openrepose.framework.test.ReposeValveTest
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain

class ClassLoaderTest extends ReposeValveTest {
    static int reposePort
    static String url
    static ReposeConfigurationProvider reposeConfigProvider

    /**
     * copy the bundle from /repose-aggregator/tests/test-bundles/bundle-one/target/
     * and copy the bundle from /repose-aggregator/tests/test-bundles/bundle-two/target/
     * to artifacts directory
     *
     * set up config that has in system model:
     *  filter-one
     *  filter-two
     *
     * start repose with the launcher
     * make a request with header foo. validate that header bar returns
     * make a request with another header.  validate we get a failure back
     *
     * Test Scenario #1: An ear file can access a dependency that is not present in another ear.
     * 1. Create a simple class place it in a jar (JAR 1) which contains a method "createBAR" that returns the string "BAR"
     * 2. EAR 1 has class as a dependency
     * 3. EAR 1 contains a request wrapper the wrapper intercepts calls to get HEADER if the header is "FOO", then the wrapper makes a call to createBAR in the dependent class and returns it's result
     * 4. EAR 1 contains the Foo filter which simply wraps the request and sends it down the chain.
     * 5. EAR 2 contains a filter that simply calls the request and gets the "FOO" header – the expected result is to get "BAR"...otherwise fail.
     * 6. Place both filters in system model EAR 1 filter before EAR 2 Filter
     * 7. Send a request that DOES NOT contain the FOO header
     * 8. That contains a FOO header with a value other than "BAR"
     *
     */
    def "An ear file can access a dependency that is not present in another ear"() {
        deproxy = new Deproxy()
        properties.targetPort = PortFinder.instance.getNextOpenPort()
        deproxy.addEndpoint(properties.targetPort, 'origin service')

        properties.reposePort = PortFinder.instance.getNextOpenPort()
        reposePort = properties.reposePort
        url = "http://localhost:${reposePort}"

        reposeConfigProvider = new ReposeConfigurationProvider(configDirectory, configTemplates)
        repose = new ReposeValveLauncher(
                reposeConfigProvider,
                properties.getReposeJar(),
                url,
                properties.getConfigDirectory(),
                reposePort
        )
        repose.enableDebug()

        def params = properties.getDefaultTemplateParams()

        reposeConfigProvider.cleanConfigDirectory()
        reposeConfigProvider.applyConfigs("common", params)
        reposeConfigProvider.applyConfigs("features/core/classloader/one", params)

        repose.start(killOthersBeforeStarting: false,
                waitOnJmxAfterStarting: false)

        reposeLogSearch.awaitByString("Repose ready", 1, 30)

        when: "make a request with the FOO header"
        def headers = [
                'FOO': 'stuff'
        ]

        MessageChain mc = deproxy.makeRequest(url: url, headers: headers)


        then: "the request header should equal BAR"
        mc.handlings.size() == 1
        mc.receivedResponse.code == "200"

        when: "make a request with the BAR header"
        headers = [
                'BAR': 'stuff'
        ]

        mc = deproxy.makeRequest(url: url, headers: headers)


        then: "the request should bomb"
        mc.handlings.size() == 0
        mc.receivedResponse.code == "500"
        reposeLogSearch.searchByString("IllegalArgumentException").size() > 0
    }

    /**
     * Test Scenario #2: An ear file cannot access a dependency from another ear on its own
     * 1. EAR 3 : contains a filter that simply tries to instantiate the simple class create in filter 1.
     *   The filter does not list the jar as a dependency. (using class.forName to try to instantiate a string)
     * 2. Place EAR 1 before EAR 3 in the system model
     * 3. Send a request
     * 4. Expected result is ClassNotFound
     */
    def "Ensure filter three (in filter-bundle-three) cannot reach a dependency in filter-bundle-one"() {
        deproxy = new Deproxy()
        properties.targetPort = PortFinder.instance.getNextOpenPort()
        deproxy.addEndpoint(properties.targetPort, 'origin service')

        properties.reposePort = PortFinder.instance.getNextOpenPort()
        reposePort = properties.reposePort
        url = "http://localhost:${reposePort}"

        reposeConfigProvider = new ReposeConfigurationProvider(configDirectory, configTemplates)
        repose = new ReposeValveLauncher(
                reposeConfigProvider,
                properties.getReposeJar(),
                url,
                properties.getConfigDirectory(),
                reposePort
        )
        repose.enableDebug()

        def params = properties.getDefaultTemplateParams()

        reposeLogSearch.cleanLog()
        reposeConfigProvider.cleanConfigDirectory()
        reposeConfigProvider.applyConfigs("common", params)
        reposeConfigProvider.applyConfigs("features/core/classloader/two", params)

        repose.start()
        reposeLogSearch.awaitByString("Repose ready", 1, 30)

        when: "make a request with the FOO header"
        def headers = [
                'FOO': 'stuff'
        ]

        MessageChain mc = deproxy.makeRequest(url: url, headers: headers)

        then: "The filter traps the exception and returns successfully"
        mc.handlings.size() == 1
        mc.receivedResponse.code == "200"
    }

    /**
     * start repose with the launcher
     * make a request with header foo. validate that BAR is logged in repose.log
     * validate that BARRR is logged in repose.log
     */

    def "test class loader three"() {
        deproxy = new Deproxy()
        properties.targetPort = PortFinder.instance.getNextOpenPort()
        deproxy.addEndpoint(properties.targetPort, 'origin service')

        properties.reposePort = PortFinder.instance.getNextOpenPort()
        reposePort = properties.reposePort
        url = "http://localhost:${reposePort}"

        reposeConfigProvider = new ReposeConfigurationProvider(configDirectory, configTemplates)
        repose = new ReposeValveLauncher(
                reposeConfigProvider,
                properties.getReposeJar(),
                url,
                properties.getConfigDirectory(),
                reposePort
        )
        repose.enableDebug()

        def params = properties.getDefaultTemplateParams()

        reposeLogSearch.cleanLog()
        reposeConfigProvider.cleanConfigDirectory()
        reposeConfigProvider.applyConfigs("common", params)
        reposeConfigProvider.applyConfigs("features/core/classloader/three", params)

        repose.start(killOthersBeforeStarting: false,
                waitOnJmxAfterStarting: false)

        reposeLogSearch.awaitByString("Repose ready", 1, 30)

        when: "make a request with the FOO header"
        def headers = [
                'FOO': 'stuff'
        ]

        MessageChain mc = deproxy.makeRequest(url: url, headers: headers)


        then: "the request should log BAR and BARRR"
        mc.handlings.size() == 1
        mc.receivedResponse.code == "200"
        reposeLogSearch.searchByString("BAR").size() == 2
        reposeLogSearch.searchByString("BARRR").size() == 1
    }

    def cleanup() {
        if (repose) {
            repose.stop()
        }
        if (deproxy) {
            deproxy.shutdown()
        }
    }
}
