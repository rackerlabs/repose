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

import groovy.json.JsonSlurper
import org.openrepose.framework.test.ReposeLogSearch
import org.openrepose.framework.test.ReposeValveTest
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain
import org.rackspace.deproxy.Response
import org.w3c.dom.Document
import org.xml.sax.InputSource
import spock.lang.Unroll

import javax.xml.parsers.DocumentBuilder
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.xpath.XPathConstants
import javax.xml.xpath.XPathFactory

import static javax.servlet.http.HttpServletResponse.*

/**
 * Copied most of this from the RateLimitingTest
 */
class AbsoluteRateLimitTest extends ReposeValveTest {
    final Map<String, String> userHeaderDefault = ["X-PP-User": "user"]
    final Map<String, String> groupHeaderDefault = ["X-PP-Groups": "customer"]
    final Map<String, String> acceptHeaderJson = ["Accept": "application/json"]

    final def xmlAbsoluteLimitResponse = {
        return new Response(SC_OK,
                "OK", ["Content-Type": "application/xml"],
                "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>" +
                        "<limits xmlns=\"http://docs.openstack.org/common/api/v1.0\"><absolute>" +
                        "<limit name=\"Admin\" value=\"15\"/><limit name=\"Tech\" value=\"10\"/>" +
                        "<limit name=\"Demo\" value=\"5\"/></absolute></limits>")
    }

    final def jsonAbsoluteLimitResponse = {
        return new Response(SC_OK,
                "OK", ["Content-Type": "application/json"],
                """{
    "limits": {
        "rate": [],
        "absolute": {
            "maxServerMeta": 40,
            "maxPersonality": 5,
            "totalPrivateNetworksUsed": 0,
            "maxImageMeta": 40,
            "maxPersonalitySize": 1000,
            "maxSecurityGroupRules": -1,
            "maxTotalKeypairs": 100,
            "totalCoresUsed": 0,
            "totalRAMUsed": 0,
            "totalInstancesUsed": 0,
            "maxSecurityGroups": -1,
            "totalFloatingIpsUsed": 0,
            "maxTotalCores": -1,
            "totalSecurityGroupsUsed": 0,
            "maxTotalPrivateNetworks": 3,
            "maxTotalFloatingIps": -1,
            "maxTotalInstances": 100,
            "maxTotalRAMSize": 131072
        }
    }
}""")
    }

    final def watResponse = {
        return new Response(SC_OK, "OK", ["Content-Type": "application/wat"], "LOL")
    }

    def setupSpec() {
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort)

        def params = properties.getDefaultTemplateParams()
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/filters/ratelimiting/onenodes", params)
        repose.start()
        repose.waitForNon500FromUrl(reposeEndpoint)

        reposeLogSearch = new ReposeLogSearch(properties.getLogFile())
    }

    def cleanup() {
        waitForLimitReset()
        reposeLogSearch.cleanLog()
    }

    @Unroll("XML UPSTREAM: when requesting rate limits for unlimited groups with #acceptHeader")
    def "When requesting rate limits for unlimited groups, should receive rate limits in request format"() {
        when:
        MessageChain messageChain = deproxy.makeRequest(url: reposeEndpoint + "/service2/limits", method: "GET",
                headers: ["X-PP-Groups": "unlimited;q=1.0", "X-PP-User": "unlimited-user"] + acceptHeader,
                defaultHandler: xmlAbsoluteLimitResponse
        )

        then:
        messageChain.receivedResponse.code as Integer == SC_OK
        messageChain.receivedResponse.headers.findAll("Content-Type").contains(expectedFormat)

        def body = messageChain.receivedResponse.body as String

        body.length() > 0
        if (expectedFormat.contains("xml")) {
            assert parseRateCountFromXML(body) == 0
            assert parseAbsoluteFromXML(body, 0) == 15
            assert parseAbsoluteFromXML(body, 1) == 10
            assert parseAbsoluteFromXML(body, 2) == 5
        } else {
            assert parseRateCountFromJSON(body) == 0
            assert parseAbsoluteFromJSON(body, "Admin") == 15
            assert parseAbsoluteFromJSON(body, "Tech") == 10
            assert parseAbsoluteFromJSON(body, "Demo") == 5
        }

        where:
        acceptHeader                   | expectedFormat
        ["Accept": "application/xml"]  | "application/xml"
        ["Accept": "application/json"] | "application/json"
        []                             | "application/json"
        ["Accept": "*/*"]              | "application/json"
    }

    @Unroll("502 error if upstream responds with a media type we don't support with #acceptHeader")
    def "502 error if upstream responds with a media type we don't support"() {
        when:
        MessageChain messageChain = deproxy.makeRequest(url: reposeEndpoint + "/service2/limits", method: "GET",
                headers: ["X-PP-Groups": "unlimited;q=1.0", "X-PP-User": "unlimited-user"] + acceptHeader,
                defaultHandler: watResponse
        )

        then:
        messageChain.receivedResponse.code as Integer == SC_BAD_GATEWAY
        //With the application/wat it's just an array of characters, so
        messageChain.receivedResponse.body as ArrayList<Integer> == [76, 79, 76]  //LOL

        where:
        acceptHeader                   | expectedFormat
        ["Accept": "application/xml"]  | "application/xml"
        ["Accept": "application/json"] | "application/json"
        []                             | "application/json"
        ["Accept": "*/*"]              | "application/json"
    }

    @Unroll("JSON UPSTREAM: when requesting rate limits for unlimited groups with #acceptHeader")
    def "When requesting rate limits for unlimited groups, should receive rate limits in JSON when upstream responds with JSON"() {
        when:
        MessageChain messageChain = deproxy.makeRequest(url: reposeEndpoint + "/service2/limits", method: "GET",
                headers: ["X-PP-Groups": "unlimited;q=1.0", "X-PP-User": "unlimited-user"] + acceptHeader,
                defaultHandler: jsonAbsoluteLimitResponse
        )

        then:
        messageChain.receivedResponse.code as Integer == SC_OK
        //Content-type will always be JSON when
        messageChain.receivedResponse.headers.findAll("Content-Type").contains(expectedFormat)

        def body = messageChain.receivedResponse.body as String

        body.length() > 0

        if (expectedFormat.contains("xml")) {
            assert parseRateCountFromXML(body) == 0

            //The JSON upstream doesn't match the simple XML upstream
            assert parseXpath(body, "//absolute/limit[@name='maxServerMeta']/@value") == 40
            assert parseXpath(body, "//absolute/limit[@name='totalPrivateNetworksUsed']/@value") == 0
            assert parseXpath(body, "//absolute/limit[@name='maxSecurityGroups']/@value") == -1
        } else {
            assert parseRateCountFromJSON(body) == 0
            assert parseAbsoluteFromJSON(body, "maxServerMeta") == 40
            assert parseAbsoluteFromJSON(body, "totalPrivateNetworksUsed") == 0
            assert parseAbsoluteFromJSON(body, "maxSecurityGroups") == -1
        }

        where:
        acceptHeader                   | expectedFormat
        ["Accept": "application/xml"]  | "application/xml"
        ["Accept": "application/json"] | "application/json"
        []                             | "application/json"
        ["Accept": "*/*"]              | "application/json"
    }


    @Unroll("XML UPSTREAM: When requesting rate limits for limited groups with #acceptHeader")
    def "When requesting rate limits for limited groups, should receive rate limits in request format"() {
        when:
        MessageChain messageChain = deproxy.makeRequest(url: reposeEndpoint + path, method: "GET",
                headers: ["X-PP-Groups": "customer;q=1.0", "X-PP-User": "user"] + acceptHeader,
                defaultHandler: xmlAbsoluteLimitResponse
        )

        then:
        messageChain.receivedResponse.code as Integer == SC_OK
        messageChain.receivedResponse.headers.findAll("Content-Type").contains(expectedFormat)

        def body = messageChain.receivedResponse.body as String

        body.length() > 0
        if (expectedFormat.contains("xml")) {
            assert parseRateCountFromXML(body) > 0
            assert parseAbsoluteFromXML(body, 0, true) == 15
            assert parseAbsoluteFromXML(body, 1, true) == 10
            assert parseAbsoluteFromXML(body, 2, true) == 5
        } else {
            assert parseRateCountFromJSON(body) > 0
            assert parseAbsoluteFromJSON(body, "Admin") == 15
            assert parseAbsoluteFromJSON(body, "Tech") == 10
            assert parseAbsoluteFromJSON(body, "Demo") == 5
        }

        where:
        path               | acceptHeader                   | expectedFormat
        "/service2/limits" | ["Accept": "application/xml"]  | "application/xml"
        "/service2/limits" | ["Accept": "application/json"] | "application/json"
        "/service2/limits" | []                             | "application/json"
        "/service2/limits" | ["Accept": "*/*"]              | "application/json"
    }

    @Unroll("JSON UPSTREAM: when requesting rate limits for limited groups with #acceptHeader")
    def "When requesting rate limits for limited groups, should receive rate limits in JSON when upstream responds with JSON"() {
        when:
        MessageChain messageChain = deproxy.makeRequest(url: reposeEndpoint + "/service2/limits", method: "GET",
                headers: ["X-PP-Groups": "customer;q=1.0", "X-PP-User": "user"] + acceptHeader,
                defaultHandler: jsonAbsoluteLimitResponse
        )

        then:
        messageChain.receivedResponse.code as Integer == SC_OK
        messageChain.receivedResponse.headers.findAll("Content-Type").contains(expectedFormat)

        def body = messageChain.receivedResponse.body as String

        body.length() > 0

        if (expectedFormat.contains("xml")) {
            //The JSON upstream doesn't match the simple XML upstream
            assert parseXpath(body, "//absolute/limit[@name='maxServerMeta']/@value") == 40
            assert parseXpath(body, "//absolute/limit[@name='totalPrivateNetworksUsed']/@value") == 0
            assert parseXpath(body, "//absolute/limit[@name='maxSecurityGroups']/@value") == -1

            assert parseRateCountFromXML(body) > 0
        } else {
            assert parseRateCountFromJSON(body) > 0
            assert parseAbsoluteFromJSON(body, "maxServerMeta") == 40
            assert parseAbsoluteFromJSON(body, "totalPrivateNetworksUsed") == 0
            assert parseAbsoluteFromJSON(body, "maxSecurityGroups") == -1
        }

        where:
        acceptHeader                   | expectedFormat
        ["Accept": "application/xml"]  | "application/xml"
        ["Accept": "application/json"] | "application/json"
        []                             | "application/json"
        ["Accept": "*/*"]              | "application/json"
    }

    def "When requesting rate limits with an invalid Accept header, Should receive 406 response when invalid Accept header"() {
        given: "an invalid Accept value will be sent"
        def headers = [
                "X-PP-Groups": "customer;q=1.0",
                "X-PP-User": "user",
                "Accept": "application/unknown"]

        when:
        MessageChain messageChain = deproxy.makeRequest(
                url: reposeEndpoint + "/service2/limits",
                method: "GET",
                headers: headers)

        then: "a 406 is returned"
        messageChain.receivedResponse.code as Integer == SC_NOT_ACCEPTABLE

        and: "the origin service does not receive the request"
        messageChain.handlings.isEmpty()
    }

    @Unroll("XML UPSTREAM: When requesting rate limits for group with special characters with #acceptHeader")
    def "When requesting rate limits as json for group with special characters and upstream responds with XML"() {
        when:
        MessageChain messageChain = deproxy.makeRequest(
                url: reposeEndpoint + path,
                method: "GET",
                headers: ["X-PP-Groups": "unique;q=1.0", "X-PP-User": "user"] + acceptHeader,
                defaultHandler: xmlAbsoluteLimitResponse)

        then:
        messageChain.receivedResponse.code as Integer == SC_OK
        messageChain.receivedResponse.headers.findAll("Content-Type").contains(expectedFormat)

        def body = messageChain.receivedResponse.body as String

        body.length() > 0
        parseRateCountFromJSON(body) > 0
        parseAbsoluteFromJSON(body, "Admin") == 15
        parseAbsoluteFromJSON(body, "Tech") == 10
        parseAbsoluteFromJSON(body, "Demo") == 5
        body.contains("service/\\\\w*")
        body.contains("service/\\\\s*")
        body.contains("service/(\\\".+\\")
        body.contains("service/\\\\d*")

        where:
        path               | acceptHeader                   | expectedFormat
        "/service2/limits" | ["Accept": "application/json"] | "application/json"
        "/service2/limits" | []                             | "application/json"
        "/service2/limits" | ["Accept": "*/*"]              | "application/json"
    }

    @Unroll("JSON UPSTREAM: When requesting rate limits for group with special characters with #acceptHeader")
    def "When requesting rate limits as json for group with special characters and upstream responds with JSON"() {
        when:
        MessageChain messageChain = deproxy.makeRequest(url: reposeEndpoint + path, method: "GET",
                headers: ["X-PP-Groups": "unique;q=1.0", "X-PP-User": "user"] + acceptHeader,
                defaultHandler: jsonAbsoluteLimitResponse
        )

        then:
        messageChain.receivedResponse.code as Integer == SC_OK
        messageChain.receivedResponse.headers.findAll("Content-Type").contains(expectedFormat)

        def body = messageChain.receivedResponse.body as String

        body.length() > 0

        //This is mutable, and depends on prior test state...
        parseRateCountFromJSON(body) > 0
        parseAbsoluteFromJSON(body, "maxServerMeta") == 40
        parseAbsoluteFromJSON(body, "totalPrivateNetworksUsed") == 0
        parseAbsoluteFromJSON(body, "maxSecurityGroups") == -1

        body.contains("service/\\\\w*")
        body.contains("service/\\\\s*")
        body.contains("service/(\\\".+\\")
        body.contains("service/\\\\d*")

        where:
        path               | acceptHeader                   | expectedFormat
        "/service2/limits" | ["Accept": "application/json"] | "application/json"
        "/service2/limits" | []                             | "application/json"
        "/service2/limits" | ["Accept": "*/*"]              | "application/json"
    }

    // Helper methods

    /**
     * Must specify the full path to the actual value you want
     * //absolute/limit[@name='lol']/@value
     * @param xml
     * @param xpathString
     * @return
     */
    private static int parseXpath(String xml, String xpathString) {
        println("body is:\n$xml")

        def builder = DocumentBuilderFactory.newInstance().newDocumentBuilder()
        def inputStream = new ByteArrayInputStream(xml.bytes)
        def records = builder.parse(inputStream).documentElement
        def xpath = XPathFactory.newInstance().newXPath()

        xpath.evaluate(xpathString, records, XPathConstants.NUMBER)
    }

    // TODO: So much of this is copy pasta from the other rate limiting test
    private static int parseAbsoluteFromXML(String s, int limit) {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance()
        factory.setNamespaceAware(true)
        DocumentBuilder documentBuilder = factory.newDocumentBuilder()
        Document document = documentBuilder.parse(new InputSource(new StringReader(s)))

        document.getDocumentElement().normalize()

        return Integer.parseInt(document.getElementsByTagName("limit").item(limit).getAttributes().getNamedItem("value").getNodeValue())
    }

    private static int parseAbsoluteFromXML(String s, int limit, boolean useSlurper) {
        if (!useSlurper)
            return parseAbsoluteFromXML(s, limit)
        else {
            def xml = XmlSlurper.newInstance().parseText(s)
            return Integer.parseInt(xml.children()[1][0].children()[limit].attributes()["value"])
        }
    }

    private static int parseAbsoluteFromJSON(String body, String limit) {
        def json = JsonSlurper.newInstance().parseText(body)
        return json.limits.absolute[limit].value
    }

    private static int parseAbsoluteLimitFromJSON(String body, int limit) {
        def json = JsonSlurper.newInstance().parseText(body)
        return json.limits.rate[limit].limit[0].value
    }

    //using this for now
    private static int parseRemainingFromJSON(String body, int limit) {
        def json = JsonSlurper.newInstance().parseText(body)
        return json.limits.rate[limit].limit[0].remaining
    }

    private static int parseRateCountFromXML(String body) {
        def limits = XmlSlurper.newInstance().parseText(body)
        return limits.rates.rate.toList().size()
    }


    private static int parseRateCountFromJSON(String body) {
        def json = JsonSlurper.newInstance().parseText(body)
        return json.limits.rate.size()
    }

    private String getDefaultLimits(Map group = null) {
        def groupHeader = (group != null) ? group : groupHeaderDefault
        MessageChain messageChain = deproxy.makeRequest(url: reposeEndpoint + "/service2/limits", method: "GET",
                headers: userHeaderDefault + groupHeader + acceptHeaderJson)

        return messageChain.receivedResponse.body
    }

    private void waitForLimitReset(Map group = null) {
        while (parseRemainingFromJSON(getDefaultLimits(group), 0) != parseAbsoluteLimitFromJSON(getDefaultLimits(group), 0)) {
            sleep(1000)
        }
    }
}
