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
package features.filters.slf4jlogging

import org.junit.experimental.categories.Category
import org.openrepose.framework.test.ReposeLogSearch
import org.openrepose.framework.test.ReposeValveTest
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain
import org.rackspace.deproxy.Response
import scaffold.category.Filters
import spock.lang.Unroll

/**
 * Created by jennyvo on 4/8/14.
 */
@Category(Filters)
class Slf4jHttpLoggingTest extends ReposeValveTest {
    def setupSpec() {
        //remove old log
        def logSearch = new ReposeLogSearch(properties.logFile)
        logSearch.cleanLog()

        def params = properties.defaultTemplateParams
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/filters/slf4jhttplogging", params)
        repose.start()
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort)
    }

    @Unroll("Test slf4jlog entry with #method")
    def "Test check slf4log for various methods"() {
        def logSearch = new ReposeLogSearch(properties.logFile)
        logSearch.cleanLog()

        when:
        deproxy.makeRequest(url: reposeEndpoint, method: method)

        then:
        logSearch.searchByString("Remote IP=127.0.0.1 Local IP=127.0.0.1 Request Method=$method").size() == 1
        logSearch.searchByString("Remote User=null\tURL Path Requested=http://localhost:${properties.reposePort}/\tRequest Protocol=HTTP/1.1").size() == 1
        logSearch.searchByString("Time Request Received=\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}").size() == 1 // default date format

        where:
        method << [
                'GET',
                'POST',
                'PUT',
                'PATCH',
                'HEAD',
                'DELETE'
        ]

    }

    @Unroll("Test slf4jlog entry failed tests with #method and response code #responseCode")
    def "Test slf4j log entry for failed tests"() {
        given:
        def xmlResp = { request -> return new Response(responseCode) }
        def logSearch = new ReposeLogSearch(properties.logFile)
        logSearch.cleanLog()
        println properties.reposePort
        println properties.targetPort

        when:
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint, method: method, defaultHandler: xmlResp)

        then:
        logSearch.searchByString("Remote IP=127.0.0.1 Local IP=127.0.0.1 Request Method=$method Response Code=$responseCode").size() == 1
        logSearch.searchByString("my-special-log - Remote User=null\tURL Path Requested=http://localhost:${properties.reposePort}/\tRequest Protocol=HTTP/1.1").size() == 1

        where:
        method   | responseCode
        'GET'    | 404
        'POST'   | 404
        'PUT'    | 404
        'PATCH'  | 404
        'HEAD'   | 404
        'DELETE' | 404

    }
}
