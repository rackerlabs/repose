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
package features.filters.valkyrie

import org.junit.experimental.categories.Category
import org.openrepose.framework.test.ReposeValveTest
import org.openrepose.framework.test.mocks.MockIdentityV2Service
import org.openrepose.framework.test.mocks.MockValkyrie
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain
import scaffold.category.Filters

import static org.openrepose.commons.utils.http.OpenStackServiceHeader.ROLES

/**
 * Created by jennyvo on 9/11/15.
 *  Bypass the calls to Valkyrie if configured roles exist.
 *    This also means we will not do culling, device authorization or role translation
 */
@Category(Filters)
class TranslateRolesBypassValkyrieTest extends ReposeValveTest {
    def static originEndpoint
    def static identityEndpoint
    def static valkyrieEndpoint

    def static MockIdentityV2Service fakeIdentityV2Service
    def static MockValkyrie fakeValkyrie
    def static Map params = [:]

    def static random = new Random()

    def setupSpec() {
        deproxy = new Deproxy()

        params = properties.getDefaultTemplateParams()
        repose.configurationProvider.cleanConfigDirectory()
        repose.configurationProvider.applyConfigs("common", params);
        repose.configurationProvider.applyConfigs("features/filters/valkyrie", params);
        repose.configurationProvider.applyConfigs("features/filters/valkyrie/bypassvalkyrie", params);
        repose.configurationProvider.applyConfigs("features/filters/valkyrie/bypassvalkyrie/withtranslaterolesconfig", params);

        repose.start()

        originEndpoint = deproxy.addEndpoint(properties.targetPort, 'origin service')
        fakeIdentityV2Service = new MockIdentityV2Service(properties.identityPort, properties.targetPort)
        identityEndpoint = deproxy.addEndpoint(properties.identityPort, 'identity service', null, fakeIdentityV2Service.handler)
        fakeIdentityV2Service.checkTokenValid = true

        fakeValkyrie = new MockValkyrie(properties.valkyriePort)
        valkyrieEndpoint = deproxy.addEndpoint(properties.valkyriePort, 'valkyrie service', null, fakeValkyrie.handler)
    }

    def setup() {
        fakeIdentityV2Service.resetDefaultParameters()
    }

    def "Get Account level permissions and translate to roles"() {
        given:
        fakeIdentityV2Service.with {
            client_apikey = UUID.randomUUID().toString()
            client_token = UUID.randomUUID().toString()
            client_tenantid = tenantID
        }
        fakeValkyrie.with {
                            account_perm = "test_perm"
        }

        when: "a request is made against a device with Valkyrie set permissions"
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint + "/account/permissions", method: method,
                headers: [
                        'content-type': 'application/json',
                        'X-Auth-Token': fakeIdentityV2Service.client_token,
                ]
        )

        then:
        mc.receivedResponse.code == responseCode
        mc.handlings[0].request.headers.findAll(ROLES).contains("test_perm")
        mc.handlings[0].request.headers.findAll(ROLES).contains("upgrade_account")
        mc.handlings[0].request.headers.findAll(ROLES).contains("edit_ticket")
        mc.handlings[0].request.headers.findAll(ROLES).contains("edit_domain")
        mc.handlings[0].request.headers.findAll(ROLES).contains("manage_users")
        mc.handlings[0].request.headers.findAll(ROLES).contains("view_domain")
        mc.handlings[0].request.headers.findAll(ROLES).contains("view_reports")

        //**This for tracing header on failed response REP-2147
        mc.receivedResponse.headers.contains("x-trans-id")
        //**This part for tracing header test REP-1704**
        // any requests send to identity also include tracing header
        mc.orphanedHandlings.each {
            e -> assert e.request.headers.contains("x-trans-id")
        }
        // include request make to valkyrie
        assert mc.orphanedHandlings[3].request.path =~ "/account/(|-)\\d*/permissions/contacts/any/by_contact/(|-)\\d*/effective"

        where:
        method | tenantID       | responseCode
        "GET"  | randomTenant() | "200"
    }

    def "Bypass valkyrie test"() {
        given: "a list permission devices defined in Valkyrie token without tenantid associated with"
        fakeIdentityV2Service.with {
            client_token = "rackerSSO"
            service_admin_role = "racker"
            client_userid = "rackerSSOUsername"
        }

        when: "a request is made against a device with Valkyrie set permissions"
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint + "/account/permissions", method: "GET",
                headers: [
                        'content-type': 'application/json',
                        'X-Auth-Token': fakeIdentityV2Service.client_token,
                ]
        )

        then:
        mc.receivedResponse.code == "200"
        // verify not interact with valkyrie
        if (mc.orphanedHandlings.size() > 0) {
            mc.orphanedHandlings.each {
                e -> assert !e.request.path.contains("/account")
            }
        }
    }

    def String randomTenant() {
        "hybrid:" + random.nextInt()
    }
}
