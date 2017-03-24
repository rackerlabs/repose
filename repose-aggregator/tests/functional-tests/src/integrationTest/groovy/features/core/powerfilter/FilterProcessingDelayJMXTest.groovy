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
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.Response

class FilterProcessingDelayJMXTest extends ReposeValveTest {

    String PREFIX = "${jmxHostname}:001=\"org\",002=\"openrepose\",003=\"core\",004=\"FilterProcessingTime\",005=\"Delay\""

    String API_VALIDATOR = PREFIX + ",006=\"api-validator\""
    String IP_IDENTITY = PREFIX + ",006=\"ip-user\""

    def handler = { return new Response(200) }

    def setupSpec() {
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort)

        def params = properties.getDefaultTemplateParams()
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/core/powerfilter/multifilters", params)
        repose.start()

        repose.waitForNon500FromUrl(reposeEndpoint + "/")
    }

    def "when a request is sent through Repose, should record per filter delay metrics"() {
        given:
        def ipIdentityCount = repose.jmx.getMBeanAttribute(IP_IDENTITY, "Count")
        def apiValidatorCount = repose.jmx.getMBeanAttribute(API_VALIDATOR, "Count")

        if (ipIdentityCount == null)
            ipIdentityCount = 0
        if (apiValidatorCount == null)
            apiValidatorCount = 0

        when:
        deproxy.makeRequest(url: reposeEndpoint + "/resource", method: "GET", headers: ["X-Roles": "role-1"],
                defaultHandler: handler)

        then:
        repose.jmx.getMBeanAttribute(IP_IDENTITY, "Count") == ipIdentityCount + 1
        repose.jmx.getMBeanAttribute(API_VALIDATOR, "Count") == apiValidatorCount + 1
        repose.jmx.getMBeanAttribute(IP_IDENTITY, "Mean") > 0
        repose.jmx.getMBeanAttribute(API_VALIDATOR, "Mean") > 0
    }
}
