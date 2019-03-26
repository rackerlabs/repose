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
package features.filters.urinormalization

import org.junit.experimental.categories.Category;
import org.openrepose.framework.test.ReposeValveTest
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.Handling
import org.rackspace.deproxy.MessageChain
import scaffold.category.Filters
import spock.lang.Unroll

@Category(Filters)
class UriNormalizationWithMediaTest extends ReposeValveTest {

    static Map params

    def setupSpec() {
        params = properties.getDefaultTemplateParams()
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/filters/uriNormalization", params)
        repose.configurationProvider.applyConfigs("features/filters/uriNormalization/withmedia", params)
        repose.start()
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort)
        repose.waitForNon500FromUrl(reposeEndpoint)
    }

    @Unroll("URI Normalization of queryParameters #behaviorExpected")
    def "When http method doesn't match the uri filter"() {
        given:
        def path = "/" + matchingUriRegex + "/?" + qpBeforeRepose

        when: "A request is made to REPOSE"
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint + path, method: method)

        then: "Request is forwarded to origin service"
        mc.handlings.size() == 1
        Handling handling = mc.handlings.get(0)

        then: "Request sent to origin service matches expected query parameter list"
        handling.request.path.endsWith(qpAfterRepose)

        where:
        method | matchingUriRegex               | qpBeforeRepose                                             | qpAfterRepose                                              | behaviorExpected
        "POST" | "uri_normalization_with_media" | "filter_me=true&a=1&a=4&a=2&r=1241.212&n=test&a=Add+Space" | "filter_me=true&a=1&a=4&a=2&r=1241.212&n=test&a=Add+Space" | "Should not filter or alphabetize any query parameters"
        "GET"  | "uri_normalization_with_media" | "a=3&b=4&a=4&A=0&c=6&d=7"                                  | "A=0&a=3&a=4&b=4&c=6"                                      | "Should allow whitelisted query parameters"
        "GET"  | "uri_normalization_with_media" | "a=3&b=4&a=4&A=0&c=6&d=7&B=8&b=9"                          | "A=0&B=8&a=3&a=4&b=4&c=6"                                  | "Should allow whitelisted query parameters up to multiplicity coun"
        "GET"  | "uri_normalization_with_media" | "a=3&b=4&a=4&A=0&c=6&C=8&c=10&C=9&c=11"                    | "A=0&a=3&a=4&b=4&c=6&c=10"                                 | "Should allow whitelisted case sensitive query parameters up to multiplicity count"
    }

}
