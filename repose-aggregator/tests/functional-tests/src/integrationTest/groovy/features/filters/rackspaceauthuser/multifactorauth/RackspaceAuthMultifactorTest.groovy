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
package features.filters.rackspaceauthuser.multifactorauth

import framework.ReposeValveTest
import org.apache.commons.lang3.RandomStringUtils
import org.openrepose.commons.utils.http.OpenStackServiceHeader
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain
import org.rackspace.deproxy.PortFinder
import org.rackspace.deproxy.Response

class RackspaceAuthMultifactorTest extends ReposeValveTest {
    static final String USERNAME = 'test-username'
    static final String PASSWORD = 'test-password'
    static final String PASSCODE = '1411594'

    static final String MFA_INITIAL_REQUEST_BODY =
            """
            {
                "auth": {
                    "passwordCredentials": {
                        "username": "${USERNAME}",
                        "password": "${PASSWORD}"
                    }
                }
            }
            """
    static final String MFA_FOLLOW_UP_REQUEST_BODY =
            """
            {
                "auth": {
                    "RAX-AUTH:passcodeCredentials": {
                        "passcode": "${PASSCODE}"
                    }
                }
            }
            """
    static final String MFA_CHALLENGE_RESPONSE_BODY =
            """
            {
                "unauthorized": {
                    "code": 401,
                    "message": "Additional authentication credentials required."
                }
            }
            """
    static final String MFA_SUCCESS_RESPONSE_BODY =
            """
            {
                "access": {
                    "serviceCatalog": [],
                    "token": {
                        "RAX-AUTH:authenticatedBy": [
                            "PASSCODE",
                            "PASSWORD"
                        ],
                        "expires": "2014-01-09T15:08:53.645-06:00",
                        "id": "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx"
                    },
                    "user": {
                        "RAX-AUTH:defaultRegion": "IAD",
                        "RAX-AUTH:federated": false,
                        "RAX-AUTH:multiFactorEnabled": true,
                        "id": "a64ee2047fc14cc7bc977caa3cfff35f",
                        "name": "${USERNAME}",
                        "roles": [
                            {
                                "description": "User Admin Role.",
                                "id": "3",
                                "name": "identity:user-admin"
                            }
                        ]
                    }
                }
            }
            """
    static final Closure<Response> MFA_SUCCESS_RESPONSE = {
        new Response(
                200,
                null,
                [
                        'Vary'          : 'Accept, Accept-Encoding, X-Auth-Token',
                        'Content-Type'  : 'application/json',
                        'Content-Length': MFA_SUCCESS_RESPONSE_BODY.length() as String
                ],
                MFA_SUCCESS_RESPONSE_BODY)
    }

    static int reposePort2
    static String reposeEndpoint2

    Closure<Response> getMfaChallengeResponseHandler(String sessionId) {
        return {
            new Response(
                    401,
                    null,
                    [
                            'WWW-Authenticate': "OS-MF sessionId='${sessionId}', factor='PASSCODE'",
                            'Vary'            : 'Accept, Accept-Encoding, X-Auth-Token',
                            'Content-Type'    : 'application/json',
                            'Content-Length': MFA_CHALLENGE_RESPONSE_BODY.length() as String
                    ],
                    MFA_CHALLENGE_RESPONSE_BODY)
        }
    }

    def setupSpec() {
        int ddPort1 = PortFinder.Singleton.getNextOpenPort()
        int ddPort2 = PortFinder.Singleton.getNextOpenPort()
        reposePort2 = PortFinder.Singleton.getNextOpenPort()
        reposeEndpoint2 = "http://localhost:${reposePort2}".toString()

        def params = properties.defaultTemplateParams
        params += [
                reposePort1: params.reposePort,
                reposePort2: reposePort2,
                ddPort1    : ddPort1,
                ddPort2    : ddPort2
        ]

        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort)

        repose.configurationProvider.applyConfigs('common', params)
        repose.configurationProvider.applyConfigs('features/filters/rackspaceauthuser/multifactorauth', params)
        repose.start(clusterId: 'cluster1', nodeId: 'node1')
        repose.waitForNon500FromUrl(reposeEndpoint)
        repose.waitForNon500FromUrl(reposeEndpoint2)
    }

    def "the username header should be set on the request for the initial mfa request"() {
        when: 'the initial authentication request is made'
        MessageChain messageChain = deproxy.makeRequest(
                url: "${reposeEndpoint}/v2.0/tokens/".toString(),
                method: 'POST',
                headers: ['Content-type': 'application/json'],
                requestBody: MFA_INITIAL_REQUEST_BODY,
                defaultHandler: getMfaChallengeResponseHandler(RandomStringUtils.randomAlphanumeric(60)))

        then: 'the request received by the origin service should contain a username header'
        messageChain.receivedResponse.code.toInteger() == 401
        !messageChain.sentRequest.headers.contains(OpenStackServiceHeader.USER_NAME.toString())
        messageChain.handlings[0].request.headers.getFirstValue(OpenStackServiceHeader.USER_NAME.toString()) == USERNAME
    }

    def "the username header should be set on the request for the additional mfa request"() {
        given: 'the initial authentication request is made'
        String sessionId = RandomStringUtils.randomAlphanumeric(60)
        deproxy.makeRequest(
                url: "${reposeEndpoint}/v2.0/tokens/".toString(),
                method: 'POST',
                headers: ['Content-type': 'application/json'],
                requestBody: MFA_INITIAL_REQUEST_BODY,
                defaultHandler: getMfaChallengeResponseHandler(sessionId))

        when: 'the follow-up authentication request is made'
        MessageChain messageChain = deproxy.makeRequest(
                url: "${reposeEndpoint}/v2.0/tokens/".toString(),
                method: 'POST',
                headers: [
                        'Content-type': 'application/json',
                        'X-SessionId' : sessionId
                ],
                requestBody: MFA_FOLLOW_UP_REQUEST_BODY,
                defaultHandler: MFA_SUCCESS_RESPONSE)

        then: 'the request received by the origin service should contain a username header'
        messageChain.receivedResponse.code.toInteger() == 200
        !messageChain.sentRequest.headers.contains(OpenStackServiceHeader.USER_NAME.toString())
        messageChain.handlings[0].request.headers.getFirstValue(OpenStackServiceHeader.USER_NAME.toString()) == USERNAME
    }

    def "the username header should be set on the request for the additional mfa request when there are multiple session IDs"() {
        given: 'the initial authentication request is made'
        String sessionId = RandomStringUtils.randomAlphanumeric(60)
        deproxy.makeRequest(
                url: "${reposeEndpoint}/v2.0/tokens/".toString(),
                method: 'POST',
                headers: ['Content-type': 'application/json'],
                requestBody: MFA_INITIAL_REQUEST_BODY,
                defaultHandler: getMfaChallengeResponseHandler(sessionId))

        when: 'the follow-up authentication request is made'
        MessageChain messageChain = deproxy.makeRequest(
                url: "${reposeEndpoint}/v2.0/tokens/".toString(),
                method: 'POST',
                headers: [
                        'Content-type': 'application/json',
                        'X-SessionId' : "${sessionId};q=0.5,foobarbazsession;q=0.8"
                ],
                requestBody: MFA_FOLLOW_UP_REQUEST_BODY,
                defaultHandler: MFA_SUCCESS_RESPONSE)

        then: 'the request received by the origin service should contain a username header'
        messageChain.receivedResponse.code.toInteger() == 200
        !messageChain.sentRequest.headers.contains(OpenStackServiceHeader.USER_NAME.toString())
        messageChain.handlings[0].request.headers.getFirstValue(OpenStackServiceHeader.USER_NAME.toString()) == USERNAME
    }

    def "the distributed datastore is used to cache the username"() {
        given: 'the initial authentication request is made'
        String sessionId = RandomStringUtils.randomAlphanumeric(60)
        deproxy.makeRequest(
                url: "${reposeEndpoint}/v2.0/tokens/".toString(),
                method: 'POST',
                headers: ['Content-type': 'application/json'],
                requestBody: MFA_INITIAL_REQUEST_BODY,
                defaultHandler: getMfaChallengeResponseHandler(sessionId))

        when: 'the follow-up authentication request is made to a different endpoint from the initial request'
        MessageChain messageChain = deproxy.makeRequest(
                url: "${reposeEndpoint2}/v2.0/tokens/".toString(),
                method: 'POST',
                headers: [
                        'Content-type': 'application/json',
                        'X-SessionId' : sessionId
                ],
                requestBody: MFA_FOLLOW_UP_REQUEST_BODY,
                defaultHandler: MFA_SUCCESS_RESPONSE)

        then: 'the request received by the origin service should contain a username header'
        messageChain.receivedResponse.code.toInteger() == 200
        !messageChain.sentRequest.headers.contains(OpenStackServiceHeader.USER_NAME.toString())
        messageChain.handlings[0].request.headers.getFirstValue(OpenStackServiceHeader.USER_NAME.toString()) == USERNAME
    }

    def "the cached session ID is re-usable"() {
        given: 'the initial authentication request is made'
        String sessionId = RandomStringUtils.randomAlphanumeric(60)
        deproxy.makeRequest(
                url: "${reposeEndpoint}/v2.0/tokens/".toString(),
                method: 'POST',
                headers: ['Content-type': 'application/json'],
                requestBody: MFA_INITIAL_REQUEST_BODY,
                defaultHandler: getMfaChallengeResponseHandler(sessionId))

        and: 'the follow-up authentication request is made'
        deproxy.makeRequest(
                url: "${reposeEndpoint2}/v2.0/tokens/".toString(),
                method: 'POST',
                headers: [
                        'Content-type': 'application/json',
                        'X-SessionId' : sessionId
                ],
                requestBody: MFA_FOLLOW_UP_REQUEST_BODY,
                defaultHandler: MFA_SUCCESS_RESPONSE)

        when: 'another authentication request is made with the same session ID'
        MessageChain messageChain = deproxy.makeRequest(
                url: "${reposeEndpoint}/v2.0/tokens/".toString(),
                method: 'POST',
                headers: [
                        'Content-type': 'application/json',
                        'X-SessionId' : sessionId
                ],
                requestBody: MFA_FOLLOW_UP_REQUEST_BODY,
                defaultHandler: MFA_SUCCESS_RESPONSE)

        then: 'the request received by the origin service should contain a username header'
        messageChain.receivedResponse.code.toInteger() == 200
        !messageChain.sentRequest.headers.contains(OpenStackServiceHeader.USER_NAME.toString())
        messageChain.handlings[0].request.headers.getFirstValue(OpenStackServiceHeader.USER_NAME.toString()) == USERNAME
    }

    def "if the session ID has not been seen, processing should continue, but a message should be logged"() {
        when: 'the follow-up authentication request is made with a bad session ID'
        MessageChain messageChain = deproxy.makeRequest(
                url: "${reposeEndpoint2}/v2.0/tokens/".toString(),
                method: 'POST',
                headers: [
                        'Content-type': 'application/json',
                        'X-SessionId' : 'not-a-session-id'
                ],
                requestBody: MFA_FOLLOW_UP_REQUEST_BODY,
                defaultHandler: MFA_SUCCESS_RESPONSE)

        then: 'the request received by the origin service should not contain a username header'
        messageChain.receivedResponse.code.toInteger() == 200
        !messageChain.sentRequest.headers.contains(OpenStackServiceHeader.USER_NAME.toString())
        !messageChain.handlings[0].request.headers.contains(OpenStackServiceHeader.USER_NAME.toString())
        reposeLogSearch.searchByString('The provided session ID has not been seen before. ' +
                'The username header will not be set, but processing will continue.')
    }
}
