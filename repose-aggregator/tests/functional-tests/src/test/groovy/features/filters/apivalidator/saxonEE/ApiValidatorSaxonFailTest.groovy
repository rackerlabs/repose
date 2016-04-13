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
package features.filters.apivalidator.saxonEE

import framework.ReposeValveTest
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain
import org.rackspace.deproxy.Response
import spock.lang.Unroll

/*
 * Api validator tests ported over from and JMeter
 */

class ApiValidatorSaxonFailTest extends ReposeValveTest {

    def setupSpec() {
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort)

        def saxonHome = System.getenv("SAXON_HOME")

        //If we're the jenkins user, set it, and see if it works
        if (saxonHome == null && System.getenv("LOGNAME").equals("jenkins")) {
            //For jenkins, it's going to be in $HOME/saxon_ee
            def home = System.getenv("HOME")
            saxonHome = "${home}/saxon_ee"
            repose.addToEnvironment("SAXON_HOME", saxonHome)
        }

        assert saxonHome != null
    }

    def cleanupSpec() {
        if (repose)
            repose.stop()
        if (deproxy)
            deproxy.shutdown()
    }

    def "GET on /path/to/test (XML) should fail without header X-TEST"() {
        setup: "declare messageChain to be of type MessageChain"
        def params = properties.getDefaultTemplateParams()
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/filters/apivalidator/saxonEE", params)
        repose.configurationProvider.applyConfigs("features/filters/apivalidator/saxonEE/xml", params)
        repose.start()
        repose.waitForNon500FromUrl(reposeEndpoint)

        MessageChain messageChain
        def customHandler = { return new Response(200, "OK") }

        def Map<String, String> headers = [
                "Accept"         : "application/xml",
                "Content-Type"   : "application/xml",
                "Host"           : "localhost",
                "User-Agent"     : "gdeproxy"
        ]

        def reqBody = "<body-root xmlns=\"http://test.rackspace.com/body\">" +
                "<body-element>1</body-element>" +
                "<body-element>2</body-element>" +
                "<body-element>3</body-element>" +
                "</body-root>"

        when:
        messageChain = deproxy.makeRequest(url: reposeEndpoint + "/path/to/test",
                method: 'GET', headers: headers,
                requestBody: reqBody, defaultHandler: customHandler,
                addDefaultHeaders: false
        )

        then:
        "result should be " + 403
        messageChain.receivedResponse.code.equals("403")
        messageChain.receivedResponse.headers["Content-Type"].equals("application/xml")
        println messageChain.receivedResponse.body
        messageChain.receivedResponse.body == "<response\n" +
                "    xmlns=\"http://docs.openstack.org/common/api/v1.1\">\n" +
                "  <message>XML Not Authorized... Syntax highlighting is magical.</message>\n" +
                "</response>"

    }

    def "GET on /path/to/test (JSON) should fail without header X-TEST"() {
        setup: "declare messageChain to be of type MessageChain"
        def params = properties.getDefaultTemplateParams()
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/filters/apivalidator/saxonEE", params)
        repose.configurationProvider.applyConfigs("features/filters/apivalidator/saxonEE/json", params)
        repose.start()
        repose.waitForNon500FromUrl(reposeEndpoint)

        MessageChain messageChain
        def customHandler = { return new Response(200, "OK") }

        def Map<String, String> headers = [
                "Accept"         : "application/json",
                "Content-Type"   : "application/json",
                "Host"           : "localhost",
                "User-Agent"     : "gdeproxy"
        ]

        def reqBody = "{\n" +
                "         \"firstName\" : \"Jorge\",\n" +
                "         \"lastName\" : \"Williams\",\n" +
                "         \"age\" : 38\n" +
                "    }"

        when:
        messageChain = deproxy.makeRequest(url: reposeEndpoint + "/path/to/test",
                method: 'GET', headers: headers,
                requestBody: reqBody, defaultHandler: customHandler,
                addDefaultHeaders: false
        )

        then:
        "result should be " + 403
        messageChain.receivedResponse.code.equals("403")
        messageChain.receivedResponse.headers["Content-Type"].equals("application/json")
        messageChain.receivedResponse.body == "{\n" +
                "            \"message\": \"JSON Not Authorized... The brackets are too confusing.\"\n" +
                "            }"

    }
}
