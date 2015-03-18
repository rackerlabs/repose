/*
 *  Copyright (c) 2015 Rackspace US, Inc.
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package features.filters.decomissionedfilter

import framework.ReposeValveTest
import org.rackspace.deproxy.Deproxy

/**
 * Created by jennyvo on 6/9/14.
 */
class DecommissionHttpLoggingTest extends ReposeValveTest{

    def "Test decommission http-log filter"() {
        given:
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort)
        reposeLogSearch.cleanLog()

        def params = properties.defaultTemplateParams
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/core/powerfilter/badfilter" , params)

        when:
        repose.start([waitOnJmxAfterStarting: false])
        waitUntilReadyToServiceRequests("503")

        then:
        reposeLogSearch.searchByString("NullPointerException").size() == 0
        reposeLogSearch.searchByString("none of the loaded artifacts supply a filter named http-logging").size() > 0
    }
    def cleanup() {
        if (deproxy)
            deproxy.shutdown()

        if (repose)
            repose.stop([throwExceptionOnKill: false])

    }
}
