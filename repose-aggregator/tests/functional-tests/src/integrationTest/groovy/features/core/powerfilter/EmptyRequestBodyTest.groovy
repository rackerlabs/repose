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
import org.rackspace.deproxy.MessageChain
import scaffold.category.Core
import spock.lang.Unroll

@Category(Core)
class EmptyRequestBodyTest extends ReposeValveTest {

    def setupSpec() {
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort)

        def params = properties.getDefaultTemplateParams()
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/core/proxy", params)
        repose.start()
    }

    @Unroll("Deproxy should not remove the body of a HTTP requests #method")
    def "Deproxy should not remove the body of a HTTP request"() {
        given:
        String requestBody = "request body"
        String deproxyEndpoint = "http://localhost:${properties.targetPort}"

        when:
        MessageChain mc = deproxy.makeRequest(url: deproxyEndpoint, requestBody: requestBody)

        then:
        mc.getSentRequest().getBody().toString() == requestBody
        mc.getHandlings().get(0).getRequest().getBody().toString() == requestBody

        where:
        method << ["GET", "HEAD", "PUT", "POST", "PATCH", "DELETE"]
    }

    // NOTE: The following test has proven contentious. In accordance with RFC 2616 Section 5, all HTTP requests may
    // contain a request body. However, in the words of Roy Fielding:
    // "Server semantics for GET, however, are restricted such that a body, if any, has no semantic meaning to the
    // request. The requirements on parsing are separate from the requirements on method semantics."
    // It has been decided that Repose will remove the request body for certain request types. No one should care.
    // Anyone who does care has either written an invalid HTTP service or is a disputatious individual.
    @Unroll("#method should have its body removed")
    def "Repose should remove request bodies"() {
        given:
        String requestBody = "request body"
        def headers = [
                "Content-Type": "plain/text"
        ]

        when:
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint, method: method, headers: headers, requestBody: requestBody)
        println(reposeEndpoint)
        then:
        mc.getSentRequest().getBody().toString() == requestBody
        mc.getHandlings().get(0).getRequest().getBody().toString() == ""

        where:
        method << ["GET", "HEAD"]
    }

    @Unroll("#method should not have its body removed")
    def "Repose should not remove request bodies unless filters do so explicitly"() {
        given:
        String requestBody = "request body"

        when:
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint, method: method, requestBody: requestBody)

        then:
        mc.getSentRequest().getBody().toString() == requestBody
        mc.getHandlings().get(0).getRequest().getBody().toString() == requestBody

        where:
        method << ["PUT", "POST", "PATCH", "DELETE"]
    }
}
