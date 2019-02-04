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

package features.filters.keystonev2.userAttributeHeaders

import org.joda.time.DateTime
import org.junit.experimental.categories.Category
import org.openrepose.framework.test.ReposeValveTest
import org.openrepose.framework.test.mocks.MockIdentityV2Service
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain
import scaffold.category.Identity

/**
 B-50304
 Pass region in header

 Description:
 As the origin service, we want the users default region to be passed back
 from the validate token response in the Auth component so that we know the
 users default region.

 Acceptance Criteria:
 The users default region is passed as header name x-default-region in the
 header.  This value comes from rax-auth:default-region from the validate
 token response.

 Test Plan
 1. Use a simple mock origin service that returns 200 for all requests, and
 the mock identity service to validate tokens and return a pre-determined
 default region for the user associated with the token.
 2. Make a call to Repose with the test token. Check that:
 a. The mock identity service received a single validate-token request.
 b. The handlings list has one entry, indicating that the request
 reached the origin service
 c. The request on that handling has a header named "X-Default-Region"
 having as its value the default region returned by the mock identity
 service
 3. Make a second call to Repose with the same token (should be cached).
 Check that:
 a. The mock identity service did not receive any validate-token
 requests, since the token info should be cached.
 b. The handlings list has one entry, indicating that the request
 reached the origin service
 c. The request on that handling has a header named "X-Default-Region"
 having as its value the default region returned by the mock identity
 service
 */

@Category(Identity)
class PassUserAttributesInHeaderTest extends ReposeValveTest {

    def originEndpoint
    def identityEndpoint

    MockIdentityV2Service fakeIdentityV2Service

    def setup() {
        deproxy = new Deproxy()

        def params = properties.defaultTemplateParams
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/filters/keystonev2/common", params)
        repose.configurationProvider.applyConfigs("features/filters/keystonev2/userAttributeHeaders", params)
        repose.configurationProvider.applyConfigs("features/filters/keystonev2/connectionpooling", params)
        repose.start()

        originEndpoint = deproxy.addEndpoint(properties.targetPort, 'origin service')

        fakeIdentityV2Service = new MockIdentityV2Service(properties.identityPort, properties.targetPort)
        fakeIdentityV2Service.resetCounts()
        identityEndpoint = deproxy.addEndpoint(properties.identityPort,
                'identity service', null, fakeIdentityV2Service.handler)

    }

    def cleanup() {
        if (deproxy) {
            deproxy.shutdown()
        }
        repose.stop()
    }


    def "when a token is validated, should pass the default region as X-Default-Region"() {

        when: "I send a GET request to Repose with an X-Auth-Token header"
        fakeIdentityV2Service.with {
            client_token = "racker456"
            tokenExpiresAt = DateTime.now().plusDays(1)
            client_userid = "456"
        }
        fakeIdentityV2Service.resetCounts()
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint, method: 'GET', headers: ['X-Auth-Token': fakeIdentityV2Service.client_token])

        then: "Repose should validate the token and path the user's default region as the X-Default_Region header to the origin service"
        mc.receivedResponse.code == "200"
        fakeIdentityV2Service.validateTokenCount == 1
        mc.handlings.size() == 1
        mc.handlings[0].endpoint == originEndpoint
        def request = mc.handlings[0].request
        request.headers.contains("X-Default-Region")
        request.headers.getFirstValue("X-Default-Region") == "DFW"

        when: "I send a second GET request to Repose with the same token"
        fakeIdentityV2Service.resetCounts()
        mc = deproxy.makeRequest(url: reposeEndpoint, method: 'GET', headers: ['X-Auth-Token': fakeIdentityV2Service.client_token])

        then: "Repose should use the cache, not call out to the fake identity service, and pass the request to origin service with the same X-Default-Region header"
        mc.receivedResponse.code == "200"
        fakeIdentityV2Service.validateTokenCount == 0
        mc.handlings.size() == 1
        mc.handlings[0].endpoint == originEndpoint
        def request2 = mc.handlings[0].request
        request2.headers.contains("X-Default-Region")
        request2.headers.getFirstValue("X-Default-Region") == "DFW"

    }

    def "when a token is validated, repose should pass the contactID attribute as X-Contact-ID"() {
        when: "I send a GET request to Repose with an X-Auth-Token header"
        fakeIdentityV2Service.with {
            client_token = "racker457"
            tokenExpiresAt = DateTime.now().plusDays(1)
            client_userid = "457"
            contact_id = "the-contactID"
        }
        fakeIdentityV2Service.resetCounts()
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint, method: 'GET', headers: ['X-Auth-Token': fakeIdentityV2Service.client_token])

        then: "Repose should validate the token and pass the user's contact ID as the X-ContactID header to the origin service"
        mc.receivedResponse.code == "200"
        fakeIdentityV2Service.validateTokenCount == 1
        mc.handlings.size() == 1
        mc.handlings[0].endpoint == originEndpoint
        def request = mc.handlings[0].request
        request.headers.contains("X-Contact-ID")
        request.headers.getFirstValue("X-Contact-ID") == "the-contactID"

        when: "I send a second GET request to Repose with the same token"
        fakeIdentityV2Service.resetCounts()
        mc = deproxy.makeRequest(url: reposeEndpoint, method: 'GET', headers: ['X-Auth-Token': fakeIdentityV2Service.client_token])

        then: "Repose should use the cache, not call out to the fake identity service, and pass the request to origin service with the same X-ContactID header"
        mc.receivedResponse.code == "200"
        fakeIdentityV2Service.validateTokenCount == 0
        mc.handlings.size() == 1
        mc.handlings[0].endpoint == originEndpoint
        def request2 = mc.handlings[0].request
        request2.headers.contains("X-Contact-ID")
        request2.headers.getFirstValue("X-Contact-ID") == "the-contactID"

    }

    def "when a token is validated and contactID does not exist in response, X-Contact-ID should not be passed in"() {

        when: "I send a GET request to Repose with an X-Auth-Token header"
        fakeIdentityV2Service.with {
            client_token = "racker457"
            tokenExpiresAt = DateTime.now().plusDays(1)
            client_userid = "457"
            contact_id = null
        }
        fakeIdentityV2Service.resetCounts()
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint, method: 'GET', headers: ['X-Auth-Token': fakeIdentityV2Service.client_token])

        then: "Repose should validate the token and pass the user's contact ID as the X-ContactID header to the origin service"
        mc.receivedResponse.code == "200"
        fakeIdentityV2Service.validateTokenCount == 1
        mc.handlings.size() == 1
        mc.handlings[0].endpoint == originEndpoint
        def request = mc.handlings[0].request
        !request.headers.contains("X-Contact-ID")

        when: "I send a second GET request to Repose with the same token"
        fakeIdentityV2Service.resetCounts()
        mc = deproxy.makeRequest(url: reposeEndpoint, method: 'GET', headers: ['X-Auth-Token': fakeIdentityV2Service.client_token])

        then: "Repose should use the cache, not call out to the fake identity service, and pass the request to origin service with the same X-ContactID header"
        mc.receivedResponse.code == "200"
        fakeIdentityV2Service.validateTokenCount == 0
        mc.handlings.size() == 1
        mc.handlings[0].endpoint == originEndpoint
        def request2 = mc.handlings[0].request
        !request2.headers.contains("X-Contact-ID")

    }
}
