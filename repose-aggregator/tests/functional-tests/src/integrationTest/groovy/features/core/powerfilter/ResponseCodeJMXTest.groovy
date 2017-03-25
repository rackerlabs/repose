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

import framework.ReposeValveTest
import framework.category.Slow
import org.junit.experimental.categories.Category
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain
import org.rackspace.deproxy.Response
import spock.lang.Ignore
import spock.lang.Unroll
import spock.util.concurrent.PollingConditions

@Category(Slow.class)
class ResponseCodeJMXTest extends ReposeValveTest {

    //One second timeout, initial delay is 0 and the delay is .1, which is every 100ms
    final def conditions = new PollingConditions(timeout: 1)

    String PREFIX = "${jmxHostname}:001=\"org\",002=\"openrepose\",003=\"core\",004=\"ResponseCode\",005=\"Repose\""

    String NAME_2XX = ",006=\"2XX\""
    String REPOSE_2XX = PREFIX + NAME_2XX

    String NAME_5XX = ",006=\"5XX\""
    String REPOSE_5XX = PREFIX + NAME_5XX

    def handler5XX = { request -> return new Response(502, 'WIZARD FAIL') }

    def setupSpec() {
        def params = properties.getDefaultTemplateParams()
        repose.configurationProvider.cleanConfigDirectory()
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/core/powerfilter/common", params)
        repose.start()

        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort)
    }

    // Greg/Dimitry: Is it expected that all2XX and repose2XX are equal?  It's not the sum of repose responses + origin service
    // responses?
    @Unroll("When sending requests, the counters should be incremented: iteration #loop")
    def "when sending requests, response code counters should be incremented"() {
        given:
        // the initial values are equivalent the the number of calls made in the when block
        def repose2XXtarget = repose.jmx.quickMBeanAttribute(REPOSE_2XX, "Count")
        repose2XXtarget = (repose2XXtarget == null) ? 3 : repose2XXtarget + 3
        def responses = []

        when:
        responses.add(deproxy.makeRequest(url: reposeEndpoint + "/endpoint"))
        responses.add(deproxy.makeRequest(url: reposeEndpoint + "/endpoint"))
        responses.add(deproxy.makeRequest(url: reposeEndpoint + "/cluster"))

        then:
        conditions.eventually {
            assert repose.jmx.getMBeanAttribute(REPOSE_2XX, "Count") == repose2XXtarget
            assert repose.jmx.quickMBeanAttribute(REPOSE_5XX, "Count").is(null)
        }

        responses.each { MessageChain mc ->
            assert (mc.receivedResponse.code == "200")
        }

        where:
        loop << (1..500).toArray()
    }

    def "when responses have 2XX and 5XX status codes, should increment 2XX and 5XX mbeans"() {
        given:
        def repose2XXtarget = repose.jmx.quickMBeanAttribute(REPOSE_2XX, "Count")
        repose2XXtarget = (repose2XXtarget == null) ? 1 : repose2XXtarget + 1
        def repose5XXtarget = repose.jmx.quickMBeanAttribute(REPOSE_5XX, "Count")
        repose5XXtarget = (repose5XXtarget == null) ? 1 : repose5XXtarget + 1

        when:
        MessageChain mc1 = deproxy.makeRequest([url: reposeEndpoint + "/endpoint", defaultHandler: handler5XX])
        MessageChain mc2 = deproxy.makeRequest(url: reposeEndpoint + "/cluster")

        then:
        mc1.receivedResponse.code == "502"
        mc2.receivedResponse.code == "200"
        repose.jmx.getMBeanAttribute(REPOSE_2XX, "Count") == repose2XXtarget
        repose.jmx.getMBeanAttribute(REPOSE_5XX, "Count") == repose5XXtarget
    }

    /**
     *  TODO:
     *
     *  1) Need to verify counts for:
     *     - endpoint
     *     - cluster
     *     - All Endpoints
     *  2) Need to verify that Repose 5XX is sum of all non "All Endpoints" 5XX bean
     *  3) Need to verify that Repose 2XX is sum of all non "All Endpoints" 2XX bean
     */
    @Ignore
    def "when sending requests to service cluster, response codes should be recorded"() {

        when:

        // NOTE:  We verify that Repose is up and running by sending a GET request in repose.start()
        // This is logged as well, so we need to add this to our count

        deproxy.makeRequest(url: reposeEndpoint + "/endpoint");
        deproxy.makeRequest(url: reposeEndpoint + "/endpoint");
        deproxy.makeRequest(url: reposeEndpoint + "/cluster");

        def reposeCount = repose.jmx.getMBeanAttribute(REPOSE_2XX, "Count")

        then:
        reposeCount == 4
    }
}
