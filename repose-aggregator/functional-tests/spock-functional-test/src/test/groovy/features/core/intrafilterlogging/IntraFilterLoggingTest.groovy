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
package features.core.intrafilterlogging

import framework.ReposeValveTest
import framework.mocks.MockIdentityService
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain

/**
 * Created by jennyvo on 7/16/15.
 * Verify load all filters from filter chain
 *  when Repose start no longer log 'null' as part of currentFilter description
 */
class IntraFilterLoggingTest extends ReposeValveTest {
    def static originEndpoint
    def static identityEndpoint
    def static MockIdentityService fakeIdentityService

    def setupSpec() {
        deproxy = new Deproxy()
        reposeLogSearch.cleanLog()

        def params = properties.getDefaultTemplateParams()
        repose.configurationProvider.cleanConfigDirectory()
        repose.configurationProvider.applyConfigs("common", params);
        repose.configurationProvider.applyConfigs("features/core/intrafilterlogging", params);

        repose.start()

        originEndpoint = deproxy.addEndpoint(properties.targetPort, 'origin service')
        fakeIdentityService = new MockIdentityService(properties.identityPort, properties.targetPort)
        identityEndpoint = deproxy.addEndpoint(properties.identityPort, 'identity service', null, fakeIdentityService.handler)
        fakeIdentityService.checkTokenValid = true

        repose.waitForNon500FromUrl(reposeEndpoint)
    }

    def cleanupSpec() {
        if (deproxy) {
            deproxy.shutdown()
        }

        if (repose) {
            repose.stop()
        }
    }

    def "Verify intra filter log for current filter no longer have 'null' in the description" () {
        given:
        // repose start up
        def headers = ["x-roles":"raxRolesDisabled"]
        when: "send request without credential"
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint + "/test", method: 'GET', headers: headers)

        then: "simply pass it on down the filter chain"
        //mc.receivedResponse.code == SC_OK.toString()
        //mc.handlings.size() == 1
        mc.orphanedHandlings.size() == 0
        reposeLogSearch.searchByString("\"currentFilter\":\"rackspace-identity-basic-auth\"").size() > 0
        reposeLogSearch.searchByString("\"currentFilter\":\"herp\"").size() > 0
        reposeLogSearch.searchByString("\"currentFilter\":\"add-header\"").size() > 0
        reposeLogSearch.searchByString("\"currentFilter\":\"keystone-v2\"").size() > 0
        reposeLogSearch.searchByString("\"currentFilter\":\"openstack-identity-v3\"").size() > 0
        reposeLogSearch.searchByString("\"currentFilter\":\"header-identity\"").size() > 0
        reposeLogSearch.searchByString("\"currentFilter\":\"ip-user\"").size() > 0
        reposeLogSearch.searchByString("\"currentFilter\":\"header-normalization\"").size() > 0
        reposeLogSearch.searchByString("\"currentFilter\":\"header-translation\"").size() > 0
        reposeLogSearch.searchByString("\"currentFilter\":\"header-id-mapping\"").size() > 0
        reposeLogSearch.searchByString("\"currentFilter\":\"content-type-stripper\"").size() > 0
        reposeLogSearch.searchByString("\"currentFilter\":\"merge-header\"").size() > 0
        reposeLogSearch.searchByString("\"currentFilter\":\"slf4j-http-logging\"").size() > 0
        reposeLogSearch.searchByString("\"currentFilter\":\"uri-user\"").size() > 0
        reposeLogSearch.searchByString("\"currentFilter\":\"uri-normalization\"").size() > 0
        reposeLogSearch.searchByString("\"currentFilter\":\"uri-stripper\"").size() > 0
        reposeLogSearch.searchByString("\"currentFilter\":\"compression\"").size() > 0
        reposeLogSearch.searchByString("\"currentFilter\":\"rate-limiting\"").size() > 0
        reposeLogSearch.searchByString("\"currentFilter\":\"api-validator\"").size() > 0
        reposeLogSearch.searchByString("\"currentFilter\":\"simple-rbac\"").size() > 0
        reposeLogSearch.searchByString("\"currentFilter\":\"derp\"").size() > 0

        reposeLogSearch.searchByString("null-rackspace-identity-basic-auth").size() == 0
        reposeLogSearch.searchByString("null-herp").size() == 0
        reposeLogSearch.searchByString("null-add-header").size() == 0
        reposeLogSearch.searchByString("null-keystone-v2").size() == 0
        reposeLogSearch.searchByString("null-openstack-identity-v3").size() == 0
        reposeLogSearch.searchByString("null-header-identity").size() == 0
        reposeLogSearch.searchByString("null-ip-user").size() == 0
        reposeLogSearch.searchByString("null-header-normalization").size() == 0
        reposeLogSearch.searchByString("null-header-translation").size() == 0
        reposeLogSearch.searchByString("null-header-id-mapping").size() == 0
        reposeLogSearch.searchByString("null-content-type-stripper").size() == 0
        reposeLogSearch.searchByString("null-merge-header").size() == 0
        reposeLogSearch.searchByString("null-slf4j-http-logging").size() == 0
        reposeLogSearch.searchByString("null-uri-user").size() == 0
        reposeLogSearch.searchByString("null-uri-normalization").size() == 0
        reposeLogSearch.searchByString("null-uri-stripper").size() == 0
        reposeLogSearch.searchByString("null-compression").size() == 0
        reposeLogSearch.searchByString("null-rate-limiting").size() == 0
        reposeLogSearch.searchByString("null-api-validator").size() == 0
        reposeLogSearch.searchByString("null-simple-rbac").size() == 0
        reposeLogSearch.searchByString("null-derp").size() == 0
    }
}
