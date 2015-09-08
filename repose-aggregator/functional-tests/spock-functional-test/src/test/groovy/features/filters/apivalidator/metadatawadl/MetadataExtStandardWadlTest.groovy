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
package features.filters.apivalidator.metadatawadl

import framework.ReposeValveTest
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain
import spock.lang.Unroll

/**
 * Created by jennyvo on 9/8/15.
 */
class MetadataExtStandardWadlTest extends ReposeValveTest {

    def setupSpec() {
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort)

        def params = properties.getDefaultTemplateParams()
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/filters/apivalidator/common", params)
        repose.configurationProvider.applyConfigs("features/filters/apivalidator/metadataextension", params)
        repose.start()

        repose.waitForNon500FromUrl(reposeEndpoint)
    }

    def cleanupSpec() {
        if (repose)
            repose.stop()
        if (deproxy)
            deproxy.shutdown()
    }

    @Unroll ("Roles: #headers, path: #path and respcode: #responseCode")
    def "Test standard meta role" () {
        given:
        MessageChain mc

        when:
        mc = deproxy.makeRequest(url: reposeEndpoint + "/" + path, headers: headers)

        then:
        mc.getReceivedResponse().getCode().equals(responseCode)

        where:
        path            | headers                                      | responseCode
        "standard"      | ["x-roles": "repose_test, admin"]            | "200"
        "standard"      | ["x-roles": "repose_test, billing:view"]     | "200"
        "standard"      | ["x-roles": "repose_test, service:view"]     | "200"
        "custom"        | ["x-roles": "repose_test, admin"]            | "200"
        "custom"        | ["x-roles": "repose_test, superadmin"]       | "200"
        "custom"        | ["x-roles": "repose_test, customer:role"]    | "200"
        "custom"        | ["x-roles": "repose_test, service:audit"]    | "200"
    }
}
