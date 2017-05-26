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
package features.filters.headertranslation

import org.openrepose.framework.test.ReposeValveTest
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain
import spock.lang.Unroll

/**
 * Created by jennyvo on 4/13/16.
 */

class HeaderTranslationSchemaAssertionTest extends ReposeValveTest {
    def static params = [:]
    def setupSpec() {
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort)
        reposeLogSearch.cleanLog()

        params = properties.defaultTemplateParams
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/filters/headertranslation/badconfig", params)
        repose.start()
    }

    @Unroll("Test with method:#method and #reqHeaders")
    def "when translating request headers one-to-one without removal"() {

        when: "client passes a request through repose with headers to be translated"
        MessageChain mc = deproxy.makeRequest(url: (String) reposeEndpoint, method: method, headers: reqHeaders)

        then: "filter failed to initialize"
        mc.receivedResponse.code == "503"
        reposeLogSearch.searchByString("Assertion failed for schema type 'header-translationType'. Original names must be unique. Evaluation is case insensitive.").size() == 1

        where:
        method | reqHeaders
        "POST" | ["Content-type": "a", "Content-Type": "b"]
        "GET"  | ["Content-type": "a", "Content-Type": "b"]
    }

    @Unroll("Test with method:#method and #reqHeaders")
    def "Change config verify quality assertion" () {
        setup: "update config"
        repose.configurationProvider.applyConfigs("features/filters/headertranslation/badconfig", params)
        repose.configurationProvider.applyConfigs("features/filters/headertranslation/badconfig/quality", params)
        repose.start()

        when: "client passes a request through repose with headers to be translated"
        MessageChain mc = deproxy.makeRequest(url: (String) reposeEndpoint, method: method, headers: reqHeaders)

        then: "filter failed to initialize"
        mc.receivedResponse.code == "400"
        reposeLogSearch.searchByString("Value '-1.0' is not facet-valid with respect to minInclusive '0.0E1' for type 'doubleBetweenZeroAndOne'").size() > 0

        where:
        method | reqHeaders
        "POST" | ["Content-type": "a", "Content-length": "b"]
        "GET"  | ["Content-length": "b", "repose-test": "test"]
    }
}
