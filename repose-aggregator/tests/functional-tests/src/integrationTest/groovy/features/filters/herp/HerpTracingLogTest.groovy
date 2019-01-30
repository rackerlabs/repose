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
package features.filters.herp

import groovy.json.JsonSlurper
import org.junit.experimental.categories.Category
import org.openrepose.commons.utils.logging.TracingHeaderHelper
import org.openrepose.framework.test.ReposeValveTest
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain
import scaffold.category.Filters

/**
 * Created by jennyvo on 5/22/15.
 */
@Category(Filters)
class HerpTracingLogTest extends ReposeValveTest {

    def setupSpec() {
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort)

        def params = properties.defaultTemplateParams
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs('features/filters/herp', params)
        repose.configurationProvider.applyConfigs('features/filters/herp/tracinglog', params)
        repose.start()
    }

    def "simple simple test"() {
        setup:
        List listattr = ["GUI", "ServiceCode", "Region", "DataCenter", "Timestamp", "Request", "Method", "URL", "Parameters",
                         "UserName", "ImpersonatorName", "ProjectID", "Role", "UserAgent", "Response", "Code", "Message"]
        reposeLogSearch.cleanLog()
        MessageChain messageChain

        when:
        messageChain = deproxy.makeRequest(url: reposeEndpoint, method: "GET", headers: ['Accept': 'application/xml'])
        String logLine = reposeLogSearch.searchByString("INFO  highly-efficient-record-processor")
        String jsonpart = logLine.substring(logLine.indexOf("{"))
        def slurper = new JsonSlurper()
        def result = slurper.parseText(jsonpart)
        def requestid = TracingHeaderHelper.getTraceGuid(messageChain.receivedResponse.headers.getFirstValue("x-trans-id"))
        println requestid

        then:
        messageChain.receivedResponse.code == "200"
        messageChain.receivedResponse.headers.contains("x-trans-id")
        reposeLogSearch.searchByString("GUID:$requestid -.*INFO  highly-efficient-record-processor").size() > 0
        checkAttribute(jsonpart, listattr)
        result.ServiceCode == "repose"
        result.Region == "USA"
        result.DataCenter == "DFW"
        result.Request.Method == "GET"
        result.Response.Code == 200
        result.Response.Message == "OK"
    }

    // Check all required attributes in the log
    private boolean checkAttribute(String jsonpart, List listattr) {
        def slurper = new JsonSlurper()
        def result = slurper.parseText(jsonpart)
        boolean check = true
        for (attr in listattr) {
            if (!jsonpart.contains(attr)) {
                check = false
                break
            }
        }
        return check
    }
}
