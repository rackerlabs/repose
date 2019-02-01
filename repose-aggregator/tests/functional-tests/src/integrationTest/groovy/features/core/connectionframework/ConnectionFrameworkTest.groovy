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
package features.core.connectionframework

import org.apache.commons.lang3.StringUtils
import org.junit.experimental.categories.Category
import org.openrepose.framework.test.ReposeValveTest
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain
import scaffold.category.Core

/**
 *  Connection framework tests ported over from python
 */
@Category(Core)
class ConnectionFrameworkTest extends ReposeValveTest {

    def setupSpec() {
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort)

        def params = properties.getDefaultTemplateParams()
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/core/connectionframework", params)
        repose.start()

        waitUntilReadyToServiceRequests()
    }

    def "When accept header is absent"() {
        setup:
        MessageChain messageChain
        def headers = ["Host": "localhost:" + String.valueOf(repose.reposePort), "User-Agent": deproxy.VERSION_STRING]

        when:
        messageChain = deproxy.makeRequest(url: reposeEndpoint + "/", headers: headers, addDefaultHeaders: false)

        then:
        !messageChain.handlings[0].request.headers.contains("accept")
    }

    def "When accept header is empty"() {
        setup:
        MessageChain messageChain
        def headers = ["Host"  : "localhost:" + String.valueOf(repose.reposePort), "User-Agent": deproxy.VERSION_STRING,
                       "Accept": ""]

        when:
        messageChain = deproxy.makeRequest(url: reposeEndpoint + "/", headers: headers, addDefaultHeaders: false)

        then:
        StringUtils.isBlank(messageChain.handlings[0].request.headers.getFirstValue("accept"))
    }

    def "When accept header is asterisks"() {
        setup:
        MessageChain messageChain
        def headers = ["Host"  : "localhost:" + String.valueOf(repose.reposePort), "User-Agent": deproxy.VERSION_STRING,
                       "Accept": "*/*"]

        when:
        messageChain = deproxy.makeRequest(url: reposeEndpoint + "/", headers: headers, addDefaultHeaders: false)

        then:
        messageChain.handlings[0].request.headers.getFirstValue("accept").equals("*/*")
    }

    def "When accept header is type asterisk"() {
        setup:
        MessageChain messageChain
        def headers = ["Host"  : "localhost:" + String.valueOf(repose.reposePort), "User-Agent": deproxy.VERSION_STRING,
                       "Accept": "text/*"]

        when:
        messageChain = deproxy.makeRequest(url: reposeEndpoint + "/", headers: headers, addDefaultHeaders: false)

        then:
        messageChain.handlings[0].request.headers.getFirstValue("accept").equals("text/*")
    }

    def "When accept header is subtype"() {
        setup:
        MessageChain messageChain
        def headers = ["Host"  : "localhost:" + String.valueOf(repose.reposePort), "User-Agent": deproxy.VERSION_STRING,
                       "Accept": "text/plain"]

        when:
        messageChain = deproxy.makeRequest(url: reposeEndpoint + "/", headers: headers, addDefaultHeaders: false)

        then:
        messageChain.handlings[0].request.headers.getFirstValue("accept").equals("text/plain")
    }
}
