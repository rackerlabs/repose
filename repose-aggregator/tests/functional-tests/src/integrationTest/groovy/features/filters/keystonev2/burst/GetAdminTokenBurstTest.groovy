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
package features.filters.keystonev2.burst

import org.junit.experimental.categories.Category
import org.openrepose.framework.test.ReposeValveTest
import org.openrepose.framework.test.mocks.MockIdentityV2Service
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.Request
import org.rackspace.deproxy.Response
import scaffold.category.Filters

import java.util.concurrent.CopyOnWriteArrayList

import static javax.servlet.http.HttpServletResponse.SC_INTERNAL_SERVER_ERROR
import static javax.servlet.http.HttpServletResponse.SC_OK

@Category(Filters)
class GetAdminTokenBurstTest extends ReposeValveTest {
    static final String X_AUTH_TOKEN = "X-Auth-Token"
    static final String CONTENT_TYPE = "Content-Type"
    static final String X_ROLES = "x-roles"

    static MockIdentityV2Service fakeIdentityV2Service

    def setupSpec() {
        reposeLogSearch.cleanLog()

        def params = properties.defaultTemplateParams
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/filters/keystonev2/common", params)

        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort, 'origin service')

        fakeIdentityV2Service = new MockIdentityV2Service(properties.identityPort, properties.targetPort)
        deproxy.addEndpoint(properties.identityPort, 'identity service', null, fakeIdentityV2Service.handler)

        deproxy.defaultHandler = { Request request ->
            if (!request.headers.contains(X_AUTH_TOKEN)) {
                new Response(SC_INTERNAL_SERVER_ERROR, null, [(CONTENT_TYPE): "text/plain"], "MISSING AUTH TOKEN")
            } else {
                new Response(SC_OK)
            }
        }

        repose.start()
        reposeLogSearch.awaitByString("Repose ready", 1, 30)
    }

    /**
     * This test occasionally fails because threading problems
     * https://repose.atlassian.net/browse/REP-558
     */
    def "under heavy load should only retrieve admin token once"() {
        given: "there will be 50 different clients (threads) and each will make 10 calls to the origin service"
        def numClients = 50
        def callsPerClient = 10

        and: "we will keep track of any errors"
        // this test produces way too much output which ends up truncating anything useful, so save errors for the end
        def missingAuthTokenHeader = new CopyOnWriteArrayList()
        def missingRolesHeader = new CopyOnWriteArrayList()

        and: "each client (thread) will have their own X-Auth-Token"
        List<Thread> clientThreads = (1..numClients).collect { threadNum ->
            Map requestHeaders = [(X_AUTH_TOKEN): UUID.randomUUID().leastSignificantBits as String]
            Thread.start {
                (1..callsPerClient).each { callNum ->
                    def mc = deproxy.makeRequest(url: reposeEndpoint, method: 'GET', headers: requestHeaders)

                    if (mc.receivedResponse.code as Integer == SC_INTERNAL_SERVER_ERROR) {
                        def orphanedHandlings = mc.orphanedHandlings.withIndex().collect { handling, index ->
                            "($index) request: ${handling.request?.body}, response: ${handling.response?.body}\n" }
                        def error = """Error for thread $threadNum, call $callNum:
                                      |Body received by client:
                                      |${mc.receivedResponse.body}
                                      |Orphaned handlings:
                                      |$orphanedHandlings""".stripMargin()
                        missingAuthTokenHeader.add(error)
                    } else if (mc.handlings[0].request.headers.getCountByName(X_ROLES) == 0) {
                        def error = """Error for thread $threadNum, call $callNum:
                                      |Request headers received by origin service:
                                      |${mc.handlings[0].request.headers}""".stripMargin()
                        missingRolesHeader.add(error)
                    }
                }
            }
        }

        when: "the clients make their requests"
        clientThreads*.join()

        then: "the admin token is only generated once"
        fakeIdentityV2Service.generateTokenCount == 1

        and: "there were no missing auth token errors"
        missingAuthTokenHeader.isEmpty()

        and: "there were no missing role header errors"
        missingRolesHeader.isEmpty()
    }

}
