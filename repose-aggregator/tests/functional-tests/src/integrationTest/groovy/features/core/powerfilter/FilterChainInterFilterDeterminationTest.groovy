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
import spock.lang.Ignore

@Category(Core)
class FilterChainInterFilterDeterminationTest extends ReposeValveTest {

    def setupSpec() {
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort, 'origin service')

        def params = properties.defaultTemplateParams
        repose.configurationProvider.applyConfigs('common', params)
        repose.configurationProvider.applyConfigs('features/core/powerfilter/chaindetermination/interfilter', params)

        repose.start()
        repose.waitForNon500FromUrl(reposeEndpoint)
    }

    @Ignore('todo: un-ignore this test once the filter chain is not longer statically determined')
    def 'filter application should be determined between filters'() {
        given: 'a path that initially matches all configured filter URI regular expressions'
        def path = '/0/1'

        when: 'a request is made to Repose to be modified by the first filter'
        def messageChain = deproxy.makeRequest(
            url: reposeEndpoint,
            path: path,
        )

        then: 'only the first filter processes the request'
        messageChain.handlings[0].request.path ==~ '/1/?'
    }
}
