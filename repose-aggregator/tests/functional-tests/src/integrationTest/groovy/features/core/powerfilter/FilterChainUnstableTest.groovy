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

import java.util.concurrent.TimeoutException

/* Checks to see if having Unstable filter chain on startup due to configuration errors will log errors into the log file */

@Category(Core)
class FilterChainUnstableTest extends ReposeValveTest {
    static int requestCount = 1
    def handler5XX = { request -> return new Response(503, 'SERVICE UNAVAILABLE') }

    def setupSpec() {
        def params = properties.getDefaultTemplateParams()
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/core/powerfilter/badconfigs", params)
        try {
            repose.start()
        } catch (TimeoutException e) {

        }

        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort)

    }

    def "when sending requests on failure to startup repose due to bad configurations"() {

        given:
        def List<String> logMatches = reposeLogSearch.searchByString("Please check your configuration files and your artifacts directory.");
        def existingWarningNumber = logMatches.size()

        when:
        for (int i = 0; i < totalRequests; i++) {
            deproxy.makeRequest([url: reposeEndpoint + "/limits", defaultHandler: handler5XX])
        }

        then:

        def List<String> logMatchesAfterRequests = reposeLogSearch.searchByString("Please check your configuration files and your artifacts directory.");
        logMatchesAfterRequests.size() == (expectedWarnings + existingWarningNumber)

        where:

        totalRequests    | expectedWarnings
        requestCount     | 1
        requestCount + 2 | 3

    }


}

