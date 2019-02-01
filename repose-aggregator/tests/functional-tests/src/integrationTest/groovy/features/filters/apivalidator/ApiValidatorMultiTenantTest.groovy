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
package features.filters.apivalidator

import org.junit.experimental.categories.Category
import org.openrepose.framework.test.ReposeValveTest
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain
import scaffold.category.XmlParsing
import spock.lang.Unroll

import static javax.servlet.http.HttpServletResponse.SC_FORBIDDEN
import static javax.servlet.http.HttpServletResponse.SC_OK
import static org.openrepose.commons.utils.http.OpenStackServiceHeader.*
import static org.openrepose.commons.utils.http.PowerApiHeader.RELEVANT_ROLES
import static org.openrepose.commons.utils.string.Base64Helper.base64EncodeUtf8

@Category(XmlParsing)
class ApiValidatorMultiTenantTest extends ReposeValveTest {

    static def HDR_OTHER = "X-Other"
    static def VAL_OTHER = "other"
    static def HDR_CASED_ID = "X-Cased-Id"

    static def mapHeaderValue = base64EncodeUtf8("""{
      |   "1" : ["a:admin","foo","bar"],
      |   "2" : ["a:creator", "foo", "a:observer"],
      |   "3" : ["a:updater", "bar", "biz", "a:creator"],
      |   "4" : ["a:observer"],
      |   "5" : ["a:admin", "bar", "biz", "a:creator"],
      |   "6" : ["biz", "baz"],
      |   "7" : ["role with spaces", "biz"],
      |   "8" : ["a:patcher"]
      |}
      |""".stripMargin())

    static def standardHeaders = [
        (HDR_OTHER)       : VAL_OTHER,
        (ROLES)           : "foo,bar",
        (TENANT_ROLES_MAP): mapHeaderValue
    ]

    static def observerRequests = [
        ["GET", "/v1/resource"]
    ]

    static def creatorRequests = [
        ["POST", "/v1/resource"],
        ["POST", "/v1/resource/other"]
    ]
    static def updaterRequests = [
        ["PUT", "/v1/resource"],
        ["PUT", "/v1/resource/other"]
    ]
    static def adminOnlyRequests = [
        ["DELETE", "/v1/resource"],
        ["DELETE", "/v1/resource/other"]
    ]

    def setupSpec() {
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort)

        def params = properties.getDefaultTemplateParams()
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/filters/apivalidator/common", params)
        repose.configurationProvider.applyConfigs("features/filters/apivalidator/multitenant", params)
        repose.start()

        repose.waitForNon500FromUrl(reposeEndpoint)
    }

    ////////////////////////////////////////////////////////////////////////////////
    // NOTE:
    // The case of the rax:roles tenant must match the case of the param name.
    // The case of the actual header does not matter.
    ////////////////////////////////////////////////////////////////////////////////

    @Unroll
    def "Should succeed on GET /v1/resource/other when appropriate headers are set (#tenants)"() {
        when:
        MessageChain mc = deproxy.makeRequest(
            url: "$reposeEndpoint/v1/resource/other",
            method: "GET",
            headers: ([(TENANT_ID.toUpperCase()): tenants] + standardHeaders))

        then:
        mc.getReceivedResponse().getCode() as Integer == SC_OK
        mc.handlings.size() == 1
        List<String> relevantRolesValues = mc.handlings[0].request.headers.findAll(RELEVANT_ROLES)
        relevantRolesValues.containsAll(["foo", "bar"])

        where:
        tenants << ["1,5", "4", "4,2", "1,2,3", "5,2", "1,3", "5,3", "1", "1,5,7", "1,5,6"]
    }

    @Unroll
    def "Should succeed on #method #url when appropriate headers are set (1, 5)"() {
        when:
        MessageChain mc = deproxy.makeRequest(
            url: "$reposeEndpoint$url",
            method: method,
            headers: ([(TENANT_ID.toLowerCase()): "1,5"] + standardHeaders))

        then:
        mc.getReceivedResponse().getCode() as Integer == SC_OK
        mc.handlings.size() == 1
        List<String> relevantRolesValues = mc.handlings[0].request.headers.findAll(RELEVANT_ROLES)
        relevantRolesValues.contains("a:admin/{X-Tenant-Id}")

        where:
        [method, url] << observerRequests + creatorRequests + updaterRequests + adminOnlyRequests
    }

    def "Should succeed on GET /v1/resource/other when appropriate headers are set (1,5)"() {
        when:
        MessageChain mc = deproxy.makeRequest(
            url: "$reposeEndpoint/v1/resource/other",
            method: "GET",
            headers: ([(TENANT_ID): "1,5"] + standardHeaders
                - [(ROLES): "foo,bar"]))

        then:
        mc.getReceivedResponse().getCode() as Integer == SC_OK
        mc.handlings.size() == 1
        List<String> relevantRolesValues = mc.handlings[0].request.headers.findAll(RELEVANT_ROLES)
        relevantRolesValues.isEmpty()
    }

    @Unroll
    def "Should fail on #method #url when appropriate headers are set but there's no tenant access (#tenants)"() {
        when:
        MessageChain mc = deproxy.makeRequest(
            url: "$reposeEndpoint$url",
            method: method,
            headers: ([(TENANT_ID): tenants] + standardHeaders))

        then:
        mc.getReceivedResponse().getCode() as Integer == SC_FORBIDDEN
        mc.handlings.size() == 0

        where:
        [a, b] << [
            observerRequests + creatorRequests + updaterRequests + adminOnlyRequests,
            ["1,5,7", "1,5,6"]
        ].combinations()

        method = a[0]
        url = a[1]
        tenants = b
    }

    @Unroll
    def "Should fail on #method #url when appropriate headers are set but there's no tenant access (#tenants) too"() {
        when:
        MessageChain mc = deproxy.makeRequest(
            url: "$reposeEndpoint$url",
            method: method,
            headers: ([(TENANT_ID): "1,5"] + standardHeaders
                - [(ROLES): "foo,bar", (TENANT_ROLES_MAP): mapHeaderValue]))

        then:
        mc.getReceivedResponse().getCode() as Integer == SC_FORBIDDEN
        mc.handlings.size() == 0

        where:
        [a, b] << [
            observerRequests + creatorRequests + updaterRequests + adminOnlyRequests,
            ["1,5"]
        ].combinations()

        method = a[0]
        url = a[1]
        tenants = b[0]
    }

    @Unroll
    def "Should succeed on GET /v1/resource when appropriate headers are set (#tenants)"() {
        when:
        MessageChain mc = deproxy.makeRequest(
            url: "$reposeEndpoint/v1/resource",
            method: "GET",
            headers: ([(TENANT_ID): tenants] + standardHeaders))

        then:
        mc.getReceivedResponse().getCode() as Integer == SC_OK
        mc.handlings.size() == 1
        List<String> relevantRolesValues = mc.handlings[0].request.headers.findAll(RELEVANT_ROLES)
        relevantRolesValues.contains("a:observer/{X-Tenant-Id}")

        where:
        tenants << ["4", "4,2"]
    }

    @Unroll
    def "Should fail on #method #url when appropriate headers are set but it's an observer only tenant (#tenants)"() {
        when:
        MessageChain mc = deproxy.makeRequest(
            url: "$reposeEndpoint$url",
            method: method,
            headers: ([(TENANT_ID): tenants] + standardHeaders))

        then:
        mc.getReceivedResponse().getCode() as Integer == SC_FORBIDDEN
        mc.handlings.size() == 0

        where:
        [a, b] << [
            creatorRequests + updaterRequests + adminOnlyRequests,
            ["4", "4,2"]
        ].combinations()

        method = a[0]
        url = a[1]
        tenants = b
    }

    @Unroll
    def "Should succeed on #method #url when appropriate headers are set (#tenants)"() {
        when:
        MessageChain mc = deproxy.makeRequest(
            url: "$reposeEndpoint$url",
            method: method,
            headers: ([(TENANT_ID): tenants] + standardHeaders))

        then:
        mc.getReceivedResponse().getCode() as Integer == SC_OK
        mc.handlings.size() == 1
        List<String> relevantRolesValues = mc.handlings[0].request.headers.findAll(RELEVANT_ROLES)
        relevantRolesValues.containsAll(["a:creator/{X-Tenant-Id}", "a:admin/{X-Tenant-Id}"])

        where:
        [a, b] << [
            creatorRequests,
            ["1,2,3", "5,2"]
        ].combinations()

        method = a[0]
        url = a[1]
        tenants = b
    }

    @Unroll
    def "Should succeed on #method #url when appropriate headers are set (#tenants) two"() {
        when:
        MessageChain mc = deproxy.makeRequest(
            url: "$reposeEndpoint$url",
            method: method,
            headers: ([(TENANT_ID): tenants] + standardHeaders))

        then:
        mc.getReceivedResponse().getCode() as Integer == SC_OK
        mc.handlings.size() == 1
        List<String> relevantRolesValues = mc.handlings[0].request.headers.findAll(RELEVANT_ROLES)
        relevantRolesValues.containsAll(["a:updater/{X-Tenant-Id}", "a:admin/{X-Tenant-Id}"])

        where:
        [a, b] << [
            updaterRequests,
            ["1,3", "5,3"]
        ].combinations()

        method = a[0]
        url = a[1]
        tenants = b
    }

    @Unroll
    def "Should succeed on #method #url when appropriate headers are set (#tenants) too"() {
        when:
        MessageChain mc = deproxy.makeRequest(
            url: "$reposeEndpoint$url",
            method: method,
            headers: ([(TENANT_ID): tenants] + standardHeaders))

        then:
        mc.getReceivedResponse().getCode() as Integer == SC_OK
        mc.handlings.size() == 1
        List<String> relevantRolesValues = mc.handlings[0].request.headers.findAll(RELEVANT_ROLES)
        relevantRolesValues.contains("a:admin/{X-Tenant-Id}")

        where:
        [a, b] << [
            updaterRequests,
            ["1,5"]
        ].combinations()

        method = a[0]
        url = a[1]
        tenants = b
    }

    @Unroll
    def "Should succeed on #method #url when appropriate headers are set (#tenants) also"() {
        when:
        MessageChain mc = deproxy.makeRequest(
            url: "$reposeEndpoint$url",
            method: method,
            headers: ([(TENANT_ID): tenants] + standardHeaders))

        then:
        mc.getReceivedResponse().getCode() as Integer == SC_OK
        mc.handlings.size() == 1
        List<String> relevantRolesValues = mc.handlings[0].request.headers.findAll(RELEVANT_ROLES)
        relevantRolesValues.contains("a:admin/{X-Tenant-Id}")

        where:
        [a, b] << [
            adminOnlyRequests,
            ["1", "1,5"]
        ].combinations()

        method = a[0]
        url = a[1]
        tenants = b
    }

    @Unroll
    def "Should fail on #method #url when appropriate headers are set but there is a tenant role mismatch (#tenants)"() {
        when:
        MessageChain mc = deproxy.makeRequest(
            url: "$reposeEndpoint$url",
            method: method,
            headers: ([(TENANT_ID): tenants] + standardHeaders))

        then:
        mc.getReceivedResponse().getCode() as Integer == SC_FORBIDDEN
        mc.handlings.size() == 0

        where:
        [a, b] << [
            creatorRequests,
            ["4,2", "6,3"]
        ].combinations()

        method = a[0]
        url = a[1]
        tenants = b
    }

    @Unroll
    def "Should fail on #method #url when appropriate headers are set but there is a tenant role mismatch (#tenants) two"() {
        when:
        MessageChain mc = deproxy.makeRequest(
            url: "$reposeEndpoint$url",
            method: method,
            headers: ([(TENANT_ID): tenants] + standardHeaders))

        then:
        mc.getReceivedResponse().getCode() as Integer == SC_FORBIDDEN
        mc.handlings.size() == 0

        where:
        [a, b] << [
            updaterRequests,
            ["4,3", "2,1"]
        ].combinations()

        method = a[0]
        url = a[1]
        tenants = b
    }

    @Unroll
    def "Should fail on #method #url when appropriate headers are set but there is a tenant role mismatch (#tenants) too"() {
        when:
        MessageChain mc = deproxy.makeRequest(
            url: "$reposeEndpoint$url",
            method: method,
            headers: ([(TENANT_ID): tenants] + standardHeaders))

        then:
        mc.getReceivedResponse().getCode() as Integer == SC_FORBIDDEN
        mc.handlings.size() == 0

        where:
        [a, b] << [
            adminOnlyRequests,
            ["4,1", "5,2"]
        ].combinations()

        method = a[0]
        url = a[1]
        tenants = b
    }

    @Unroll
    def "Should succeed on PATCH in /v1/resource/other if a role with a:patcher is specified (#tenants)"() {
        when:
        MessageChain mc = deproxy.makeRequest(
            url: "$reposeEndpoint/v1/resource/other",
            method: "PATCH",
            headers: ([(TENANT_ID): tenants] + standardHeaders))

        then:
        mc.getReceivedResponse().getCode() as Integer == SC_OK
        mc.handlings.size() == 1
        List<String> relevantRolesValues = mc.handlings[0].request.headers.findAll(RELEVANT_ROLES)
        relevantRolesValues.containsAll(relevant)

        where:
        tenants | relevant
        "7"     | ["role with spaces/{X-Tenant-Id}"]
        "7,8"   | ["role with spaces/{X-Tenant-Id}", "a:patcher/{X-Tenant-Id}"]
    }

    def "Should fail on PATCH in /v1/resource/other if a tenant without an appropriate role is specified"() {
        when:
        MessageChain mc = deproxy.makeRequest(
            url: "$reposeEndpoint/v1/resource/other",
            method: "PATCH",
            headers: ([(TENANT_ID): "7,8,4"] + standardHeaders))

        then:
        mc.getReceivedResponse().getCode() as Integer == SC_FORBIDDEN
        mc.handlings.size() == 0
    }

    @Unroll
    def "Should succeed on GET in /v2/cased if a role with a:observer is specified (#tenants)"() {
        when:
        MessageChain mc = deproxy.makeRequest(
            url: "$reposeEndpoint/v2/cased",
            method: "GET",
            headers: ([(HDR_CASED_ID): tenants] + standardHeaders))

        then:
        mc.getReceivedResponse().getCode() as Integer == SC_OK
        mc.handlings.size() == 1
        List<String> relevantRolesValues = mc.handlings[0].request.headers.findAll(RELEVANT_ROLES)
        relevantRolesValues.contains("a:observer/{x-cased-id}")

        where:
        tenants << ["2", "4"]
    }

    def "Should succeed on PUT in /v2/cased if a role with a:updater is specified (3)"() {
        when:
        MessageChain mc = deproxy.makeRequest(
            url: "$reposeEndpoint/v2/cased",
            method: "PUT",
            headers: ([(HDR_CASED_ID): "3"] + standardHeaders))

        then:
        mc.getReceivedResponse().getCode() as Integer == SC_OK
        mc.handlings.size() == 1
        List<String> relevantRolesValues = mc.handlings[0].request.headers.findAll(RELEVANT_ROLES)
        relevantRolesValues.contains("a:updater/{X-Cased-Id}")
    }

    @Unroll
    def "Should succeed on POST in /v2/cased if a role with a:creator is specified (#tenants)"() {
        when:
        MessageChain mc = deproxy.makeRequest(
            url: "$reposeEndpoint/v2/cased",
            method: "POST",
            headers: ([(HDR_CASED_ID): tenants] + standardHeaders))

        then:
        mc.getReceivedResponse().getCode() as Integer == SC_OK
        mc.handlings.size() == 1
        List<String> relevantRolesValues = mc.handlings[0].request.headers.findAll(RELEVANT_ROLES)
        relevantRolesValues.contains("a:creator/{x-cASED-iD}")

        where:
        tenants << ["2", "3", "5"]
    }

    @Unroll
    def "Should succeed on DELETE in /v2/cased if a role with a:admin is specified (#tenants)"() {
        when:
        MessageChain mc = deproxy.makeRequest(
            url: "$reposeEndpoint/v2/cased",
            method: "DELETE",
            headers: ([(HDR_CASED_ID): tenants] + standardHeaders))

        then:
        mc.getReceivedResponse().getCode() as Integer == SC_OK
        mc.handlings.size() == 1
        List<String> relevantRolesValues = mc.handlings[0].request.headers.findAll(RELEVANT_ROLES)
        relevantRolesValues.contains("a:admin/{X-CASED-ID}")

        where:
        tenants << ["1", "5"]
    }
}
