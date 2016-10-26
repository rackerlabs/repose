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
package features.core.languages

import framework.ReposeValveTest
import org.rackspace.deproxy.ApacheClientConnector
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain
import spock.lang.Ignore

import static java.nio.charset.StandardCharsets.UTF_8
/**
 * Created by adrian on 10/19/16.
 */
class BasicSupportTest extends ReposeValveTest {

    def setupSpec() {
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort)

        def params = properties.getDefaultTemplateParams()
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/core/languages/basic", params)
        repose.start()
        repose.waitForNon500FromUrl(properties.reposeEndpoint)
    }


    def cleanupSpec() {
        if (repose) {
            repose.stop()
        }
        if (deproxy) {
            deproxy.shutdown()
        }
    }

    @Ignore("Jetty 'helpfully' rejects this one with a 400")
    def "send a header with a japanese name"() {

        when:
        MessageChain mc = deproxy.makeRequest([url: reposeEndpoint + "/resource", method: "get", headers: ['バナナ': 'phone']])

        then:
        mc.receivedResponse.code == "200"
        mc.handlings.get(0).request.headers.getFirstValue('バナナ') == 'phone'
    }

    @Ignore("""This one is mangled by the time it hits the first filter, not certain if it's us or jetty.
            By the time it goes out the backdoor it's no longer usable and gets rejected by deproxy.""")
    def "send a header with a japanese value"() {

        when:
        MessageChain mc = deproxy.makeRequest([url: reposeEndpoint + "/resource", method: "get", headers: ['banana': '電話']])

        then:
        mc.receivedResponse.code == "200"
        mc.handlings.get(0).request.headers.getFirstValue('banana') == '電話'
    }

    @Ignore("Comes in fine works till we need to go to the origin service, then we encode it and change the url.")
    def "send a request with a japanese url"() {

        when:
        MessageChain mc = deproxy.makeRequest([url: reposeEndpoint + "/ガンダム", method: "get", headers: ['banana': 'phone']])

        then:
        mc.receivedResponse.code == "200"
        mc.handlings.size() == 1
        mc.handlings.get(0).request.path.endsWith("/ガンダム")
    }

    @Ignore("Whelp this transmits fine, but by the time it gets to a filter its gone, not sure if its jetty or repose eating it")
    def "send a request with a japanese body"() {

        when:
        MessageChain mc = deproxy.makeRequest([url: reposeEndpoint + "/resource",
                                               method: "post",
                                               headers: ['banana': 'phone', 'Content-Type': 'application/json; charset=utf-8'],
                                               requestBody: '''{"バナナ": "電話"}'''.getBytes(UTF_8),
                                               clientConnector: new ApacheClientConnector()])

        then:
        mc.receivedResponse.code == "200"
        mc.handlings.size() == 1
        mc.handlings.get(0).request.body == '''{"バナナ": "電話"}'''
    }
}
