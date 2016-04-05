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
import spock.lang.Unroll

/**
 * Created by jennyvo on 4/1/16.
 */
class ScriptingLanguageTest extends ReposeValveTest {

    static def params

    def setupSpec() {
        deproxy = new Deproxy()
        reposeLogSearch.cleanLog()
        deproxy.addEndpoint(properties.targetPort)

        params = properties.getDefaultTemplateParams()
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/filters/scripting", params)
    }

    def cleanupSpec() {
        if (deproxy) {
            deproxy.shutdown()
        }

        if (repose) {
            repose.stop()
        }
    }

    @Unroll("Test with support language: #language")
    def "Test with all support languages scripting" (){
        repose.configurationProvider.applyConfigs("features/filters/scripting/"+language, params)
        repose.start([waitOnJmxAfterStarting: false])
        sleep(5000)
        waitUntilReadyToServiceRequests("200", true, true)
        //repose.start()
        //repose.waitForNon500FromUrl(reposeEndpoint)

        when:"send request"
        MessageChain mc = deproxy.makeRequest(
                [
                        method        : 'GET',
                        url           : reposeEndpoint,
                        defaultHandler: {
                            new Response(200, null, null, "This should be the body")
                        }
                ])

        then: "repse response"
        mc.receivedResponse.code == '200'
        mc.handlings[0].request.headers.contains("language")
        mc.handlings[0].request.headers.getFirstValue("language") == language
        mc.receivedResponse.headers.contains("ya")
        mc.receivedResponse.headers.getFirstValue("ya") == "hoo"

        where:
        language    << ["python", "jruby", "javascript", "jython", "scala", "lua"]
    }

}
