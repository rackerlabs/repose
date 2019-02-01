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
package features.filters.experimental.servletContract

import org.junit.experimental.categories.Category
import org.openrepose.framework.test.ReposeValveTest
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain
import org.rackspace.deproxy.Response
import scaffold.category.Filters

@Category(Filters)
class ServletContractFilterTest extends ReposeValveTest {

    /**
     * This test fails because repose does not properly support the servlet filter contract.
     * It should not fail.
     *
     * This test is currently ignored until the splodeDate.
     *
     * It should be corrected in REP-320:
     * https://repose.atlassian.net/browse/REP-320
     */
    def "Proving that a custom filter (although tightly coupled) does in fact work"() {
        setup:
        def params = properties.defaultTemplateParams
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/filters/experimental/servletcontract", params)

        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort)
        def started = true
        repose.start([waitOnJmxAfterStarting: false])

        waitUntilReadyToServiceRequests("200", false, true)

        when:
        MessageChain mc = null
        mc = deproxy.makeRequest(
                [
                        method        : 'GET',
                        url           : reposeEndpoint + "/get",
                        defaultHandler: {
                            new Response(200, null, null, "This should be the body")
                        }
                ])


        then:
        mc.receivedResponse.code == '200'
        mc.receivedResponse.body.contains("<extra> Added by TestFilter, should also see the rest of the content </extra>")
        println(mc.receivedResponse.body)

        cleanup:
        if (started)
            repose.stop()
        if (deproxy != null)
            deproxy.shutdown()

    }
}
