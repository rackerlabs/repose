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
package features.core.tracing

import org.junit.experimental.categories.Category
import org.openrepose.commons.utils.io.ObjectSerializer
import org.openrepose.core.services.datastore.types.StringPatch
import org.openrepose.framework.test.PortFinder
import org.openrepose.framework.test.ReposeValveTest
import org.openrepose.framework.test.mocks.MockIdentityService
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain
import scaffold.category.Core
import spock.lang.Ignore

/**
 * Specific tests for admin token
 */
@Category(Core)
@Ignore("Use the Hazelcast Datastore once it supports OpenTracing")
class TracingDistDatastoreTest extends ReposeValveTest {

    final ObjectSerializer objectSerializer = new ObjectSerializer(this.getClass().getClassLoader())
    def static originEndpoint
    def static identityEndpoint

    def static MockIdentityService fakeIdentityService
    static def distDatastoreEndpoint

    def setupSpec() {

        deproxy = new Deproxy()
        int dataStorePort1 = PortFinder.instance.getNextOpenPort()
        int dataStorePort2 = PortFinder.instance.getNextOpenPort()

        distDatastoreEndpoint = "http://localhost:${dataStorePort1}"

        def params = properties.defaultTemplateParams
        params += [
                'datastorePort1': dataStorePort1,
                'datastorePort2': dataStorePort2
        ]

        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/core/tracing", params)
        repose.configurationProvider.applyConfigs("features/core/tracing/distdatastore", params)
        reposeLogSearch.cleanLog()

        repose.start([clusterId: "repose", nodeId: "nofilters"])

        originEndpoint = deproxy.addEndpoint(properties.targetPort, 'origin service')
        fakeIdentityService = new MockIdentityService(properties.identityPort, properties.targetPort)
        identityEndpoint = deproxy.addEndpoint(properties.identityPort,
                'identity service', null, fakeIdentityService.handler)

    }

    def setup() {
        waitForHttpClientRequestCacheToClear()
        fakeIdentityService.resetHandlers()
    }

    def "DistDatastore reads the header for tracing logs"() {
        given:
        //Repose is running

        when:
        //An http request is made to the dist datastore endpoint with a tracing header
        MessageChain mc = deproxy.makeRequest(
                url: distDatastoreEndpoint + "/powerapi/dist-datastore/objects/",
                method: 'GET',
                headers: [
                        'X-Trans-Id': "LOLOL"
                ]
        )

        then:
        //That header is used in the log output
        //Don't actually care about the result of the failure

        //Find the GUID out of :  Trans-Id:e6a7f92b-1d22-4f97-8367-7787ccb5f100 - 2015-05-20 12:07:14,045 68669 [qtp172333204-48] DEBUG org.openrepose.filters.clientauth.common.AuthenticationHandler - Uri is /servers/1111/
        List<String> lines = reposeLogSearch.searchByString("Trans-Id:LOLOL - .*SERVICING DISTDATASTORE REQUEST\$")
        lines.size() == 1
    }

    def "DistDatastore service request should be added the tracing header"() {
        given:
        //Repose is running
        def key = UUID.randomUUID().toString()
        def body = objectSerializer.writeObject(new StringPatch("test data"))

        when:
        //An http request is made to the dist datastore endpoint with a tracing header
        MessageChain mc = deproxy.makeRequest(
                url: distDatastoreEndpoint + "/powerapi/dist-datastore/objects/" + key,
                method: 'PUT',
                headers: ['X-TTL'     : '10',
                          'X-Trans-Id': 'test12345'],
                requestBody: body
        )
        List<String> lines = reposeLogSearch.searchByString("Trans-Id:test12345 - .*SERVICING DISTDATASTORE REQUEST\$")

        then: "should report success"
        mc.receivedResponse.code == "202"
        mc.receivedResponse.body == ""
        //mc.sentRequest.headers.contains("x-trans-id")
        lines.size() == 1

        when:
        mc = deproxy.makeRequest(
                url: distDatastoreEndpoint + "/powerapi/dist-datastore/objects/" + key,
                method: 'GET',
                headers: ['X-TTL'     : '10',
                          'X-Trans-Id': 'test22345'],
        )
        lines = reposeLogSearch.searchByString("Trans-Id:test22345 - .*SERVICING DISTDATASTORE REQUEST\$")

        then: "should report that it is"
        mc.receivedResponse.code == "200"
        mc.receivedResponse.body == body
        //mc.sentRequest.headers.contains("x-trans-id")
        lines.size() == 1


        when: "deleting the object from the datastore"
        mc = deproxy.makeRequest(
                url: distDatastoreEndpoint + "/powerapi/dist-datastore/objects/" + key,
                method: 'DELETE',
                headers: ['X-TTL'     : '10',
                          'X-Trans-Id': 'test32345'],
        )
        lines = reposeLogSearch.searchByString("Trans-Id:test22345 - .*SERVICING DISTDATASTORE REQUEST\$")

        then: "should report that it was successfully deleted"
        mc.receivedResponse.code == "204"
        mc.receivedResponse.body == ""
        //mc.sentRequest.headers.contains("x-trans-id")

        //Find the GUID out of :  Trans-Id:e6a7f92b-1d22-4f97-8367-7787ccb5f100 - 2015-05-20 12:07:14,045 68669 [qtp172333204-48] DEBUG org.openrepose.filters.clientauth.common.AuthenticationHandler - Uri is /servers/1111/
        lines.size() == 1
    }
}
