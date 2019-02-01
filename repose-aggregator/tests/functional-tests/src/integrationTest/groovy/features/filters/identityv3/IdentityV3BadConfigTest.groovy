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
import org.rackspace.deproxy.Deproxy
import scaffold.category.Filters

import static javax.servlet.http.HttpServletResponse.SC_SERVICE_UNAVAILABLE

@Category(Filters)
class IdentityV3BadConfigTest extends ReposeValveTest {
    def static originEndpoint
    def static identityEndpoint

    def setupSpec() {
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort, 'origin service')
    }

    def "should fail to start with bad config"() {
        given: "Repose with a bad configuration"
        def params = properties.defaultTemplateParams
        repose.configurationProvider.cleanConfigDirectory()
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/filters/identityv3/common", params)
        repose.configurationProvider.applyConfigs("features/filters/identityv3/badconfig", params)
        repose.start()

        when: "Request is sent to repose"
        def mc = deproxy.makeRequest(url: reposeEndpoint, method: 'get')

        then: "Repose will respond appropriately"
        mc.receivedResponse.code as Integer == SC_SERVICE_UNAVAILABLE

        and: "will log the failure reasons"
        reposeLogSearch.searchByString("The content of element 'openstack-identity-v3' is not complete.").size() > 0
        reposeLogSearch.searchByString("OpenStackIdentityV3Filter - Filter has not yet initialized.").size() > 0
    }
}
