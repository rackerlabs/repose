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
package features.filters.derp

import org.junit.Ignore
import org.junit.experimental.categories.Category
import org.openrepose.framework.test.ReposeValveTest
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain
import scaffold.category.Filters

@Ignore
@Category(Filters)
class DerpResponseMessagingTest extends ReposeValveTest {

    def setupSpec() {
        reposeLogSearch.cleanLog()
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort)

        def params = properties.getDefaultTemplateParams()
        repose.configurationProvider.applyConfigs('common', params)
        repose.configurationProvider.applyConfigs('features/filters/derp/responsemessaging', params)
        repose.start(waitOnJmxAfterStarting: false)
        repose.waitForNon500FromUrl(reposeEndpoint)
    }

    def "when a request is delegated, then the derp filter response should be processed by the response messaging service"() {
        when:
        MessageChain messageChain = deproxy.makeRequest(url: getReposeEndpoint(), method: 'GET',
                headers: ['X-Delegated': 'status_code=500`component=foo`message=bar;q=1.0'])

        then:
        messageChain.getHandlings().size() + messageChain.getOrphanedHandlings().size() == 0
        messageChain.getReceivedResponse().getBody().equals('Response messaging caught a 5xx response')

        // Verify Repose not throw error log for non anotation filter
        reposeLogSearch.searchByString("Requested filter, *.DerpFilter is not an annotated Component. Make sure your filter is an annotated Spring Bean.").size() == 0

    }
}
