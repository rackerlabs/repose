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
package features.core.jettycontainerconfig

import org.junit.experimental.categories.Category
import org.openrepose.framework.test.ReposeValveTest
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain
import scaffold.category.Core

/**
 * Created by jennyvo on 6/1/16.
 *   Verify expo some of jetty config item
 */
@Category(Core)
class JettyConfigTest extends ReposeValveTest {

    def static params = [:]

    def setupSpec() {
        reposeLogSearch.cleanLog()
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort)

        params = properties.getDefaultTemplateParams()
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/core/jettycontainerconfig", params)
        repose.start()
    }

    def "Repose should start and handle request normaly when the jetty server listening on both an HTTP port and an HTTPS port has non-default idleTimeout & soLingerTime"() {
        when:
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint)

        then:
        reposeLogSearch.searchByString("Repose ready").size() > 0
        mc.receivedResponse.code == "200"
    }

    def "reject a config with soLingerTime exceed max (int)"() {
        params = properties.getDefaultTemplateParams()
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/core/jettycontainerconfig/soLingerTimeexceedmax", params)

        when:
        sleep(15000)

        then:
        reposeLogSearch.searchByString("Value '2147483648' is not facet-valid with respect to maxInclusive '2147483647' for type 'int'.").size() > 0
    }

    def "reject a config with idleTimeout exceed max (long)"() {
        params = properties.getDefaultTemplateParams()
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/core/jettycontainerconfig/idleTimeoutexceedmax", params)

        when:
        sleep(15000)

        then:
        reposeLogSearch.searchByString("Value '9223372036854775808' is not facet-valid with respect to maxInclusive '9223372036854775807' for type 'long'.").size() > 0
    }
}
