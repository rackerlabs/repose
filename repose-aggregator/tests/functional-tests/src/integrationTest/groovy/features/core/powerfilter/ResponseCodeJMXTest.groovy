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
import org.rackspace.deproxy.Response
import scaffold.category.Core
import spock.lang.Unroll
import spock.util.concurrent.PollingConditions

import static javax.servlet.http.HttpServletResponse.*

@Category(Core)
class ResponseCodeJMXTest extends ReposeValveTest {
    private static final String KEY_PROPERTIES_PREFIX = /001="org",002="openrepose",003="core",004="ResponseCode"/
    private static final String REPOSE_ENDPOINT = /005="Repose"/
    private static final String ALL_ENDPOINTS = /005="All Endpoints"/
    private static final String STATUS_2XX = /006="2XX"/
    private static final String STATUS_4XX = /006="4XX"/
    private static final String STATUS_5XX = /006="5XX"/

    private static String rootEndpoint2xxMetric
    private static String rootTwoEndpoint2xxMetric
    private static String repose2xxMetric
    private static String allEndpoints2xxMetric
    private static String rootEndpoint4xxMetric
    private static String repose4xxMetric
    private static String allEndpoints4xxMetric
    private static String rootEndpoint5xxMetric
    private static String repose5xxMetric
    private static String allEndpoints5xxMetric

    def setupSpec() {
        def params = properties.getDefaultTemplateParams()
        repose.configurationProvider.cleanConfigDirectory()
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/core/powerfilter/common", params)
        repose.configurationProvider.applyConfigs("features/core/powerfilter/responsecodejmx", params)
        repose.start()

        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort)

        String rootEndpoint = $/005="localhost:${properties.targetPort}/root_path"/$
        String rootTwoEndpoint = $/005="localhost:${properties.targetPort}/root_path2"/$
        rootEndpoint2xxMetric = "$jmxHostname:$KEY_PROPERTIES_PREFIX,$rootEndpoint,$STATUS_2XX"
        rootTwoEndpoint2xxMetric = "$jmxHostname:$KEY_PROPERTIES_PREFIX,$rootTwoEndpoint,$STATUS_2XX"
        repose2xxMetric = "$jmxHostname:$KEY_PROPERTIES_PREFIX,$REPOSE_ENDPOINT,$STATUS_2XX"
        allEndpoints2xxMetric = "$jmxHostname:$KEY_PROPERTIES_PREFIX,$ALL_ENDPOINTS,$STATUS_2XX"
        rootEndpoint4xxMetric = "$jmxHostname:$KEY_PROPERTIES_PREFIX,$rootEndpoint,$STATUS_4XX"
        repose4xxMetric = "$jmxHostname:$KEY_PROPERTIES_PREFIX,$REPOSE_ENDPOINT,$STATUS_4XX"
        allEndpoints4xxMetric = "$jmxHostname:$KEY_PROPERTIES_PREFIX,$ALL_ENDPOINTS,$STATUS_4XX"
        rootEndpoint5xxMetric = "$jmxHostname:$KEY_PROPERTIES_PREFIX,$rootEndpoint,$STATUS_5XX"
        repose5xxMetric = "$jmxHostname:$KEY_PROPERTIES_PREFIX,$REPOSE_ENDPOINT,$STATUS_5XX"
        allEndpoints5xxMetric = "$jmxHostname:$KEY_PROPERTIES_PREFIX,$ALL_ENDPOINTS,$STATUS_5XX"
    }

    @Unroll("When sending requests, the counters should be incremented: iteration #loop")
    def "when sending requests, response code counters should be incremented"() {
        given: "we know the initial values"
        def rootPath2xxTarget = repose.jmx.getMBeanCountAttribute(rootEndpoint2xxMetric)
        def repose2xxTarget = repose.jmx.getMBeanCountAttribute(repose2xxMetric)
        def all2xxTarget = repose.jmx.getMBeanCountAttribute(allEndpoints2xxMetric)
        def rootPath5xxTarget = repose.jmx.getMBeanCountAttribute(rootEndpoint5xxMetric)
        def repose5xxTarget = repose.jmx.getMBeanCountAttribute(repose5xxMetric)
        def all5xxTarget = repose.jmx.getMBeanCountAttribute(allEndpoints5xxMetric)
        //One second timeout, initial delay is 0 and the delay is .1, which is every 100ms
        def conditions = new PollingConditions(timeout: 1)

        when: "requests are made that should go to the default endpoint"
        def responses = [
                deproxy.makeRequest(url: reposeEndpoint + "/endpoint"),
                deproxy.makeRequest(url: reposeEndpoint + "/endpoint"),
                deproxy.makeRequest(url: reposeEndpoint + "/cluster")]

        then: "the client received a good response code for every request"
        responses.every { it.receivedResponse.code as Integer == SC_OK }

        and: "the 2xx metrics should be incremented by 3"
        conditions.eventually {
            assert repose.jmx.getMBeanCountAttributeWithWaitForNonZero(rootEndpoint2xxMetric) == rootPath2xxTarget + 3
            assert repose.jmx.getMBeanCountAttributeWithWaitForNonZero(repose2xxMetric) == repose2xxTarget + 3
            assert repose.jmx.getMBeanCountAttributeWithWaitForNonZero(allEndpoints2xxMetric) == all2xxTarget + 3
        }

        and: "the 5xx metrics should not be incremented"
        repose.jmx.getMBeanCountAttribute(rootEndpoint5xxMetric) == rootPath5xxTarget
        repose.jmx.getMBeanCountAttribute(repose5xxMetric) == repose5xxTarget
        repose.jmx.getMBeanCountAttribute(allEndpoints5xxMetric) == all5xxTarget

        where:
        loop << (1..500).toArray()
    }

    def "when responses have 2XX and 5XX status codes, should increment 2XX and 5XX mbeans"() {
        given:
        def rootPath2xxTarget = repose.jmx.getMBeanCountAttribute(rootEndpoint2xxMetric)
        def repose2xxTarget = repose.jmx.getMBeanCountAttribute(repose2xxMetric)
        def all2xxTarget = repose.jmx.getMBeanCountAttribute(allEndpoints2xxMetric)
        def rootPath5xxTarget = repose.jmx.getMBeanCountAttribute(rootEndpoint5xxMetric)
        def repose5xxTarget = repose.jmx.getMBeanCountAttribute(repose5xxMetric)
        def all5xxTarget = repose.jmx.getMBeanCountAttribute(allEndpoints5xxMetric)

        when: "requests are made that should go to the default endpoint"
        def mc1 = deproxy.makeRequest(
                url: reposeEndpoint + "/endpoint",
                defaultHandler: { new Response(SC_BAD_GATEWAY) })
        def mc2 = deproxy.makeRequest(url: reposeEndpoint + "/cluster")

        then: "the client received the correct response code"
        mc1.receivedResponse.code as Integer == SC_BAD_GATEWAY
        mc2.receivedResponse.code as Integer == SC_OK

        and: "all of the metrics were incremented by 1"
        repose.jmx.getMBeanCountAttributeWithWaitForNonZero(rootEndpoint2xxMetric) == rootPath2xxTarget + 1
        repose.jmx.getMBeanCountAttributeWithWaitForNonZero(repose2xxMetric) == repose2xxTarget + 1
        repose.jmx.getMBeanCountAttributeWithWaitForNonZero(allEndpoints2xxMetric) == all2xxTarget + 1
        repose.jmx.getMBeanCountAttributeWithWaitForNonZero(rootEndpoint5xxMetric) == rootPath5xxTarget + 1
        repose.jmx.getMBeanCountAttributeWithWaitForNonZero(repose5xxMetric) == repose5xxTarget + 1
        repose.jmx.getMBeanCountAttributeWithWaitForNonZero(allEndpoints5xxMetric) == all5xxTarget + 1
    }

    def "when the response code returned by Repose differs from the origin service response code, mbeans should be incremented appropriately"() {
        given:
        def rootPath2xxTarget = repose.jmx.getMBeanCountAttribute(rootEndpoint2xxMetric)
        def repose2xxTarget = repose.jmx.getMBeanCountAttribute(repose2xxMetric)
        def all2xxTarget = repose.jmx.getMBeanCountAttribute(allEndpoints2xxMetric)
        def rootPath4xxTarget = repose.jmx.getMBeanCountAttribute(rootEndpoint4xxMetric)
        def repose4xxTarget = repose.jmx.getMBeanCountAttribute(repose4xxMetric)
        def all4xxTarget = repose.jmx.getMBeanCountAttribute(allEndpoints4xxMetric)
        def rootPath5xxTarget = repose.jmx.getMBeanCountAttribute(rootEndpoint5xxMetric)
        def repose5xxTarget = repose.jmx.getMBeanCountAttribute(repose5xxMetric)
        def all5xxTarget = repose.jmx.getMBeanCountAttribute(allEndpoints5xxMetric)

        when: "requests are made that should go to the default endpoint"
        def mc1 = deproxy.makeRequest(
                url: reposeEndpoint + "/endpoint",
                defaultHandler: { new Response(SC_BAD_GATEWAY) })
        def mc2 = deproxy.makeRequest(
                url: reposeEndpoint + "/endpoint",
                defaultHandler: { new Response(SC_INTERNAL_SERVER_ERROR) })
        def mc3 = deproxy.makeRequest(url: reposeEndpoint + "/cluster")

        then: "the client received the correct response codes"
        mc1.receivedResponse.code as Integer == SC_BAD_GATEWAY
        mc2.receivedResponse.code as Integer == SC_GONE
        mc3.receivedResponse.code as Integer == SC_OK

        and: "the root path endpoint metrics are updated to reflect what the origin service returned (502, 500, 200)"
        repose.jmx.getMBeanCountAttributeWithWaitForNonZero(rootEndpoint2xxMetric) == rootPath2xxTarget + 1
        repose.jmx.getMBeanCountAttribute(rootEndpoint4xxMetric) == rootPath4xxTarget
        repose.jmx.getMBeanCountAttributeWithWaitForNonZero(rootEndpoint5xxMetric) == rootPath5xxTarget + 2

        and: "the Repose response metrics are updated to reflect what it returned (502, 410, 200)"
        repose.jmx.getMBeanCountAttributeWithWaitForNonZero(repose2xxMetric) == repose2xxTarget + 1
        repose.jmx.getMBeanCountAttributeWithWaitForNonZero(repose4xxMetric) == repose4xxTarget + 1
        repose.jmx.getMBeanCountAttributeWithWaitForNonZero(repose5xxMetric) == repose5xxTarget + 1

        and: "the ALL response metrics are updated to reflect what the origin service returned (502, 500, 200)"
        repose.jmx.getMBeanCountAttributeWithWaitForNonZero(allEndpoints2xxMetric) == all2xxTarget + 1
        repose.jmx.getMBeanCountAttribute(allEndpoints4xxMetric) == all4xxTarget
        repose.jmx.getMBeanCountAttributeWithWaitForNonZero(allEndpoints5xxMetric) == all5xxTarget + 2
    }

    def "the ALL endpoint metrics should include response code data for all of the configured endpoints"() {
        given: "we know the initial values"
        def rootPath2xxTarget = repose.jmx.getMBeanCountAttribute(rootEndpoint2xxMetric)
        def rootTwoPath2xxTarget = repose.jmx.getMBeanCountAttribute(rootTwoEndpoint2xxMetric)
        def repose2xxTarget = repose.jmx.getMBeanCountAttribute(repose2xxMetric)
        def all2xxTarget = repose.jmx.getMBeanCountAttribute(allEndpoints2xxMetric)
        def rootPath5xxTarget = repose.jmx.getMBeanCountAttribute(rootEndpoint5xxMetric)
        def repose5xxTarget = repose.jmx.getMBeanCountAttribute(repose5xxMetric)
        def all5xxTarget = repose.jmx.getMBeanCountAttribute(allEndpoints5xxMetric)

        when: "requests are made that should go to different endpoints"
        def responses = [
                deproxy.makeRequest(url: reposeEndpoint + "/endpoint"),  // /root_path
                deproxy.makeRequest(url: reposeEndpoint + "/secondary"), // /root_path2
                deproxy.makeRequest(url: reposeEndpoint + "/cluster")]   // /root_path

        then: "the client received a good response code for every request"
        responses.every { it.receivedResponse.code as Integer == SC_OK }

        and: "the 2xx metric for the default endpoint is only incremented by 2"
        repose.jmx.getMBeanCountAttributeWithWaitForNonZero(rootEndpoint2xxMetric) == rootPath2xxTarget + 2

        and: "the 2xx metric for the secondary endpoint is incremented by 1"
        repose.jmx.getMBeanCountAttributeWithWaitForNonZero(rootTwoEndpoint2xxMetric) == rootTwoPath2xxTarget + 1

        and: "the 2xx metrics for the Repose and the ALL Endpoints are incremented by 3"
        repose.jmx.getMBeanCountAttributeWithWaitForNonZero(repose2xxMetric) == repose2xxTarget + 3
        repose.jmx.getMBeanCountAttributeWithWaitForNonZero(allEndpoints2xxMetric) == all2xxTarget + 3

        and: "the 5xx metrics are not incremented"
        repose.jmx.getMBeanCountAttribute(rootEndpoint5xxMetric) == rootPath5xxTarget
        repose.jmx.getMBeanCountAttribute(repose5xxMetric) == repose5xxTarget
        repose.jmx.getMBeanCountAttribute(allEndpoints5xxMetric) == all5xxTarget
    }
}
