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
package features.filters.keystonev2.connectionpooling

import org.junit.experimental.categories.Category
import org.openrepose.framework.test.ReposeValveTest
import org.openrepose.framework.test.mocks.MockIdentityV2Service
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.Endpoint
import org.rackspace.deproxy.Handling
import scaffold.category.Identity
import spock.lang.Shared

/**
 * Created with IntelliJ IDEA.
 * User: izrik
 *
 */
@Category(Identity)
class KeystoneV2ConnectionPoolingTest extends ReposeValveTest {

    @Shared
    MockIdentityV2Service fakeIdentityV2Service

    @Shared
    Endpoint identityEndpoint

    def setupSpec() {
        fakeIdentityV2Service = new MockIdentityV2Service(properties.identityPort, properties.targetPort)

        // start deproxy
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort)
        identityEndpoint = deproxy.addEndpoint(properties.identityPort,
                "identity", "localhost", fakeIdentityV2Service.handler)

        // configure and start repose
        def params = properties.getDefaultTemplateParams()
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/filters/keystonev2/connectionpooling", params)
        repose.configurationProvider.applyConfigs("features/filters/keystonev2/connectionpooling2", params)
        repose.start()
    }

    def "when a client makes requests, Repose should re-use the connection to the Identity service"() {

        setup: "craft an url to a resource that requires authentication"
        def url = "${reposeEndpoint}/servers/tenantid/resource"


        when: "making two authenticated requests to Repose"
        def mc1 = deproxy.makeRequest(url: url, headers: ['X-Auth-Token': 'token1'])
        def mc2 = deproxy.makeRequest(url: url, headers: ['X-Auth-Token': 'token2'])
        // collect all of the handlings that make it to the identity endpoint into one list
        def allOrphanedHandlings = mc1.orphanedHandlings + mc2.orphanedHandlings
        List<Handling> identityHandlings = allOrphanedHandlings.findAll { it.endpoint == identityEndpoint }
        def commons = allOrphanedHandlings.intersect(identityHandlings)
        def diff = allOrphanedHandlings.plus(identityHandlings)
        diff.removeAll(commons)


        then: "the connections for Repose's request to Identity should have the same id"

        mc1.orphanedHandlings.size() > 0
        mc2.orphanedHandlings.size() > 0
        identityHandlings.size() > 0
        // there should be no requests to auth with a different connection id
        diff.size() == 0
        // REP-3577 - add header using connection pool
        mc1.orphanedHandlings.get(0).request.headers.contains("auth-proxy")
        mc1.orphanedHandlings.get(0).request.headers.getFirstValue("auth-proxy") == "testing"
    }
}
