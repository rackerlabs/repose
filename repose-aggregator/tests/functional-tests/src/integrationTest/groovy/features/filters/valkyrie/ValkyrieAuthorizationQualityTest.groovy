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
import org.openrepose.framework.test.mocks.MockValkyrie
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain
import scaffold.category.Filters
import spock.lang.Shared
import spock.lang.Unroll

import static javax.servlet.http.HttpServletResponse.SC_FORBIDDEN
import static javax.servlet.http.HttpServletResponse.SC_OK
import static org.openrepose.commons.utils.http.CommonHttpHeader.AUTH_TOKEN
import static org.openrepose.commons.utils.http.OpenStackServiceHeader.CONTACT_ID
import static org.openrepose.commons.utils.http.OpenStackServiceHeader.TENANT_ID

@Category(Filters)
class ValkyrieAuthorizationQualityTest extends ReposeValveTest {
    static def HDR_DEVICE_ID = "X-Device-Id"

    @Shared
    MockValkyrie mockValkyrie

    @Shared
    def oneUpCounter = Math.abs(new Random().nextInt())

    @Shared
    String otherTenantId

    def setupSpec() {
        Map params = properties.getDefaultTemplateParams()
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/filters/valkyrie/quality", params)

        repose.start()

        mockValkyrie = new MockValkyrie(properties.valkyriePort)

        deproxy = new Deproxy()
        deproxy.addEndpoint(port: properties.targetPort)
        deproxy.addEndpoint(port: properties.valkyriePort, name: "valkyrie service", defaultHandler: mockValkyrie.getHandler())
    }


    def setup() {
        mockValkyrie.resetCounts()
        mockValkyrie.resetHandlers()
        mockValkyrie.resetParameters()
        mockValkyrie.with {
            valid_auth = UUID.randomUUID().toString()
            contact_id = ++oneUpCounter as String
            tenant_id = ++oneUpCounter as String
        }
        otherTenantId = ++oneUpCounter as String
    }

    @Unroll
    def "should allow #method calls with permission '#devicePermission' and quality '#quality'"() {
        given:
        mockValkyrie.device_perm = devicePermission

        when:
        MessageChain mc = deproxy.makeRequest(
            url: "$reposeEndpoint/",
            method: method,
            headers: [
                (AUTH_TOKEN)   : mockValkyrie.valid_auth,
                (TENANT_ID)    : "$otherTenantId;q=0.5,hybrid:${mockValkyrie.tenant_id}$quality",
                (CONTACT_ID)   : mockValkyrie.contact_id,
                (HDR_DEVICE_ID): mockValkyrie.device_id])

        then:
        mockValkyrie.getAuthorizationCount() == 1
        mc.getOrphanedHandlings().size() == 1
        mc.handlings.size() == 1
        mc.handlings[0].request.path == "/"
        List<String> tenantIdValues = mc.handlings[0].request.headers.findAll(TENANT_ID)
        tenantIdValues.any { it.contains("hybrid:${mockValkyrie.tenant_id}$quality" as String) }
        mc.receivedResponse.code as Integer == SC_OK

        where:
        [a, b, c] << [
            ["GET", "HEAD", "PUT", "POST", "DELETE", "PATCH"],
            ["edit_product", "admin_product", "account_admin"],
            [";q=0.98", ""]
        ].combinations()

        method = a
        devicePermission = b
        quality = c

    }

    @Unroll
    def "should allow #method calls with permission 'view_product' and quality '#quality'"() {
        given:
        mockValkyrie.device_perm = "view_product"


        when:
        MessageChain mc = deproxy.makeRequest(
            url: "$reposeEndpoint/",
            method: method,
            headers: [
                (AUTH_TOKEN)   : mockValkyrie.valid_auth,
                (TENANT_ID)    : "$otherTenantId;q=0.5,hybrid:${mockValkyrie.tenant_id}$quality",
                (CONTACT_ID)   : mockValkyrie.contact_id,
                (HDR_DEVICE_ID): mockValkyrie.device_id])

        then:
        mockValkyrie.getAuthorizationCount() == 1
        mc.getOrphanedHandlings().size() == 1
        mc.handlings.size() == 1
        mc.handlings[0].request.path == "/"
        List<String> tenantIdValues = mc.handlings[0].request.headers.findAll(TENANT_ID)
        tenantIdValues.any { it.contains("hybrid:${mockValkyrie.tenant_id}$quality" as String) }
        mc.receivedResponse.code as Integer == SC_OK

        where:
        [a, b] << [
            ["GET", "HEAD"],
            [";q=0.98", ""]
        ].combinations()

        method = a
        quality = b

    }

    @Unroll
    def "should not allow #method calls with permission 'view_product' and quality '#quality'"() {
        given:
        mockValkyrie.device_perm = "view_product"


        when:
        MessageChain mc = deproxy.makeRequest(
            url: "$reposeEndpoint/",
            method: method,
            headers: [
                (AUTH_TOKEN)   : mockValkyrie.valid_auth,
                (TENANT_ID)    : "$otherTenantId;q=0.5,hybrid:${mockValkyrie.tenant_id}$quality",
                (CONTACT_ID)   : mockValkyrie.contact_id,
                (HDR_DEVICE_ID): mockValkyrie.device_id])

        then:
        mc.handlings.size() == 0
        mc.receivedResponse.code as Integer == SC_FORBIDDEN

        where:
        [a, b] << [
            ["PUT", "POST", "DELETE", "PATCH"],
            [";q=0.98", ""]
        ].combinations()

        method = a
        quality = b

    }

    @Unroll
    def "should not allow #method calls with permission '#devicePermission' and quality '#quality'"() {
        given:
        mockValkyrie.device_perm = devicePermission

        when:
        MessageChain mc = deproxy.makeRequest(
            url: "$reposeEndpoint/",
            method: method,
            headers: [
                (AUTH_TOKEN)   : mockValkyrie.valid_auth,
                (TENANT_ID)    : "$otherTenantId$quality,hybrid:${mockValkyrie.tenant_id};q=0.5",
                (CONTACT_ID)   : mockValkyrie.contact_id,
                (HDR_DEVICE_ID): mockValkyrie.device_id])

        then:
        mc.handlings.size() == 0
        mc.receivedResponse.code as Integer == SC_FORBIDDEN

        where:
        [a, b, c] << [
            ["GET", "HEAD", "PUT", "POST", "DELETE", "PATCH"],
            ["edit_product", "admin_product", "account_admin"],
            [";q=0.98", ""]
        ].combinations()

        method = a
        devicePermission = b
        quality = c

    }
}
