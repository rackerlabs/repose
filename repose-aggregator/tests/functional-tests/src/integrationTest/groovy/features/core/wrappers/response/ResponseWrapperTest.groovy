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
package features.core.wrappers.response

import framework.ReposeValveTest
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain

class ResponseWrapperTest extends ReposeValveTest {
    static final String REASON_HEADER_NAME = 'X-Reason'
    static final String REASON_PHRASE = 'SOME REASON PHRASE'

    def setupSpec() {
        def params = properties.defaultTemplateParams
        params += [
                'reasonHeaderName': REASON_HEADER_NAME,
                'reasonPhrase'    : REASON_PHRASE
        ]

        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort)

        repose.configurationProvider.applyConfigs('common', params)
        repose.configurationProvider.applyConfigs('features/core/wrappers/response', params)
        repose.start()
        repose.waitForNon500FromUrl(reposeEndpoint)
    }

    def 'the reason phrase used should make it into the reason header'() {
        when: 'any request is made'
        MessageChain messageChain = deproxy.makeRequest(url: reposeEndpoint, method: 'GET')

        then: 'the response received by the user should contain a reason header'
        messageChain.receivedResponse.headers.contains(REASON_HEADER_NAME)
        messageChain.receivedResponse.headers.getFirstValue(REASON_HEADER_NAME) == REASON_PHRASE
    }
}
