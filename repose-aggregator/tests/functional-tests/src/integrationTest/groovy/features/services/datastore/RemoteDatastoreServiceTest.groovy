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

import framework.*
import framework.category.Slow
import org.junit.experimental.categories.Category
import org.rackspace.deproxy.Deproxy
import spock.lang.Specification

import java.util.concurrent.TimeUnit

import static javax.servlet.http.HttpServletResponse.SC_OK
import static javax.servlet.http.HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE

@Category(Slow.class)
class RemoteDatastoreServiceTest extends Specification {

    ReposeValveLauncher repose1
    ReposeValveLauncher repose2
    ReposeValveLauncher remoteDatastore

    Deproxy deproxy

    int repose1Port
    int repose2Port
    int remoteDatastorePort
    int datastorePort
    int targetPort

    String repose1Endpoint
    String repose2Endpoint
    String remoteDatastoreEndpoint

    ReposeLogSearch repose1LogSearch
    ReposeLogSearch repose2LogSearch
    ReposeLogSearch remoteDatastoreLogSearch

    def setup() {
        repose1Port = PortFinder.instance.getNextOpenPort()
        repose2Port = PortFinder.instance.getNextOpenPort()
        remoteDatastorePort = PortFinder.instance.getNextOpenPort()
        datastorePort = PortFinder.instance.getNextOpenPort()
        targetPort = PortFinder.instance.getNextOpenPort()

        repose1Endpoint = "http://localhost:$repose1Port"
        repose2Endpoint = "http://localhost:$repose2Port"
        remoteDatastoreEndpoint = "http://localhost:$datastorePort"

        deproxy = new Deproxy()
        deproxy.addEndpoint(targetPort, 'origin service')
    }

    def cleanup() {
        deproxy?.shutdown()
        repose1?.stop()
        repose2?.stop()
        remoteDatastore?.stop()
    }

    static def startRepose(String subName, int reposePort, int targetPort, int datastorePort, boolean client) {
        def testProperties = new TestProperties(RemoteDatastoreServiceTest.canonicalName.replace('.', '/') + "/$subName")
        testProperties.setReposePort(reposePort)
        testProperties.setTargetPort(targetPort)

        def reposeConfigProvider = new ReposeConfigurationProvider(testProperties)
        def reposeValveLauncher = new ReposeValveLauncher(reposeConfigProvider, testProperties)
        reposeValveLauncher.enableDebug()

        def reposeLogSearch = new ReposeLogSearch(testProperties.getLogFile())
        reposeLogSearch.cleanLog()

        def type = client ? "client" : "datastore"
        def params = testProperties.getDefaultTemplateParams() + [datastorePort: datastorePort]
        reposeValveLauncher.configurationProvider.cleanConfigDirectory()
        reposeValveLauncher.configurationProvider.applyConfigs("common", params)
        reposeValveLauncher.configurationProvider.applyConfigs("features/services/datastore/remote", params)
        reposeValveLauncher.configurationProvider.applyConfigs("features/services/datastore/remote/$type", params)

        reposeValveLauncher.start(false, false, "repose", type)

        return [reposeValveLauncher, reposeLogSearch]
    }

    static def waitUntilReadyToServiceRequests(ReposeLogSearch reposeLogSearch) {
        reposeLogSearch.awaitByString("Repose ready", 1, 60, TimeUnit.SECONDS)
    }

    def "When a limit has not been reached, request should pass"() {
        given: "three Repose instances are started, two to handle traffic, one to act as the remote datastore"
        (repose1, repose1LogSearch) = startRepose('repose1', repose1Port, targetPort, datastorePort, true)
        (repose2, repose2LogSearch) = startRepose('repose2', repose2Port, targetPort, datastorePort, true)
        (remoteDatastore, remoteDatastoreLogSearch) =
            startRepose('remote', remoteDatastorePort, targetPort, datastorePort, false)

        and: "the Repose instances are ready to service requests"
        waitUntilReadyToServiceRequests(repose1LogSearch)
        waitUntilReadyToServiceRequests(repose2LogSearch)
        waitUntilReadyToServiceRequests(remoteDatastoreLogSearch)

        and: "the rate-limit has not been reached"
        def headers = ["X-PP-User": "user", "X-PP-Groups": "group"]
        def messageChain1
        def messageChain2

        when: "the user sends their request"
        5.times {
            messageChain1 = deproxy.makeRequest(url: repose1Endpoint, headers: headers)
            messageChain2 = deproxy.makeRequest(url: repose2Endpoint, headers: headers)

            then: "the request is not rate-limited, and passes to the origin service"
            assert messageChain1.receivedResponse.code as Integer == SC_OK
            assert messageChain1.handlings.size() == 1
            assert messageChain2.receivedResponse.code as Integer == SC_OK
            assert messageChain2.handlings.size() == 1
        }

        and: "the user sends their request after the rate-limit has been reached"
        messageChain1 = deproxy.makeRequest(url: repose1Endpoint, headers: headers)
        messageChain2 = deproxy.makeRequest(url: repose2Endpoint, headers: headers)

        then: "the request is rate-limited, and passes to the origin service"
        messageChain1.receivedResponse.code as Integer == SC_REQUEST_ENTITY_TOO_LARGE
        messageChain1.handlings.size() == 0
        messageChain2.receivedResponse.code as Integer == SC_REQUEST_ENTITY_TOO_LARGE
        messageChain2.handlings.size() == 0
    }
}
