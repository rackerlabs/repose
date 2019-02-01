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
package features.services.datastore

import org.junit.experimental.categories.Category
import org.openrepose.framework.test.PortFinder
import org.openrepose.framework.test.ReposeValveTest
import org.rackspace.deproxy.Deproxy
import scaffold.category.Services
import spock.lang.Unroll

import static org.openrepose.framework.test.TestUtils.timedSearch

/**
 * Created by jennyvo on 4/9/14.
 */
@Category(Services)
class DistDataStoreMisConfigTest extends ReposeValveTest {
    static def datastoreEndpoint


    @Unroll("When start data store using config #configuration")
    def "Test data store with wrong config"() {
        given:
        def searchError = "Configuration update error. Reason: "
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort)
        int dataStorePort = PortFinder.instance.getNextOpenPort()
        reposeLogSearch.cleanLog()

        datastoreEndpoint = "http://localhost:${dataStorePort}"

        def params = properties.getDefaultTemplateParams()
        params += [
                'datastorePort': dataStorePort
        ]
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/services/datastore", params)
        repose.configurationProvider.applyConfigs("features/services/datastore/" + configuration, params)

        when:
        repose.start([waitOnJmxAfterStarting: false])
        waitUntilReadyToServiceRequests("503")

        then:
        reposeLogSearch.searchByString("NullPointerException").size() == 0
        timedSearch(10) {
            reposeLogSearch.searchByString(searchError).size() > 0
        }
        timedSearch(10) {
            reposeLogSearch.searchByString(searchMsg).size() > 0
        }

        where:
        configuration        | searchMsg
        "noportconfig"       | "port-config"
        "noportelement"      | "The content of element 'port-config' is not complete"
        "noportattribute"    | "Attribute 'port' must appear on element 'port'"

    }

    def "Test data store with mismatch config"() {
        given:
        def searchError = "Unable to determine Distributed Datastore port for" //nodeId
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort)
        int dataStorePort = PortFinder.instance.getNextOpenPort()
        reposeLogSearch.cleanLog()

        datastoreEndpoint = "http://localhost:${dataStorePort}"

        def params = properties.getDefaultTemplateParams()
        params += [
                'datastorePort': dataStorePort
        ]
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/services/datastore", params)
        repose.configurationProvider.applyConfigs("features/services/datastore/nodemismatch", params)

        when:
        repose.start([waitOnJmxAfterStarting: false])
        waitUntilReadyToServiceRequests("503")

        then:
        reposeLogSearch.searchByString("NullPointerException").size() == 0
        timedSearch(10) {
            reposeLogSearch.searchByString(searchError).size() > 0
        }

    }

    @Unroll("When start data store with port out of range: #port")
    def "Test data store with port out of range"() {
        given:
        def searchError = "Distributed Datastore port out of range: " + port
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort)
        int dataStorePort = port
        reposeLogSearch.cleanLog()

        datastoreEndpoint = "http://localhost:${dataStorePort}"

        def params = properties.getDefaultTemplateParams()
        params += [
                'datastorePort': dataStorePort
        ]
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/services/datastore", params)
        repose.configurationProvider.applyConfigs("features/services/datastore/portranges", params)

        when:
        repose.start([waitOnJmxAfterStarting: false])
        waitUntilReadyToServiceRequests("503")

        then:
        timedSearch(10) {
            reposeLogSearch.searchByString(searchError).size() > 0
        }

        where:
        port << [65536, -3] //-1 is used internally as "I can't find the port"
    }

    @Unroll("When start data store with reserved: #port")
    def "Test start data store with reserved ports"() {
        given:
        def searchError = "Unable to start Distributed Datastore Server instance on ${port}"
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort)
        int dataStorePort = port
        reposeLogSearch.cleanLog()

        datastoreEndpoint = "http://localhost:${dataStorePort}"

        def params = properties.getDefaultTemplateParams()
        params += [
                'datastorePort': dataStorePort
        ]
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/services/datastore", params)
        repose.configurationProvider.applyConfigs("features/services/datastore/portranges", params)

        when:
        repose.start([waitOnJmxAfterStarting: false])
        waitUntilReadyToServiceRequests("503", false)

        then:
        reposeLogSearch.searchByString("NullPointerException").size() == 0
        timedSearch(10) {
            reposeLogSearch.searchByString(searchError).size() > 0
        }

        where:
        port << [21, 22, 23, 1023]

    }

    def cleanup() {
        if (deproxy)
            deproxy.shutdown()

        if (repose)
            repose.stop([throwExceptionOnKill: false])

    }
}
