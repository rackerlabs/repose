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
package features.filters.identityv3

import org.apache.commons.lang3.RandomStringUtils
import org.joda.time.DateTime
import org.junit.experimental.categories.Category
import org.openrepose.framework.test.ReposeValveTest
import org.openrepose.framework.test.mocks.MockIdentityV3Service
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain
import org.rackspace.deproxy.Response
import scaffold.category.Filters
import spock.lang.Unroll

/**
 * Created by jennyvo on 8/25/14.
 */
@Category(Filters)
class IdentityV3Test extends ReposeValveTest {
    def static originEndpoint
    def static identityEndpoint
    def static MockIdentityV3Service fakeIdentityV3Service

    def setupSpec() {
        deproxy = new Deproxy()
        reposeLogSearch.cleanLog()
        def params = properties.defaultTemplateParams
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/filters/identityv3/common", params)
        repose.start()
        waitUntilReadyToServiceRequests('401')

        originEndpoint = deproxy.addEndpoint(properties.targetPort, 'origin service')
        fakeIdentityV3Service = new MockIdentityV3Service(properties.identityPort, properties.targetPort)
        identityEndpoint = deproxy.addEndpoint(properties.identityPort,
                'identity service', null, fakeIdentityV3Service.handler)
    }

    def setup() {
        fakeIdentityV3Service.resetParameters()
        fakeIdentityV3Service.resetHandlers()
    }

    def "Test send request with user token"() {
        given:
        def reqDomain = fakeIdentityV3Service.client_domainid
        def reqUserId = fakeIdentityV3Service.client_userid

        fakeIdentityV3Service.with {
            client_token = UUID.randomUUID().toString()
            tokenExpiresAt = DateTime.now().plusDays(1)
            client_domainid = reqDomain
            client_userid = reqUserId
        }

        when: "User passes a request through repose"
        MessageChain mc = deproxy.makeRequest(
                url: "$reposeEndpoint/servers/$reqDomain/",
                method: 'GET',
                headers: [
                        'content-type'   : 'application/json',
                        'X-Subject-Token': fakeIdentityV3Service.client_token,
                ]
        )

        then: "Request body sent from repose to the origin service should contain"
        mc.receivedResponse.code == "200"
        mc.handlings.size() == 1
        // REP-2886 fix: ERROR org.openrepose.powerfilter.PowerFilterChain - Failure in filter: OpenStackIdentityV3Filter
        // when missing group discription
        reposeLogSearch.searchByString("ERROR org.openrepose.powerfilter.PowerFilterChain - Failure in filter: OpenStackIdentityV3Filter  -  Reason: Object is missing required member 'description'").size() == 0
    }

    def "Tracing header should include in request to Identity"() {
        def reqDomain = fakeIdentityV3Service.client_domainid


        fakeIdentityV3Service.with {
            client_token = UUID.randomUUID().toString()
            tokenExpiresAt = DateTime.now().plusDays(1)
        }

        when: "User passes a request through repose"
        MessageChain mc = deproxy.makeRequest(
                url: "$reposeEndpoint/servers/$reqDomain/",
                method: 'GET',
                headers: [
                        'content-type'   : 'application/json',
                        'X-Subject-Token': fakeIdentityV3Service.client_token,
                ]
        )

        then: "Request body sent from repose to the origin service should contain"
        mc.receivedResponse.code == "200"
        mc.handlings.size() == 1
        // any requests send to identity also include tracing header
        mc.orphanedHandlings.each {
            e ->
                assert e.request.headers.contains("x-trans-id")
                assert e.request.headers.getFirstValue("x-trans-id") == mc.handlings[0].request.headers.getFirstValue("x-trans-id")
        }
    }

    @Unroll("Failed response from repose with Identity respcode #identityrespcode")
    def "Tracing header should include in Failed response from repose"() {
        def reqDomain = fakeIdentityV3Service.client_domainid
        fakeIdentityV3Service.with {
            client_token = UUID.randomUUID().toString()
            tokenExpiresAt = DateTime.now().plusDays(1)
        }

        fakeIdentityV3Service.validateTokenHandler = {
            tokenId, request ->
                return new Response(identityrespcode)
        }
        when: "User passes a request through repose"
        MessageChain mc = deproxy.makeRequest(
                url: "$reposeEndpoint/servers/$reqDomain/",
                method: 'GET',
                headers: [
                        'content-type'   : 'application/json',
                        'X-Subject-Token': fakeIdentityV3Service.client_token,
                ]
        )

        then:
        mc.handlings.size() == 0
        mc.receivedResponse.code == respcode
        mc.receivedResponse.headers.contains("x-trans-id")
        mc.orphanedHandlings.each {
            e -> assert e.request.headers.contains("x-trans-id")
        }

        where:
        identityrespcode | respcode
        "401"            | "500"
        "403"            | "500"
        "413"            | "503"
        "404"            | "401"
        "500"            | "500"
    }

    def "Handle large Token test"() {
        given: "keystone v3 with random generate at least 226 char token"
        def largetoken = RandomStringUtils.random(226, 'ABCDEFGHIJKLMNOPQRSTUVWYZabcdefghijklmnopqrstuvwyz-_1234567890')
        println largetoken
        def reqDomain = fakeIdentityV3Service.client_domainid

        fakeIdentityV3Service.with {
            client_token = largetoken
            tokenExpiresAt = DateTime.now().plusDays(1)
        }

        when: "User passes a request through repose"
        MessageChain mc = deproxy.makeRequest(
                url: "$reposeEndpoint/servers/$reqDomain/",
                method: 'GET',
                headers: [
                        'content-type'   : 'application/json',
                        'X-Subject-Token': fakeIdentityV3Service.client_token,
                ]
        )

        then: "Request body sent from repose to the origin service should contain"
        mc.receivedResponse.code == "200"
        mc.handlings.size() == 1
    }

    def "No project-id in uri"() {
        fakeIdentityV3Service.with {
            client_token = UUID.randomUUID().toString()
            tokenExpiresAt = DateTime.now().plusDays(1)
        }

        when: "User passes a request through repose"
        MessageChain mc = deproxy.makeRequest(
                url: "$reposeEndpoint/servers/whatever/",
                method: 'GET',
                headers: [
                        'content-type'   : 'application/json',
                        'X-Subject-Token': fakeIdentityV3Service.client_token,
                ]
        )

        then: "Request body sent from repose to the origin service should contain"
        mc.receivedResponse.code == "200"
        mc.handlings.size() == 1
    }
}
