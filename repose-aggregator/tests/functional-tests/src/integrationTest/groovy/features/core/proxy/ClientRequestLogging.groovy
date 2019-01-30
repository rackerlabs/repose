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
package features.core.proxy

import org.junit.experimental.categories.Category
import org.openrepose.framework.test.ReposeValveTest
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain
import scaffold.category.Bug

/**
 * User: dimi5963
 * Date: 10/8/13
 * Time: 4:45 PM
 *
 * D-15731
 *
 * Client-request-logging attribute does not filter out logging information.
 * It is assigned to add request/response logging into log4j logs.
 * However, it maps to jetty properties.
 * Since we are not using Jetty but HttpClient, all requests are logged regardless of Client-request-logging attribute status.
 * Current workaround is to set the following properties in log4j2.xml to anything but DEBUG
 *
 *    <Logger name="org.apache.commons.httpclient" level="warn"/>
 *    <Logger name="org.apache.http.wire"          level="warn"/>
 *    <Logger name="org.apache.http.headers"       level="warn"/>
 *
 * Fix would map client-request-logging attribute to HttpClient
 */
@Category(Bug)
class ClientRequestLogging extends ReposeValveTest {

    def setupSpec() {
        cleanLogDirectory()
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort)
        params = properties.getDefaultTemplateParams()
    }

    static def params

    def cleanup() {
        if (repose) {
            repose.stop()
        }
    }

    def "test with client request logging true"() {

        given: "Repose configs are updated"
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/core/proxy/clientRequestLoggingTrue", params)
        repose.start()
        List<String> wire_logs = reposeLogSearch.searchByString("org.apache.http.wire")
        List<String> headers_logs = reposeLogSearch.searchByString("org.apache.http.headers")

        when:
        MessageChain messageChain = deproxy.makeRequest([url: reposeEndpoint, method: "GET"])
        List<String> after_wire_logs = reposeLogSearch.searchByString("org.apache.http.wire")
        List<String> after_headers_logs = reposeLogSearch.searchByString("org.apache.http.headers")

        then:
        after_wire_logs.size() - wire_logs.size() > 0
        after_headers_logs.size() - headers_logs.size() > 0
    }

    def "test with client request logging false"() {

        given: "Repose configs are updated"
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/core/proxy/clientRequestLoggingFalse", params)
        repose.start()
        List<String> wire_logs = reposeLogSearch.searchByString("org.apache.http.wire")
        List<String> headers_logs = reposeLogSearch.searchByString("org.apache.http.headers")

        when:
        MessageChain messageChain = deproxy.makeRequest([url: reposeEndpoint, method: "GET"])
        List<String> after_wire_logs = reposeLogSearch.searchByString("org.apache.http.wire")
        List<String> after_headers_logs = reposeLogSearch.searchByString("org.apache.http.headers")

        then:
        after_wire_logs.size() - wire_logs.size() == 0
        after_headers_logs.size() - headers_logs.size() == 0
    }

    def "test with client request logging missing"() {

        given: "Repose configs are updated"
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/core/proxy/clientRequestLoggingDNE", params)
        repose.start()
        List<String> wire_logs = reposeLogSearch.searchByString("org.apache.http.wire")
        List<String> headers_logs = reposeLogSearch.searchByString("org.apache.http.headers")

        when:
        MessageChain messageChain = deproxy.makeRequest([url: reposeEndpoint, method: "GET"])
        List<String> after_wire_logs = reposeLogSearch.searchByString("org.apache.http.wire")
        List<String> after_headers_logs = reposeLogSearch.searchByString("org.apache.http.headers")

        then:
        after_wire_logs.size() - wire_logs.size() == 0
        after_headers_logs.size() - headers_logs.size() == 0
    }
}
