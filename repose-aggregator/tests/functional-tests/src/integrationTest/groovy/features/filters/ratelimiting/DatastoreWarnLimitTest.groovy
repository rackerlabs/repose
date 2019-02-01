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
package features.filters.ratelimiting

import org.junit.experimental.categories.Category
import org.openrepose.framework.test.ReposeValveTest
import org.rackspace.deproxy.Deproxy
import scaffold.category.Filters
import spock.lang.Unroll

/* Checks to see if DatastoreWarnLimit throws warn in log if hit that limit of cache keys */

@Category(Filters)
class DatastoreWarnLimitTest extends ReposeValveTest {
    static int WARN_LIMIT = 1

    def setupSpec() {
        def params = properties.getDefaultTemplateParams()
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/filters/ratelimiting/datastore", params)
        repose.start()
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort)

    }

    @Unroll
    def "when sending requests that match capture group with different cache keys should warn when exceeds #totalRequests"() {

        given:
        def user = UUID.randomUUID().toString();

        when:
        for (int i = 0; i < totalRequests; i++) {
            def path = UUID.randomUUID().toString();
            deproxy.makeRequest(url: reposeEndpoint + "/" + path, method: 'GET', headers: ['X-PP-USER': user, 'X-PP-Groups': "BETA_Group"])
        }

        then:

        def List<String> logMatches = reposeLogSearch.searchByString("Large amount of limits recorded.  Repose Rate Limited may be misconfigured, keeping track of rate limits for user: " + user + ". Please review capture groups in your rate limit configuration.  If using clustered datastore, you may experience network latency.");
        logMatches.size() == expectedWarnings

        where:

        totalRequests  | expectedWarnings
        WARN_LIMIT + 1 | 2
        WARN_LIMIT + 2 | 3

    }


}
