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

import org.junit.experimental.categories.Category
import org.openrepose.framework.test.ReposeValveTest
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain
import scaffold.category.Filters
import spock.lang.Unroll

@Category(Filters)
class UriNormalizationMediaTypeTest extends ReposeValveTest {
    def setupSpec() {
        def params = properties.defaultTemplateParams

        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort)

        repose.configurationProvider.applyConfigs('common', params)
        repose.configurationProvider.applyConfigs('features/filters/uriNormalization', params)
        repose.configurationProvider.applyConfigs('features/filters/uriNormalization/onlymediavariant', params)

        repose.start()
    }

    @Unroll
    def "a #method request is processed appropriately"() {
        when:
        MessageChain messageChain = deproxy.makeRequest(
            method: method,
            url: reposeEndpoint,
            path: "/path.json"
        )

        then:
        messageChain.handlings.size() == 1
        messageChain.handlings[0].request.path == "/path"
        messageChain.handlings[0].request.headers.getFirstValue("Accept") == "application/json"

        where:
        method << ["GET", "POST", "PUT", "DELETE"]
    }

    // the accept header partially matches the preferred media-type, but the extension does not
    // the accept header fully matches the preferred media type, but the extension does not
    // there is no mapped extension and the accept header does not match the wildcard (i.e., */*)
    // the extension is all numbers (e.g., ".123") but does match a configured media variant
    @Unroll
    def "no configured media variant matches on a request for #path with accept set to #accept"() {
        when:
        MessageChain messageChain = deproxy.makeRequest(
            method: "GET",
            url: reposeEndpoint,
            path: path,
            headers: ["Accept": accept]
        )

        then:
        messageChain.handlings.size() == 1
        messageChain.handlings[0].request.path == path
        messageChain.handlings[0].request.headers.getFirstValue("Accept") == accept

        where:
        path        | accept
        "/path"     | "application/*"
        "/path"     | "application/xml"
        "/path"     | "application/json"
        "/path.foo" | "application/*"
        "/path.foo" | "application/xml"
        "/path.foo" | "application/json"
        "/path.123" | "application/*"
        "/path.123" | "application/xml"
        "/path.123" | "application/json"
    }

    // the media extension matches a configured media extension
    // the media extension matches a configuerd media extension even if the accept header does not
    // the media extension in the path is not at the end of the path
    @Unroll
    def "matching media variant on a #method request for #path with accept set to #accept"() {
        when:
        MessageChain messageChain = deproxy.makeRequest(
            method: "GET",
            url: reposeEndpoint,
            path: path,
            headers: accept ? ["Accept": accept] : [:]
        )

        then:
        messageChain.handlings.size() == 1
        messageChain.handlings[0].request.path == expectedPath
        messageChain.handlings[0].request.headers.getFirstValue("Accept") == expectedAccept

        where:
        path            | accept             || expectedPath | expectedAccept
        "/path.json"    | null               || "/path"      | "application/json"
        "/path.json"    | "application/json" || "/path"      | "application/json"
        "/path.json"    | "application/xml"  || "/path"      | "application/json"
        "/path.json/to" | null               || "/path/to"   | "application/json"
        "/path.json/to" | "application/json" || "/path/to"   | "application/json"
        "/path.json/to" | "application/xml"  || "/path/to"   | "application/json"
        "/path.xml"     | null               || "/path"      | "application/xml"
        "/path.xml"     | "application/json" || "/path"      | "application/xml"
        "/path.xml"     | "application/xml"  || "/path"      | "application/xml"
        "/path.xml/to"  | null               || "/path/to"   | "application/xml"
        "/path.xml/to"  | "application/json" || "/path/to"   | "application/xml"
        "/path.xml/to"  | "application/xml"  || "/path/to"   | "application/xml"
    }

    // no media extension
    // no media extension with the wildcard accept
    // a media extension that starts with a number
    // a media extension matching the preferred media variant
    @Unroll
    def "preferred media variant on a request with media extension #mediaExtension with accept set to #accept"() {
        when:
        MessageChain messageChain = deproxy.makeRequest(
            method: "GET",
            url: reposeEndpoint,
            path: "/path$mediaExtension",
            headers: accept ? ["Accept": accept] : [:]
        )

        then:
        messageChain.handlings.size() == 1
        messageChain.handlings[0].request.path == expectedPath
        messageChain.handlings[0].request.headers.getFirstValue("Accept") == "application/json"

        where:
        mediaExtension | accept || expectedPath
        ""             | null   || "/path"
        ""             | "*/*"  || "/path"
        ".123"         | null   || "/path.123"
        ".123"         | "*/*"  || "/path.123"
        ".json"        | null   || "/path"
        ".json"        | "*/*"  || "/path"
    }
}
