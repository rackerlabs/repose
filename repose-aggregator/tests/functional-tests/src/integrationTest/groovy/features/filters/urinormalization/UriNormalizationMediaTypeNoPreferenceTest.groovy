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
package features.filters.urinormalization

import org.junit.experimental.categories.Category
import org.openrepose.framework.test.ReposeValveTest
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain
import scaffold.category.Filters
import spock.lang.Unroll

@Category(Filters)
class UriNormalizationMediaTypeNoPreferenceTest extends ReposeValveTest {
    def setupSpec() {
        def params = properties.defaultTemplateParams

        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort)

        repose.configurationProvider.applyConfigs('common', params)
        repose.configurationProvider.applyConfigs('features/filters/uriNormalization', params)
        repose.configurationProvider.applyConfigs('features/filters/uriNormalization/mediavariantsnopreference', params)

        repose.start()
    }

    @Unroll
    def "the first configured media variant should be preferred"() {
        when:
        MessageChain messageChain = deproxy.makeRequest(
            method: "GET",
            url: reposeEndpoint,
            path: "/path$mediaExtension",
            headers: accept ? ["Accept": accept] : [:]
        )

        then:
        messageChain.handlings.size() == 1
        messageChain.handlings[0].request.path == expectedPath
        messageChain.handlings[0].request.headers.getFirstValue("Accept") == "application/json"

        where:
        mediaExtension | accept || expectedPath
        ""             | null   || "/path"
        ""             | "*/*"  || "/path"
        ".123"         | null   || "/path.123"
        ".123"         | "*/*"  || "/path.123"
        ".json"        | null   || "/path"
        ".json"        | "*/*"  || "/path"
    }
}
