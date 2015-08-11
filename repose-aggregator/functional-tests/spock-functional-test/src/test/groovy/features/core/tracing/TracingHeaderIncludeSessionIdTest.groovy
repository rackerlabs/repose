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
package features.core.tracing

import framework.ReposeValveTest
import framework.mocks.MockIdentityService
import org.joda.time.DateTime
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain
import spock.lang.Unroll

/**
 * Created by jennyvo on 8/10/15.
 * Verify Tracing header x-trans-id also include sessionid and requestid
 */
class TracingHeaderIncludeSessionIdTest extends ReposeValveTest {

    def static originEndpoint
    def static identityEndpoint

    def static MockIdentityService fakeIdentityService

    def setupSpec() {

        deproxy = new Deproxy()

        def params = properties.defaultTemplateParams
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/core/tracing", params)
        repose.start()

        originEndpoint = deproxy.addEndpoint(properties.targetPort, 'origin service')
        fakeIdentityService = new MockIdentityService(properties.identityPort, properties.targetPort)
        identityEndpoint = deproxy.addEndpoint(properties.identityPort,
                'identity service', null, fakeIdentityService.handler)

        reposeLogSearch.cleanLog()
    }

    def cleanupSpec() {
        deproxy.shutdown()

        repose.stop()
    }

    def setup() {
        sleep 500
        fakeIdentityService.resetHandlers()
    }

    @Unroll ("Checking with session id string: #sessionid")
    def "Trans id should include session id"() {

        given:
        fakeIdentityService.with {
            client_tenant = 1212
            client_userid = 1212
            client_token = UUID.randomUUID().toString()
            tokenExpiresAt = DateTime.now().plusDays(1)
        }

        def url = "$reposeEndpoint/servers/1212"
        if (sessionid != ""){
            url = url + ";" + sessionid
        }


        when: "User passes a request through repose"
        MessageChain mc = deproxy.makeRequest(
                url: url,
                method: 'GET',
                headers: [
                        'content-type': 'application/json',
                        'X-Auth-Token': fakeIdentityService.client_token
                ]
        )
        // get tracing header from request
        def transid = mc.handlings[0].request.headers.getFirstValue("x-trans-id")
        println transid
        def sesid = getSessionId(sessionid)
        println sesid
        def username = mc.handlings[0].request.headers.getFirstValue("x-pp-user")
        def requestid = mc.handlings[0].request.headers.getFirstValue("deproxy-request-id")

        then: "Make sure there are appropriate log messages with matching GUIDs"
        mc.receivedResponse.code == "200"

        transid.contains(requestid)
        transid.contains(sesid)
        transid.contains(username)

        // should be able to find the same tracing header from log
        reposeLogSearch.searchByString("GUID:$transid -.*AuthTokenFutureActor request!").size() > 0

        where:
        sessionid  << ["sessionid=abcdedfg1234567", "sessionid=1234567890", "1234567890", "sessionid=", ""]
    }

    def getSessionId (String stringsession){
        def id = ""
        if (stringsession.contains("sessionid=")){
            List session = stringsession.split("=")
            if (session.size() > 1) {
                id = session[1]
            }
        }
        return id
    }

}
