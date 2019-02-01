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
package features.filters.identityv3.authz

import org.joda.time.DateTime
import org.junit.experimental.categories.Category
import org.openrepose.framework.test.ReposeValveTest
import org.openrepose.framework.test.mocks.MockIdentityV3Service
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain
import scaffold.category.Filters
import spock.lang.Unroll

/**
 * Created by adrian on 8/16/16.
 */
@Category(Filters)
class IdentityV3AuthZBypassTest extends ReposeValveTest {
    def static originEndpoint
    def static identityEndpoint
    def static targetPort

    static MockIdentityV3Service fakeIdentityV3Service

    def setupSpec() {
        deproxy = new Deproxy()

        def params = properties.defaultTemplateParams
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/filters/identityv3/common", params)
        repose.configurationProvider.applyConfigs("features/filters/identityv3/authz/bypass", params)
        repose.start()

        targetPort = properties.targetPort
        originEndpoint = deproxy.addEndpoint(properties.targetPort, 'origin service')
        fakeIdentityV3Service = new MockIdentityV3Service(properties.identityPort, properties.targetPort)
        identityEndpoint = deproxy.addEndpoint(properties.identityPort,
                'identity service', null, fakeIdentityV3Service.handler)
        //targetPort = properties.targetPort
    }

    @Unroll("With Project ID: #requestProject, return from identity with response project: #responseProject, and role: #serviceAdminRole, return 200")
    def "when authenticating project id and roles that bypass"() {
        given:
        fakeIdentityV3Service.with {
            client_token = UUID.randomUUID().toString()
            tokenExpiresAt = DateTime.now().plusDays(1)
            client_projectid = responseProject
            service_admin_role = serviceAdminRole
            client_userid = requestProject
            endpointUrl = "localhost"
            servicePort = targetPort
        }

        when:
        "User passes a request through repose with request tenant: $requestProject, response tenant: $responseProject in a bypassed role = $serviceAdminRole"
        MessageChain mc = deproxy.makeRequest(
                url: "$reposeEndpoint/servers/$requestProject/",
                method: 'GET',
                headers: [
                        'X-Subject-Token': fakeIdentityV3Service.client_token
                ]
        )

        then: "Request body sent from repose to the origin service should contain"
        mc.receivedResponse.code == responseCode
        mc.handlings.size() == 1
        mc.handlings[0].endpoint == originEndpoint
        def request2 = mc.handlings[0].request
        request2.headers.getFirstValue("x-forwarded-for") == "127.0.0.1"
        //if projectId as tenantid in keystonev2 we expect should behave the same
        //then x-project-id should contain default project id - this isn't sure an issue
        //Comment this out for now will state in issue REP-3006
        //TODO - enable this after issue fix
        //request2.headers.getFirstValue("x-project-id") == responseProject.toString()
        request2.headers.contains("x-token-expires")
        request2.headers.getFirstValue("x-pp-user") == fakeIdentityV3Service.client_username + ";q=1.0"
        request2.headers.contains("x-roles")
        request2.headers.getFirstValue("x-authorization") == "Proxy"
        request2.headers.getFirstValue("x-user-name") == "username"

        mc.receivedResponse.headers.contains("www-authenticate") == false

        where:
        requestProject | responseProject | serviceAdminRole      | endpoint        | responseCode
        717            | 717             | "not-admin"           | "localhost"     | "200"
        718            | 719             | "service:admin-role1" | "rackspace.com" | "200"
        719            | 720             | "service:admin-role2" | "rackspace.com" | "200"
    }
}
