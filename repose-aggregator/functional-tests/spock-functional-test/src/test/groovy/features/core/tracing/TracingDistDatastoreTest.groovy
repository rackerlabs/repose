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

import framework.ReposeValveTest
import framework.mocks.MockIdentityService
import org.joda.time.DateTime
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain
import org.rackspace.deproxy.PortFinder

/**
 * Specific tests for admin token
 */
class TracingDistDatastoreTest extends ReposeValveTest {

    def static originEndpoint
    def static identityEndpoint

    def static MockIdentityService fakeIdentityService

    static int dataStorePort

    def setupSpec() {

        deproxy = new Deproxy()
        dataStorePort = PortFinder.Singleton.getNextOpenPort()

        def params = properties.defaultTemplateParams
        params += [
                'datastorePort': dataStorePort,
        ]

        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/core/tracing", params)
        repose.configurationProvider.applyConfigs("features/core/tracing/distdatastore", params)
        reposeLogSearch.cleanLog()

        repose.start()

        originEndpoint = deproxy.addEndpoint(properties.targetPort, 'origin service')
        fakeIdentityService = new MockIdentityService(properties.identityPort, properties.targetPort)
        identityEndpoint = deproxy.addEndpoint(properties.identityPort,
                'identity service', null, fakeIdentityService.handler)

    }

    def cleanupSpec() {
        deproxy.shutdown()

        repose.stop()
    }

    def setup() {
        sleep 500
        fakeIdentityService.resetHandlers()
    }

    def "DistDatastore reads the header for tracing logs"() {
        given:
        //Repose is running

        when:
        //An http request is made to the dist datastore endpoint with a tracing header
        MessageChain mc = deproxy.makeRequest(
                url: "http://localhost:${dataStorePort}/powerapi/dist-datastore/objects/",
                method: 'GET',
                headers: [
                        'X-Trans-Id': "LOLOL"
                ]
        )

        then:
        //That header is used in the log output
        //Don't actually care about the result of the failure

        //Find the GUID out of :  GUID:e6a7f92b-1d22-4f97-8367-7787ccb5f100 - 2015-05-20 12:07:14,045 68669 [qtp172333204-48] DEBUG org.openrepose.filters.clientauth.common.AuthenticationHandler - Uri is /servers/1111/
        List<String> lines = reposeLogSearch.searchByString("GUID:LOLOL - .*SERVICING DISTDATASTORE REQUEST\$")
        lines.size() == 1
    }

}
