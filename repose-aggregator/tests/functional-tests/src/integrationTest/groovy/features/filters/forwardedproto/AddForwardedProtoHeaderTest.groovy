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
package features.filters.forwardedproto

import org.junit.experimental.categories.Category
import org.openrepose.framework.test.ReposeValveTest
import org.rackspace.deproxy.Deproxy
import scaffold.category.Filters

/**
 * Created by jcombs on 1/7/15.
 */
@Category(Filters)
class AddForwardedProtoHeaderTest extends ReposeValveTest {

    def setupSpec() {
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort)

        def params = properties.defaultTemplateParams
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/filters/forwardedProto", params)
        repose.start(waitOnJmxAfterStarting: false)
        repose.waitForNon500FromUrl(reposeEndpoint)
    }

    def "When using forwarded-proto filter, Repose addes the x-forwarded-proto header to the request"() {
        given:
        def Map headers = ["x-rax-user": "test-user-a", "x-rax-groups": "reposegroup11"]

        when: "Request contains value(s) of the target header"
        def mc = deproxy.makeRequest([url: reposeEndpoint, headers: headers])

        then: "The x-forwarded-proto header is additionally added to the request going to the origin service"
        mc.getSentRequest().getHeaders().contains("x-rax-user")
        mc.getSentRequest().getHeaders().getFirstValue("x-rax-user") == "test-user-a"
        mc.getSentRequest().getHeaders().contains("x-forwarded-proto") == false
        mc.handlings[0].request.headers.contains("x-rax-user")
        mc.handlings[0].request.headers.getFirstValue("x-rax-user") == "test-user-a"
        mc.handlings[0].request.headers.contains("x-forwarded-proto")
        String forwardedProto = mc.handlings[0].request.headers.getFirstValue("x-forwarded-proto")
        forwardedProto.toLowerCase().contains("http")
    }
}
