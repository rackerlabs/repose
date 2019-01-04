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
package features.services.httpconnectionpool

import org.junit.experimental.categories.Category
import org.openrepose.framework.test.ReposeValveTest
import scaffold.category.Slow
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.Handlers
import org.rackspace.deproxy.MessageChain

class ConnectionPoolDecommissioningTest extends ReposeValveTest {

    def setupSpec() {
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort)
    }

    def setup() {
        reposeLogSearch.cleanLog()
    }

    def "on startup, HttpClientService should log out when pools are created"() {

        given:
        def params = properties.getDefaultTemplateParams()
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/services/httpconnectionpool/common", params)
        repose.configurationProvider.applyConfigs("features/services/httpconnectionpool/decommissioned/onepool", params)
        repose.start()

        when: "Repose is up and the HTTPClientService has been configured"
        waitUntilReadyToServiceRequests()

        then: "HTTPClientService has logged that the conn pool is created"
        def logLines = reposeLogSearch.searchByString("HTTP client default-1 has been created with instance ID .*") //default-1 comes from connection pool config
        logLines.size() == 1

        cleanup:
        repose.stop()
    }

    def "when Repose is reconfigured and a pool is decommissioned, then destroyed after no registered users"() {

        given:
        def params = properties.getDefaultTemplateParams()
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/services/httpconnectionpool/common", params)
        repose.configurationProvider.applyConfigs("features/services/httpconnectionpool/decommissioned/onepool", params)
        repose.start([waitOnJmxAfterStarting: false])

        when: "Repose is up and the HTTPClientService has been reconfigured"
        waitUntilReadyToServiceRequests()
        def createdLog = reposeLogSearch.searchByString("HTTP client default-1 has been created with instance ID .*") //default-1 comes from connection pool config

        and: "The HttpClientService is reconfigured"
        repose.configurationProvider.applyConfigs("features/services/httpconnectionpool/decommissioned/onepool_reconfig", params, /*sleepTime*/ 25)

        then: "The HttpClientService should log the first pool as destroyed"
        println createdLog
        def uuid = createdLog.get(0).tokenize(" ").reverse().get(0) //reverse done to account for different log formatting
        def logLines = reposeLogSearch.searchByString("Successfully decommissioned HTTP client " + uuid)
        logLines.size() == 1

        cleanup:
        repose.stop()
    }

    @Category(Slow)
    def "active connections should stay alive during config changes and log an error"() {
        given:
        def MessageChain messageChain

        def params = properties.getDefaultTemplateParams()
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/services/httpconnectionpool/common", params)
        repose.configurationProvider.applyConfigs("features/services/httpconnectionpool/decommissioned/onepool", params)
        repose.start([waitOnJmxAfterStarting: false])
        waitUntilReadyToServiceRequests()

        when:
        def thread = Thread.start {
            messageChain = deproxy.makeRequest(url: reposeEndpoint, method: 'GET', defaultHandler: Handlers.Delay(25000))
        }

        and:
        repose.configurationProvider.applyConfigs("features/services/httpconnectionpool/decommissioned/onepool_reconfig", params, /*sleepTime*/ 25)
        thread.join()

        then:
        messageChain.receivedResponse.code == "200"

        and:
        def logLines = reposeLogSearch.searchByString("Failed to decommission HTTP client .* as it is still in use")
        logLines.size() > 0

        cleanup:
        repose.stop()
    }

    @Category(Slow)
    def "under heavy load and constant HTTPClientService reconfigures, should not drop in use connections"() {

        given: "Repose is up and the HTTPClientService has been configured"
        def params = properties.getDefaultTemplateParams()
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/services/httpconnectionpool/common", params)
        repose.configurationProvider.applyConfigs("features/services/httpconnectionpool/decommissioned/" + firstConfig, params)
        repose.start([waitOnJmxAfterStarting: false])
        waitUntilReadyToServiceRequests()

        and: "Alot of concurrent users are making requests to Repose"
        List<Thread> clientThreads = new ArrayList<Thread>()

        Random rand = new Random()
        int totalErrors = 0

        for (x in 1..50) {
            println("Starting client: " + x)
            def thread = Thread.start {
                for (y in 1..5) {
                    MessageChain messageChain = deproxy.makeRequest(url: reposeEndpoint, defaultHandler: Handlers.Delay(250 + rand.nextInt(500)))
                    if (messageChain.receivedResponse.code != "200") {
                        println("ERROR: call received an error response")
                        println("RESPONSE CODE: ${messageChain.receivedResponse.code}")
                        println("RESPONSE: " + messageChain.receivedResponse.body)
                        totalErrors++
                    }
                }
            }
            clientThreads.add(thread)
        }

        and: "The HTTP Client Service is continuously being reconfigured"
        def keepReconfiguring = true
        def reconfigureCount = 0
        def reconfigureThread = Thread.start {
            while (keepReconfiguring) {
                println("Reconfiguring...")
                waitUntilReadyToServiceRequests("200", false, true)
                if (reconfigureCount % 2) {
                    repose.configurationProvider.applyConfigs("features/services/httpconnectionpool/decommissioned/" + secondConfig, params, /*sleepTime*/ 25)
                } else {
                    repose.configurationProvider.applyConfigs("features/services/httpconnectionpool/decommissioned/" + firstConfig, params, /*sleepTime*/ 25)
                }
                reconfigureCount++
            }
        }

        when: "All clients have completed their calls"
        clientThreads*.join()

        and: "We stop reconfiguring madness in Repose"
        keepReconfiguring = false
        reconfigureThread.join()

        then: "All client calls should have succeeded"
        totalErrors == 0

        cleanup:
        repose.stop()

        where:
        firstConfig | secondConfig
        "onepool"   | "onepool_reconfig"
        "twopool"   | "twopool_reconfig"
    }
}
