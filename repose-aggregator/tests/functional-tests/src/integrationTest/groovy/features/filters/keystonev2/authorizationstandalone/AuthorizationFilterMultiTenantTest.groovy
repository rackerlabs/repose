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
package features.filters.keystonev2.authorizationstandalone

import org.junit.experimental.categories.Category
import org.openrepose.framework.test.ReposeValveTest
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain
import scaffold.category.Filters

import static javax.servlet.http.HttpServletResponse.*
import static org.openrepose.commons.utils.string.Base64Helper.base64DecodeUtf8
import static org.openrepose.commons.utils.string.Base64Helper.base64EncodeUtf8

@Category(Filters)
class AuthorizationFilterMultiTenantTest extends ReposeValveTest {
    def static originEndpoint
    def static random = new Random()
    def static tenantId
    def static xCatalog = base64EncodeUtf8(
        """{ "endpoints": [ { "publicURL":"https://service.example.com", "region":"ORD", "name":"OpenStackCompute", "type":"compute" } ] }""")

    def static tenantIdGenerator = new Iterator<Integer>() {
        def seed = Math.abs(random.nextInt())

        @Override
        boolean hasNext() {
            true
        }

        @Override
        Integer next() {
            this.seed++
        }
    }

    def setupSpec() {
        deproxy = new Deproxy()

        def params = properties.defaultTemplateParams
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/filters/keystonev2/authorizationstandalone/common", params)
        repose.start()

        originEndpoint = deproxy.addEndpoint(properties.targetPort, "origin service")
    }

    def setup() {
        reposeLogSearch.cleanLog()
        tenantId = ++tenantIdGenerator
    }

    def "User with Pre-Authorized Role should receive a 200 OK response"() {
        given: "a configured tenant"
        def mapRoles = """{"tenant1":["role1","role2","role3"],"$tenantId":["role3"],"repose/domain/roles":["role4"],"tenantPreAuth":["racker"]}"""

        when: "User sends a request through repose"
        MessageChain mc = deproxy.makeRequest(
            url: """$reposeEndpoint/ss""",
            method: "GET",
            headers: [
                "X-Catalog"  : xCatalog,
                "X-Map-Roles": base64EncodeUtf8(mapRoles),
                "X-Tenant-Id": """$tenantId;q=1,tenant1;q=0.5""",
                "X-Roles"    : "role1,role2,role3,role4,racker"])

        then: "User should receive a 200 response"
        mc.receivedResponse.code as Integer == SC_OK

        and: "It should have made it to the origin service"
        mc.handlings.size() == 1

        and: "Tenant ID header should be removed."
        def xTenantIdHdrs = mc.handlings[0].request.headers.findAll("x-tenant-id")
        xTenantIdHdrs.size() == 0

        and: "Roles should not be reduced."
        def rolesRequestHeaders = mc.handlings[0].request.getHeaders().findAll("X-Roles").join(",").split(",")
        rolesRequestHeaders as Set == ["role1", "role2", "role3", "role4", "racker"] as Set
        def mapRolesRequestHeaders = mc.handlings[0].request.getHeaders().findAll("X-Map-Roles")
        mapRolesRequestHeaders.size() == 1
        base64DecodeUtf8(mapRolesRequestHeaders[0]) == mapRoles
    }

    def "User with Multiple Tenants and URI extraction should receive a 200 OK response"() {
        when: "User sends a request through repose"
        MessageChain mc = deproxy.makeRequest(
            url: """$reposeEndpoint/extract/$tenantId/ss""",
            method: "GET",
            headers: [
                "X-Catalog"  : xCatalog,
                "X-Map-Roles": base64EncodeUtf8("""{"tenant1":["role1","role2","role3"],"$tenantId":["role3"],"repose/domain/roles":["role4"]}"""),
                "X-Tenant-Id": "tenant1;q=0.5",
                "X-Roles"    : "role1,role2,role3,role4"])

        then: "User should receive a 200 response"
        mc.receivedResponse.code as Integer == SC_OK

        and: "It should have made it to the origin service"
        mc.handlings.size() == 1

        and: "Tenant should be extracted from URI and replace existing."
        def xTenantIdHdrs = mc.handlings[0].request.headers.findAll("x-tenant-id")
        xTenantIdHdrs.size() == 1
        xTenantIdHdrs[0] == """$tenantId;q=0.8"""

        and: "Roles should be reduced to only the tenant's and the global"
        def rolesRequestHeaders = mc.handlings[0].request.getHeaders().findAll("X-Roles").join(",").split(",")
        rolesRequestHeaders as Set == ["role3", "role4"] as Set
        def mapRolesRequestHeaders = mc.handlings[0].request.getHeaders().findAll("X-Map-Roles")
        mapRolesRequestHeaders.size() == 1
        def mapRolesDecode = base64DecodeUtf8(mapRolesRequestHeaders[0]).split("[\\[\\]{}:,\"]") as Set
        if (mapRolesDecode.contains("")) {
            mapRolesDecode.remove("")
        }
        mapRolesDecode == [tenantId as String, "role3", "repose/domain/roles", "role4"] as Set
    }

    def "User with Multiple Tenants and invalid URI extraction should receive a 401 UNAUTHORIZED response"() {
        when: "User sends a request through repose"
        MessageChain mc = deproxy.makeRequest(
            url: """$reposeEndpoint/extract/${++tenantIdGenerator}/ss""",
            method: "GET",
            headers: [
                "X-Catalog"  : xCatalog,
                "X-Map-Roles": base64EncodeUtf8("""{"tenant1":["role1","role2","role3"],"$tenantId":["role3"],"repose/domain/roles":["role4"]}"""),
                "X-Tenant-Id": """$tenantId;q=1,tenant1;q=0.5""",
                "X-Roles"    : "role1,role2,role3,role4"])

        then: "User should receive a 401 response"
        mc.receivedResponse.code as Integer == SC_UNAUTHORIZED

        and: "It should not have made it to the origin service"
        mc.handlings.size() == 0

        and: "The reason should have been logged"
        def foundLogs = reposeLogSearch.searchByString("A tenant from the configured header does not match any of the user's tenants")
        foundLogs.size() == 1
    }

    def "User with Multiple Tenants and header extraction should receive a 200 OK response"() {
        when: "User sends a request through repose"
        MessageChain mc = deproxy.makeRequest(
            url: """$reposeEndpoint/ss""",
            method: "GET",
            headers: [
                "X-Catalog"        : xCatalog,
                "X-Map-Roles"      : base64EncodeUtf8("""{"tenant1":["role1","role2","role3"],"$tenantId":["role3"],"repose/domain/roles":["role4"]}"""),
                "X-Tenant-Id"      : "tenant1;q=0.5",
                "X-Roles"          : "role1,role2,role3,role4",
                "X-Expected-Tenant": tenantId])

        then: "User should receive a 200 response"
        mc.receivedResponse.code as Integer == SC_OK

        and: "It should have made it to the origin service"
        mc.handlings.size() == 1

        and: "Tenant should be extracted from header and replace existing."
        def xTenantIdHdrs = mc.handlings[0].request.headers.findAll("x-tenant-id")
        xTenantIdHdrs.size() == 1
        xTenantIdHdrs[0] == """$tenantId;q=0.8"""

        and: "Roles should be reduced to only the tenant's and the global"
        def rolesRequestHeaders = mc.handlings[0].request.getHeaders().findAll("X-Roles").join(",").split(",")
        rolesRequestHeaders as Set == ["role3", "role4"] as Set
    }

    def "User with Multiple Tenants and invalid header extraction should receive a 401 UNAUTHORIZED response"() {
        when: "User sends a request through repose"
        MessageChain mc = deproxy.makeRequest(
            url: """$reposeEndpoint/ss""",
            method: "GET",
            headers: [
                "X-Catalog"        : xCatalog,
                "X-Map-Roles"      : base64EncodeUtf8("""{"tenant1":["role1","role2","role3"],"$tenantId":["role3"],"repose/domain/roles":["role4"]}"""),
                "X-Tenant-Id"      : """$tenantId;q=1,tenant1;q=0.5""",
                "X-Roles"          : "role1,role2,role3,role4",
                "X-Expected-Tenant": ++tenantIdGenerator])

        then: "User should receive a 401 response"
        mc.receivedResponse.code as Integer == SC_UNAUTHORIZED

        and: "It should not have made it to the origin service"
        mc.handlings.size() == 0

        and: "The reason should have been logged"
        def foundLogs = reposeLogSearch.searchByString("A tenant from the configured header does not match any of the user's tenants")
        foundLogs.size() == 1
    }

    def "User with Multiple Tenants and no header or URI extraction should receive a 401 UNAUTHORIZED response"() {
        when: "User sends a request through repose"
        MessageChain mc = deproxy.makeRequest(
            url: """$reposeEndpoint/ss""",
            method: "GET",
            headers: [
                "X-Catalog"  : xCatalog,
                "X-Map-Roles": base64EncodeUtf8("""{"tenant1":["role1","role2","role3"],"$tenantId":["role3"],"repose/domain/roles":["role4"]}"""),
                "X-Tenant-Id": """$tenantId;q=1,tenant1;q=0.5""",
                "X-Roles"    : "role1,role2,role3,role4"])

        then: "User should receive a 401 response"
        mc.receivedResponse.code as Integer == SC_UNAUTHORIZED

        and: "It should not have made it to the origin service"
        mc.handlings.size() == 0

        and: "The reason should have been logged"
        def foundLogs = reposeLogSearch.searchByString("Could not parse tenant from the configured header")
        foundLogs.size() == 1
    }

    def "User without the required catalog endpoint should receive a 403 FORBIDDEN response"() {
        when: "User sends a request through repose"
        MessageChain mc = deproxy.makeRequest(
            url: """$reposeEndpoint/ss""",
            method: "GET",
            headers: [
                "X-Catalog"        : base64EncodeUtf8("""{"endpoints":[{"publicURL":"https://service.example.com","region":"BAD","name":"OpenStackCompute","type":"compute"}]}"""),
                "X-Map-Roles"      : base64EncodeUtf8("""{"tenant1":["role1","role2","role3"],"$tenantId":["role3"],"repose/domain/roles":["role4"]}"""),
                "X-Tenant-Id"      : "tenant1;q=0.5",
                "X-Roles"          : "role1,role2,role3,role4",
                "X-Expected-Tenant": tenantId])

        then: "User should receive a 403 response"
        mc.receivedResponse.code as Integer == SC_FORBIDDEN

        and: "It should not have made it to the origin service"
        mc.handlings.size() == 0

        and: "The reason should have been logged"
        def foundLogs = reposeLogSearch.searchByString("User did not have the required endpoint")
        foundLogs.size() == 1
    }

    def "Request without X-Catalog header should receive a 500 INTERNAL SERVER ERROR response"() {
        when: "User sends a request through repose"
        MessageChain mc = deproxy.makeRequest(
            url: """$reposeEndpoint/ss""",
            method: "GET",
            headers: [
                "X-Map-Roles"      : base64EncodeUtf8("""{"tenant1":["role1","role2","role3"],"$tenantId":["role3"],"repose/domain/roles":["role4"]}"""),
                "X-Tenant-Id"      : "tenant1;q=0.5",
                "X-Roles"          : "role1,role2,role3,role4",
                "X-Expected-Tenant": tenantId])

        then: "User should receive a 500 response"
        mc.receivedResponse.code as Integer == SC_INTERNAL_SERVER_ERROR

        and: "It should not have made it to the origin service"
        mc.handlings.size() == 0

        and: "The reason should have been logged"
        def foundLogs = reposeLogSearch.searchByString("x-catalog header does not exist")
        foundLogs.size() == 1
    }

    def "Request with an invalid X-Catalog header should receive a 500 INTERNAL SERVER ERROR response"() {
        when: "User sends a request through repose"
        MessageChain mc = deproxy.makeRequest(
            url: """$reposeEndpoint/ss""",
            method: "GET",
            headers: [
                "X-Catalog"        : "Totally Bogus",
                "X-Map-Roles"      : base64EncodeUtf8("""{"tenant1":["role1","role2","role3"],"$tenantId":["role3"],"repose/domain/roles":["role4"]}"""),
                "X-Tenant-Id"      : "tenant1;q=0.5",
                "X-Roles"          : "role1,role2,role3,role4",
                "X-Expected-Tenant": tenantId])

        then: "User should receive a 500 response"
        mc.receivedResponse.code as Integer == SC_INTERNAL_SERVER_ERROR

        and: "It should not have made it to the origin service"
        mc.handlings.size() == 0

        and: "The reason should have been logged"
        def foundLogs = reposeLogSearch.searchByString("x-catalog header value is not a valid endpoints representation")
        foundLogs.size() == 1
    }

    def "Request without X-Map-Roles header should receive a 500 INTERNAL SERVER ERROR response"() {
        when: "User sends a request through repose"
        MessageChain mc = deproxy.makeRequest(
            url: """$reposeEndpoint/ss""",
            method: "GET",
            headers: [
                "X-Catalog"        : xCatalog,
                "X-Tenant-Id"      : "tenant1;q=0.5",
                "X-Roles"          : "role1,role2,role3,role4",
                "X-Expected-Tenant": tenantId])

        then: "User should receive a 500 response"
        mc.receivedResponse.code as Integer == SC_INTERNAL_SERVER_ERROR

        and: "It should not have made it to the origin service"
        mc.handlings.size() == 0

        and: "The reason should have been logged"
        def foundLogs = reposeLogSearch.searchByString("X-Map-Roles header does not exist")
        foundLogs.size() == 1
    }

    def "Request with an invalid X-Map-Roles header should receive a 500 INTERNAL SERVER ERROR response"() {
        when: "User sends a request through repose"
        MessageChain mc = deproxy.makeRequest(
            url: """$reposeEndpoint/ss""",
            method: "GET",
            headers: [
                "X-Catalog"        : xCatalog,
                "X-Map-Roles"      : "Totally Bogus",
                "X-Tenant-Id"      : "tenant1;q=0.5",
                "X-Roles"          : "role1,role2,role3,role4",
                "X-Expected-Tenant": tenantId])

        then: "User should receive a 500 response"
        mc.receivedResponse.code as Integer == SC_INTERNAL_SERVER_ERROR

        and: "It should not have made it to the origin service"
        mc.handlings.size() == 0

        and: "The reason should have been logged"
        def foundLogs = reposeLogSearch.searchByString("X-Map-Roles header value is not a valid tenant-to-roles map representation")
        foundLogs.size() == 1
    }
}
