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
package features.core.powerfilter

import org.junit.experimental.categories.Category
import org.openrepose.framework.test.ReposeValveTest
import org.rackspace.deproxy.Deproxy
import scaffold.category.Core
import spock.lang.Unroll

@Category(Core)
class FilterChainDeterminationTest extends ReposeValveTest {

    final static String HEADER_NAME = "Applied-Filters"

    def setupSpec() {
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort, 'origin service')

        def params = properties.defaultTemplateParams
        params += [
            headerName: HEADER_NAME
        ]

        repose.configurationProvider.applyConfigs('common', params)
        repose.configurationProvider.applyConfigs('features/core/powerfilter/chaindetermination/criteria', params)

        repose.start()
        repose.waitForNon500FromUrl(reposeEndpoint)
    }

    @Unroll
    def 'filter application should be determined based on #criterion for request to #method #path with #headers'() {
        when: 'a request is made to Repose'
        def messageChain = deproxy.makeRequest(
            method: method,
            url: reposeEndpoint + path,
            headers: headers
        )

        then: 'the request reaches the origin service with header values matching applied filters'
        messageChain.handlings[0].request.headers.findAll(HEADER_NAME).collect({ it as Integer }) == appliedFilters

        where:
        criterion       | method   | path      | headers                      || appliedFilters
        "method"        | "GET"    | ""        | [:]                          || [1]
        "method"        | "POST"   | ""        | [:]                          || [2]
        "path"          | "DELETE" | "/third"  | [:]                          || [3]
        "path"          | "DELETE" | "/thord"  | [:]                          || [3]
        "path"          | "DELETE" | "/THIRD"  | [:]                          || []
        "headers"       | "DELETE" | ""        | ['test-presence': "foo"]     || [4]
        "headers"       | "DELETE" | ""        | ['tEsT-pReSeNcE': "foo"]     || [4]
        "headers"       | "DELETE" | ""        | ['TEST-PRESENCE': "foo"]     || [4]
        "headers"       | "DELETE" | ""        | ['test-value': "test value"] || [5]
        "headers"       | "DELETE" | ""        | ['TEST-VALUE': "test value"] || [5]
        "headers"       | "DELETE" | ""        | ['TEST-VALUE': "TEST VALUE"] || []
        "methodAndPath" | "DELETE" | "/delete" | [:]                          || [6]
        "methodOrPath"  | "HEAD"   | ""        | [:]                          || [7]
        "methodOrPath"  | "DELETE" | "/head"   | [:]                          || [7]
        "notMethod"     | "PUT"    | ""        | [:]                          || [8]
    }
}
