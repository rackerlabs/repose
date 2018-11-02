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
package features.core.proxy

import org.openrepose.framework.test.ReposeValveTest
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.Handling
import org.rackspace.deproxy.MessageChain

/**
 * User: dimi5963
 * Date: 9/24/13
 * Time: 3:36 PM
 */
class ChunkedTranslationTest extends ReposeValveTest {

    def static String xmlPayLoad = "<a><remove-me>test</remove-me>somebody</a>"

    def setupSpec() {

        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort)

        def params = properties.getDefaultTemplateParams()
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/filters/translation/common", params)
        repose.configurationProvider.applyConfigs("features/filters/translation/request", params)
        repose.configurationProvider.applyConfigs("features/filters/translation/chunkedfalse", params)
        repose.start()
    }

    def "When set to send chunked encoding to false. Repose should send content length of translated request"() {
        def reqHeaders = ["accept": "application/xml", "content-type": "application/xml"]

        when:
        MessageChain messageChain = deproxy.makeRequest([url    : reposeEndpoint, method: "POST", requestBody: xmlPayLoad,
                                                         headers: reqHeaders])

        def sentRequest = ((MessageChain) messageChain).getHandlings()[0]

        then: "repose should not sending any transfer encoding header"
        ((Handling) sentRequest).request.getHeaders().findAll("transfer-encoding").size() == 0

        and: "repose should send a content-length with the request"
        ((Handling) sentRequest).request.getHeaders().findAll("content-length").size() == 1

        and: "new content length should not match that of the original request"
        ((Handling) sentRequest).request.getHeaders().getFirstValue("content-length") == "15"
        ((Handling) sentRequest).request.getHeaders().getFirstValue("content-length") != messageChain.sentRequest.headers.getFirstValue("content-length")

    }
}

