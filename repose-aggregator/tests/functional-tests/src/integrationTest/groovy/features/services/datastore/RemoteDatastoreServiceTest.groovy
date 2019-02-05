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
import org.openrepose.framework.test.*
import org.rackspace.deproxy.Deproxy
import scaffold.category.Intense
import spock.lang.Specification

import java.util.concurrent.TimeUnit

import static javax.servlet.http.HttpServletResponse.SC_OK
import static javax.servlet.http.HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE
import static org.openrepose.framework.test.ReposeLauncher.MAX_STARTUP_TIME
import static org.springframework.http.HttpStatus.I_AM_A_TEAPOT

@Category(Intense.class)
class RemoteDatastoreServiceTest extends Specification {

    ReposeValveLauncher repose1
    ReposeValveLauncher repose2
    ReposeValveLauncher remoteDatastore

    Deproxy deproxy

    int repose1Port
    int repose2Port
    int reposeRemotePort
    int datastorePort
    int targetPort

    String repose1Endpoint
    String repose2Endpoint
    String reposeRemoteEndpoint

    ReposeLogSearch repose1LogSearch
    ReposeLogSearch repose2LogSearch
    ReposeLogSearch reposeRemoteLogSearch

    def setup() {
        repose1Port = PortFinder.instance.getNextOpenPort()
        repose2Port = PortFinder.instance.getNextOpenPort()
        reposeRemotePort = PortFinder.instance.getNextOpenPort()
        datastorePort = PortFinder.instance.getNextOpenPort()
        targetPort = PortFinder.instance.getNextOpenPort()

        repose1Endpoint = "http://localhost:$repose1Port"
        repose2Endpoint = "http://localhost:$repose2Port"
        reposeRemoteEndpoint = "http://localhost:$reposeRemotePort"

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

        reposeValveLauncher.start(false, false, type)

        return [reposeValveLauncher, reposeLogSearch]
    }

    static def waitUntilReadyToServiceRequests(ReposeLogSearch reposeLogSearch) {
        reposeLogSearch.awaitByString("Repose ready", 1, MAX_STARTUP_TIME, TimeUnit.SECONDS)
    }

    def "When a limit has not been reached, request should pass"() {
        given: "three Repose instances are started, two to handle traffic, one to act as the remote datastore"
        (remoteDatastore, reposeRemoteLogSearch) =
            startRepose('remote', reposeRemotePort, targetPort, datastorePort, false)
        (repose1, repose1LogSearch) = startRepose('repose1', repose1Port, targetPort, datastorePort, true)
        (repose2, repose2LogSearch) = startRepose('repose2', repose2Port, targetPort, datastorePort, true)

        and: "they are ready to service requests"
        waitUntilReadyToServiceRequests(reposeRemoteLogSearch)
        waitUntilReadyToServiceRequests(repose1LogSearch)
        waitUntilReadyToServiceRequests(repose2LogSearch)

        and: "the rate-limit has not been reached"
        def headers = ["X-PP-User": "user", "X-PP-Groups": "group"]

        when: "the user sends 10 requests total (5 to each Repose instance)"
        def manyMessageChains1 = (1..5).collect { deproxy.makeRequest(url: repose1Endpoint, headers: headers) }
        def manyMessageChains2 = (1..5).collect { deproxy.makeRequest(url: repose2Endpoint, headers: headers) }

        then: "the requests are not rate-limited and pass to the origin service"
        manyMessageChains1.every { it.receivedResponse.code as Integer == SC_OK }
        manyMessageChains2.every { it.receivedResponse.code as Integer == SC_OK }
        manyMessageChains1.every { it.handlings.size() == 1 }
        manyMessageChains2.every { it.handlings.size() == 1 }

        when: "the user sends their request after the rate-limit has been reached"
        def messageChain1 = deproxy.makeRequest(url: repose1Endpoint, headers: headers)
        def messageChain2 = deproxy.makeRequest(url: repose2Endpoint, headers: headers)

        then: "the requests are rate-limited"
        messageChain1.receivedResponse.code as Integer == SC_REQUEST_ENTITY_TOO_LARGE
        messageChain2.receivedResponse.code as Integer == SC_REQUEST_ENTITY_TOO_LARGE

        and: "the requests do not pass to the origin service"
        messageChain1.handlings.size() == 0
        messageChain2.handlings.size() == 0
    }

    def "Rate limits survive Repose swap outs if the remote datastore remains running"() {
        given: "the remote datastore and a Repose instance are started"
        (repose1, repose1LogSearch) = startRepose('repose1', repose1Port, targetPort, datastorePort, true)
        (remoteDatastore, reposeRemoteLogSearch) =
            startRepose('remote', reposeRemotePort, targetPort, datastorePort, false)

        and: "they are ready to service requests"
        waitUntilReadyToServiceRequests(repose1LogSearch)
        waitUntilReadyToServiceRequests(reposeRemoteLogSearch)

        and: "requests will be made using the rate limiting group allowing 10 requests per hour"
        def headers = ["X-PP-User": "user", "X-PP-Groups": "10_per_hour"]

        when: "the user sends 10 requests to the Repose instance that is running"
        def messageChains = (1..10).collect { deproxy.makeRequest(url: repose1Endpoint, headers: headers) }

        then: "all 10 requests are successful"
        messageChains.every { it.receivedResponse.code as Integer == SC_OK }
        messageChains.every { it.handlings.size() == 1 }

        when: "the Repose instance is stopped"
        repose1.stop()

        and: "another Repose instance is started and is ready to service requests"
        (repose2, repose2LogSearch) = startRepose('repose2', repose2Port, targetPort, datastorePort, true)
        waitUntilReadyToServiceRequests(repose2LogSearch)

        and: "the user sends their request to the new Repose instance that is running after the rate-limit has been reached"
        def messageChain = deproxy.makeRequest(url: repose2Endpoint, headers: headers)

        then: "the request is rate-limited"
        messageChain.receivedResponse.code as Integer == SC_REQUEST_ENTITY_TOO_LARGE

        and: "the request does not pass to the origin service"
        messageChain.handlings.size() == 0
    }

    def "Repose instances should reconnect to the remote datastore when the remote datastore is restarted"() {
        given: "three Repose instances are started, two to handle traffic, one to act as the remote datastore"
        (repose1, repose1LogSearch) = startRepose('repose1', repose1Port, targetPort, datastorePort, true)
        (repose2, repose2LogSearch) = startRepose('repose2', repose2Port, targetPort, datastorePort, true)
        (remoteDatastore, reposeRemoteLogSearch) =
            startRepose('remote', reposeRemotePort, targetPort, datastorePort, false)

        and: "they are ready to service requests"
        waitUntilReadyToServiceRequests(repose1LogSearch)
        waitUntilReadyToServiceRequests(repose2LogSearch)
        waitUntilReadyToServiceRequests(reposeRemoteLogSearch)

        and: "requests will be made using the rate limiting group allowing 10 requests per hour"
        def headers = ["X-PP-User": "user", "X-PP-Groups": "10_per_hour"]

        when: "the user sends a request to each Repose instance"
        def singleMessageChain1 = deproxy.makeRequest(url: repose1Endpoint, headers: headers)
        def singleMessageChain2 = deproxy.makeRequest(url: repose2Endpoint, headers: headers)

        then: "the requests are not rate-limited and pass to the origin service"
        singleMessageChain1.receivedResponse.code as Integer == SC_OK
        singleMessageChain2.receivedResponse.code as Integer == SC_OK
        singleMessageChain1.handlings.size() == 1
        singleMessageChain2.handlings.size() == 1

        when: "the remote datastore is stopped"
        remoteDatastore.stop()

        and: "the user sends 10 requests to each Repose instance"
        def manyMessageChains1 = (1..10).collect { deproxy.makeRequest(url: repose1Endpoint, headers: headers) }
        def manyMessageChains2 = (1..10).collect { deproxy.makeRequest(url: repose2Endpoint, headers: headers) }

        then: "all 20 requests are successful since they should be using their local datastore now"
        manyMessageChains1.every { it.receivedResponse.code as Integer == SC_OK }
        manyMessageChains2.every { it.receivedResponse.code as Integer == SC_OK }
        manyMessageChains1.every { it.handlings.size() == 1 }
        manyMessageChains2.every { it.handlings.size() == 1 }

        when: "the remote datastore is started again and is ready to service requests"
        (remoteDatastore, reposeRemoteLogSearch) =
            startRepose('remote', reposeRemotePort, targetPort, datastorePort, false)
        waitUntilReadyToServiceRequests(reposeRemoteLogSearch)

        and: "the user sends 10 requests total (5 to each Repose instance)"
        manyMessageChains1 = (1..5).collect { deproxy.makeRequest(url: repose1Endpoint, headers: headers) }
        manyMessageChains2 = (1..5).collect { deproxy.makeRequest(url: repose2Endpoint, headers: headers) }

        then: "all 10 requests are successful since they should be using the remote datastore again"
        manyMessageChains1.every { it.receivedResponse.code as Integer == SC_OK }
        manyMessageChains2.every { it.receivedResponse.code as Integer == SC_OK }
        manyMessageChains1.every { it.handlings.size() == 1 }
        manyMessageChains2.every { it.handlings.size() == 1 }

        when: "the user sends a request after the rate-limit has been reached"
        singleMessageChain1 = deproxy.makeRequest(url: repose1Endpoint, headers: headers)
        singleMessageChain2 = deproxy.makeRequest(url: repose2Endpoint, headers: headers)

        then: "the request is rate-limited"
        singleMessageChain1.receivedResponse.code as Integer == SC_REQUEST_ENTITY_TOO_LARGE
        singleMessageChain2.receivedResponse.code as Integer == SC_REQUEST_ENTITY_TOO_LARGE

        and: "the request does not pass to the origin service"
        singleMessageChain1.handlings.size() == 0
        singleMessageChain2.handlings.size() == 0
    }

    def "Repose instances should continue rate limiting (with reset limits) using a local cache when the remote datastore goes down"() {
        given: "three Repose instances are started, two to handle traffic, one to act as the remote datastore"
        (repose1, repose1LogSearch) = startRepose('repose1', repose1Port, targetPort, datastorePort, true)
        (repose2, repose2LogSearch) = startRepose('repose2', repose2Port, targetPort, datastorePort, true)
        (remoteDatastore, reposeRemoteLogSearch) =
            startRepose('remote', reposeRemotePort, targetPort, datastorePort, false)

        and: "they are ready to service requests"
        waitUntilReadyToServiceRequests(repose1LogSearch)
        waitUntilReadyToServiceRequests(repose2LogSearch)
        waitUntilReadyToServiceRequests(reposeRemoteLogSearch)

        and: "requests will be made using the rate limiting group allowing 10 requests per hour"
        def headers = ["X-PP-User": "user", "X-PP-Groups": "10_per_hour"]

        when: "the user sends a request to each Repose instance"
        def singleMessageChain1 = deproxy.makeRequest(url: repose1Endpoint, headers: headers)
        def singleMessageChain2 = deproxy.makeRequest(url: repose2Endpoint, headers: headers)

        then: "the requests are not rate-limited and pass to the origin service"
        singleMessageChain1.receivedResponse.code as Integer == SC_OK
        singleMessageChain2.receivedResponse.code as Integer == SC_OK
        singleMessageChain1.handlings.size() == 1
        singleMessageChain2.handlings.size() == 1

        when: "the remote datastore is stopped"
        remoteDatastore.stop()

        and: "the user sends 10 requests to each Repose instance"
        def manyMessageChains1 = (1..10).collect { deproxy.makeRequest(url: repose1Endpoint, headers: headers) }
        def manyMessageChains2 = (1..10).collect { deproxy.makeRequest(url: repose2Endpoint, headers: headers) }

        then: "all 20 requests are successful since they should be using their local datastore now"
        manyMessageChains1.every { it.receivedResponse.code as Integer == SC_OK }
        manyMessageChains2.every { it.receivedResponse.code as Integer == SC_OK }
        manyMessageChains1.every { it.handlings.size() == 1 }
        manyMessageChains2.every { it.handlings.size() == 1 }

        when: "the user sends their request after the rate-limit has been reached"
        singleMessageChain1 = deproxy.makeRequest(url: repose1Endpoint, headers: headers)
        singleMessageChain2 = deproxy.makeRequest(url: repose2Endpoint, headers: headers)

        then: "the requests are rate-limited"
        singleMessageChain1.receivedResponse.code as Integer == SC_REQUEST_ENTITY_TOO_LARGE
        singleMessageChain2.receivedResponse.code as Integer == SC_REQUEST_ENTITY_TOO_LARGE

        and: "the requests do not pass to the origin service"
        singleMessageChain1.handlings.size() == 0
        singleMessageChain2.handlings.size() == 0
    }

    def "Remote Datastore can be configured to not allow traffic to the origin service which would bypass rate limiting"() {
        given: "three Repose instances are started, two to handle traffic, one to act as the remote datastore"
        (repose1, repose1LogSearch) = startRepose('repose1', repose1Port, targetPort, datastorePort, true)
        (repose2, repose2LogSearch) = startRepose('repose2', repose2Port, targetPort, datastorePort, true)
        (remoteDatastore, reposeRemoteLogSearch) =
            startRepose('remote', reposeRemotePort, targetPort, datastorePort, false)

        and: "they are ready to service requests"
        waitUntilReadyToServiceRequests(repose1LogSearch)
        waitUntilReadyToServiceRequests(repose2LogSearch)
        waitUntilReadyToServiceRequests(reposeRemoteLogSearch)

        and: "requests will be made using the rate limiting group allowing 10 requests per hour"
        def headers = ["X-PP-User": "user", "X-PP-Groups": "10_per_hour"]

        when: "the user sends a request to the first Repose instance"
        def messageChain = deproxy.makeRequest(url: repose1Endpoint, headers: headers)

        then: "the request is not rate-limited and passes to the origin service"
        messageChain.receivedResponse.code as Integer == SC_OK
        messageChain.handlings.size() == 1

        when: "the user sends a request to the second Repose instance"
        messageChain = deproxy.makeRequest(url: repose2Endpoint, headers: headers)

        then: "the request is not rate-limited and passes to the origin service"
        messageChain.receivedResponse.code as Integer == SC_OK
        messageChain.handlings.size() == 1

        when: "the user sends a request to the remote datastore Repose instance"
        messageChain = deproxy.makeRequest(url: reposeRemoteEndpoint, headers: headers)

        then: "the request fails with the configured status code and does not get to the origin service"
        messageChain.receivedResponse.code as Integer == I_AM_A_TEAPOT.value()
        messageChain.handlings.size() == 0
    }
}
