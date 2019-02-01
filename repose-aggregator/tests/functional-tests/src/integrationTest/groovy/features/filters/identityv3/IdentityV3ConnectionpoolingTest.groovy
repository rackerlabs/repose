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
package features.filters.identityv3

import org.junit.experimental.categories.Category
import org.openrepose.framework.test.ReposeValveTest
import org.openrepose.framework.test.mocks.MockIdentityV3Service
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.Endpoint
import org.rackspace.deproxy.Handling
import scaffold.category.Filters
import spock.lang.Shared

/**
 * Created by jennyvo on 9/5/14.
 * Test Repose re-use connection to identity service
 */
@Category(Filters)
class IdentityV3ConnectionpoolingTest extends ReposeValveTest {

    @Shared
    MockIdentityV3Service fakeIdentityV3Service

    @Shared
    Endpoint originEndpoint
    @Shared
    Endpoint identityEndpoint

    def setupSpec() {

        fakeIdentityV3Service = new MockIdentityV3Service(properties.identityPort, properties.targetPort)

        // start deproxy
        deproxy = new Deproxy()
        originEndpoint = deproxy.addEndpoint(properties.targetPort)
        identityEndpoint = deproxy.addEndpoint(properties.identityPort,
                "identity", "localhost", fakeIdentityV3Service.handler)

        def params = properties.getDefaultTemplateParams()
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/filters/identityv3/connectionpooling", params)
        repose.configurationProvider.applyConfigs("features/filters/identityv3/connectionpooling2", params)
        repose.start()
    }

    def "when a client makes requests, Repose should re-use the connection to the Identity service"() {

        setup: "craft an url to a resource that requires authentication"
        def url = "${reposeEndpoint}/servers/domainid/resource"

        when: "making two authenticated requests to Repose"
        def mc1 = deproxy.makeRequest(url: url, headers: ['X-Subject-Token': 'token1'])
        def mc2 = deproxy.makeRequest(url: url, headers: ['X-Subject-Token': 'token2'])

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
    }
}
