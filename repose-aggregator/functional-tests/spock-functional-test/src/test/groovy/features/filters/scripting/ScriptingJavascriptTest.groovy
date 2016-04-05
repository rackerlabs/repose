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
package features.filters.scripting

import framework.ReposeValveTest
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain
import org.rackspace.deproxy.Response

/**
 * Created by jennyvo on 4/4/16.
 */
class ScriptingJavascriptTest extends ReposeValveTest {

    def setupSpec() {
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort)

        def params = properties.getDefaultTemplateParams()
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/filters/scripting", params)
        repose.configurationProvider.applyConfigs("features/filters/scripting/javascript", params)
        repose.start([waitOnJmxAfterStarting: false])
        waitUntilReadyToServiceRequests("200", true, true)
    }

    def cleanupSpec() {
        if (deproxy) {
            deproxy.shutdown()
        }

        if (repose) {
            repose.stop()
        }
    }

    def "Test with javascript scripting" (){
        def headers = ["test": "repose", "foo":"bar"]
        when:"send request"
        MessageChain mc = deproxy.makeRequest(
                [
                        method        : 'GET',
                        url           : reposeEndpoint,
                        headers       : headers,
                        defaultHandler: {
                            new Response(200, null, headers, "This should be the body")
                        }
                ])

        then: "repse response"
        mc.receivedResponse.code == '200'
        mc.handlings[0].request.headers.contains("language")
        mc.handlings[0].request.headers.getFirstValue("language") == "javascript"
        mc.receivedResponse.headers.contains("ya")
        mc.receivedResponse.headers.getFirstValue("ya") == "hoo"
    }

}
