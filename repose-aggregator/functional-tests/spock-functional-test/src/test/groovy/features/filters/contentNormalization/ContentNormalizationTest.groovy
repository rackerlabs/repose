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
package features.filters.contentNormalization

import framework.ReposeValveTest
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain
import spock.lang.Unroll

class ContentNormalizationTest extends ReposeValveTest {

    def setupSpec() {
        def params = properties.defaultTemplateParams
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/filters/contentnormalization", params)
        repose.start()
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort)

    }

    def cleanupSpec() {
        repose.stop()
        deproxy.shutdown()
    }

    @Unroll("Send req with Accept Headers #sendAcceptHeaders when content normalizing will be #acceptHeaders")
    def "When content normalizing with Accept Headers contains #sendAcceptHeaders then Accept Headers #acceptHeaders" () {
        given:
        def headers = []
        def acceptHeaderList = acceptHeaders.split(',')
        if(sendAcceptHeaders != null){
            sendAcceptHeaders.split(',').each {
                headers << ['accept': it]
            }
            //headers = ['accept':sendAcceptHeaders]
        }


        when:
        MessageChain mc = null

        mc = deproxy.makeRequest(
                [
                        method: 'GET',
                        url:reposeEndpoint + "/v1/usertest1/servers/something",
                        headers:headers
                ])


        then:
        mc.handlings.size() == 1
        mc.receivedResponse.code == '200'
        mc.handlings[0].request.headers.findAll("accept") == acceptHeaderList

        where:
        sendAcceptHeaders                               |acceptHeaders
        'application/xml'                               |'application/xml'
        'application/xml,application/json'              |'application/json'
        'application/other'                             |'application/other'
        'application/other,application/xml'             |'application/xml'
        'html/text,application/xml'                     |'application/xml'
        'application/xml,html/text'                     |'application/xml'
        'application/xml,html/text,application/json'    |'application/json'
        '*/*,application/json'                          |'application/json'
        '*/*'                                           |'application/json'
        null                                            |'application/json'
        'application/json;q=1,application/xml;q=0.5'    |'application/json'
        'application/xml;q=1,application/json;q=0.5'    |'application/json'
        'application/xml;q=1'                           |'application/xml'
        '*/json'                                        |'application/json'
        '*/other'                                       |'application/json'

    }
}
