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

package features.filters.keystonev2.tokenexpireheader

import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import org.joda.time.format.DateTimeFormat
import org.joda.time.format.DateTimeFormatter
import org.junit.experimental.categories.Category
import org.openrepose.framework.test.ReposeValveTest
import org.openrepose.framework.test.mocks.MockIdentityV2Service
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain
import scaffold.category.Filters

/**
 B-50304
 Pass token expiration in header

 Description:
 As the origin service, we want the expiration date of token will be passed
 as header from the Auth component so that we know when stored tokens expire.

 Acceptance Criteria:
 Expiration date is passed as header name x-token-expires in the header.
 Format of type x-token-expires time follows http spec for time format. This
 will be converted to the http spec format. rfc1123... needs to be GMT time.

 Sun, 06 Nov 1994 08:49:37 GMT  ; RFC 822, updated by RFC 1123

 Test Plan
 1. Use a simple mock origin service that returns 200 for all requests, and
 the mock identity service to validate tokens and return a pre-determined
 expiration date for the token sent in the validation call.
 2. Make a call to Repose with the test token. Check that:
 a. The mock identity service received a single validate-token request.
 b. The handlings list has one entry, indicating that the request
 reached the origin service
 c. The request on that handling has a header named "X-Token-Expires"
 having as its value the default region returned by the mock identity
 service
 3. Make a second call to Repose with the same token (should be cached).
 Check that:
 a. The mock identity service did not receive any validate-token
 requests, since the token info should be cached.
 b. The handlings list has one entry, indicating that the request
 reached the origin service
 c. The request on that handling has a header named "X-Token-Expires"
 having as its value the default region returned by the mock identity
 service
 */

@Category(Filters)
class PassTokenExpirationInHeaderTest extends ReposeValveTest {

    def originEndpoint
    def identityEndpoint

    MockIdentityV2Service fakeIdentityV2Service

    def setup() {
        deproxy = new Deproxy()

        originEndpoint = deproxy.addEndpoint(properties.targetPort, 'origin service')

        fakeIdentityV2Service = new MockIdentityV2Service(properties.identityPort, properties.targetPort);

        fakeIdentityV2Service.with {
            tokenExpiresAt = DateTime.now().plusDays(1)
            resetHandlers()
            resetCounts()
        }


        identityEndpoint = deproxy.addEndpoint(properties.identityPort,
                'identity service', null, fakeIdentityV2Service.handler);

        def params = properties.defaultTemplateParams
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/filters/keystonev2/common", params)
        repose.configurationProvider.applyConfigs("features/filters/keystonev2/tokenexpireheader", params)
        repose.configurationProvider.applyConfigs("features/filters/keystonev2/connectionpooling", params)
        repose.start()
    }

    def cleanup() {
        if (deproxy) {
            deproxy.shutdown()
        }
        repose.stop()
    }

    def "when a token is validated, should pass the token expiration as X-Token-Expires"() {

        // Calculate what the string representation of the rfc1123 date will be
        DateTimeFormatter fmt = DateTimeFormat
                .forPattern("EEE, dd MMM yyyy HH:mm:ss 'GMT'")
                .withLocale(Locale.US)
                .withZone(DateTimeZone.UTC);
        def expiresString = fmt.print(fakeIdentityV2Service.tokenExpiresAt);

        when: "I send a GET request to Repose with an X-Auth-Token header"
        fakeIdentityV2Service.resetCounts()
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint, method: 'GET', headers: ['X-Auth-Token': fakeIdentityV2Service.client_token])

        then: "Repose should validate the token and path the token's expiration date/time as the X-Token-Expires header to the origin service"
        mc.receivedResponse.code == "200"
        fakeIdentityV2Service.validateTokenCount == 1
        mc.handlings.size() == 1
        mc.handlings[0].endpoint == originEndpoint
        def request = mc.handlings[0].request
        request.headers.contains("X-Token-Expires")
        request.headers.getFirstValue("X-Token-Expires") == expiresString



        when: "I send a second GET request to Repose with the same token"
        fakeIdentityV2Service.resetCounts()
        mc = deproxy.makeRequest(url: reposeEndpoint, method: 'GET', headers: ['X-Auth-Token': fakeIdentityV2Service.client_token])

        then: "Repose should use the cache, not call out to the fake identity service, and pass the request to origin service with the same X-Token-Expires header as before"
        mc.receivedResponse.code == "200"
        fakeIdentityV2Service.validateTokenCount == 0
        mc.handlings.size() == 1
        mc.handlings[0].endpoint == originEndpoint
        def request2 = mc.handlings[0].request
        request2.headers.contains("X-Token-Expires")
        request2.headers.getFirstValue("X-Token-Expires") == expiresString

    }
}
