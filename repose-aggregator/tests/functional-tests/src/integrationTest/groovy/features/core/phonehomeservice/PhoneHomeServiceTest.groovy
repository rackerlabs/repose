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
package features.core.phonehomeservice

import groovy.json.JsonSlurper
import org.junit.experimental.categories.Category
import org.openrepose.framework.test.ReposeValveTest
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain
import org.rackspace.deproxy.Response
import scaffold.category.Core

/**
 * Created by jennyvo on 8/25/15.
 *  As Repose Product, I want better insight into how people are using Repose,
 *  Who and How many reposes are out there, How they are performing,
 *  so that I have accurate/up to date trackable data of who is using Repose,
 *  how they are using us, how well we are performing, and what version is in use, etc.
 *  Verify events will come on Repose start up and with system model changes
 *  Who is the customer - by name
 *  What is the Repose Version #
 *  What is the Repose System Model (filter chain)
 */
@Category(Core)
class PhoneHomeServiceTest extends ReposeValveTest {
    def static phonehomeEndpoint

    def setupSpec() {
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort, 'origin service')
        phonehomeEndpoint = deproxy.addEndpoint(properties.phonehomePort, 'phone home service')

        reposeLogSearch.cleanLog()

        // empty phone-home.log
        def logpath = logFile.substring(0, logFile.indexOf("logs"))
        reposeLogSearch.setLogFileLocation(logpath + "logs/phone-home.log")
        reposeLogSearch.cleanLog()

    }

    def "Verify Phone home service when start repose"() {
        given:
        def params = properties.getDefaultTemplateParams()
        repose.configurationProvider.cleanConfigDirectory()
        repose.configurationProvider.applyConfigs("common", params);
        repose.configurationProvider.applyConfigs("features/core/phonehomeservice/common", params);
        repose.configurationProvider.applyConfigs("features/core/phonehomeservice/nofilter", params);

        repose.start()

        // repose start up with no filter
        def logpath = logFile.substring(0, logFile.indexOf("logs"))
        reposeLogSearch.setLogFileLocation(logpath + "logs/repose.log")

        when: "send request"
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint, method: 'GET')

        then: "request will pass with simple config"
        mc.receivedResponse.code == "200"
        reposeLogSearch.searchByString("PhoneHomeService - Registering system model listener")
        reposeLogSearch.searchByString("PhoneHomeService - Sending usage data update to data collection service")
        reposeLogSearch.searchByString("PhoneHomeService - Could not send an update to the collection service").size() == 0
    }

    def "Start Repose with some filters verify phone home service log"() {
        setup: "repose is config with Phone Home Service log"
        def logpath = logFile.substring(0, logFile.indexOf("logs"))
        reposeLogSearch.setLogFileLocation(logpath + "logs/phone-home.log")

        def headers = ['content-length': 0]
        phonehomeEndpoint.defaultHandler = { return new Response(400, "", headers) }

        def params = properties.getDefaultTemplateParams()
        repose.configurationProvider.applyConfigs("common", params);
        repose.configurationProvider.applyConfigs("features/core/phonehomeservice/common", params);
        repose.configurationProvider.applyConfigs("features/core/phonehomeservice/somefilters", params);
        repose.start()

        when: "repose update system moder"
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint, method: 'GET')
        def log = reposeLogSearch.searchByString("serviceId")
        def line = getLog(log, "repose-test-service2")

        then: "log will contains these info"
        reposeLogSearch.searchByString("serviceId").size() != 0
        line != null
        line.contactEmail == "repose.core@rackspace.com"
        line.reposeVersion == properties.reposeVersion
        line.filters[0] == "rate-limiting"
        line.services[0] == "dist-datastore"

        // REP-2733 PhoneHomeService Report Java Runtime Environment Version and More
        line.createdAt =~ "[0-9]{4}-[0-9]{2}-[0-9]{2}T[0-9]{2}:[0-9]{2}:[0-9]{2}.[0-9]{3}Z"
        line.createdAtMillis =~ "[0-9]{13}"
        line.jreVersion =~ ".*" //System.getProperty("java.version")
        line.jvmName =~ ".*"//System.getProperty("java.vm.name")
        line.serviceId == "repose-test-service2"
    }

    def cleanup() {
        repose?.stop()
    }

    def getLog(def log, String serviceid) {
        def slurper = new JsonSlurper()
        def logline = null
        for (int i = 0; i < log.size(); i++) {
            def jsonlog = slurper.parseText(log.get(i))
            if (jsonlog.serviceId == serviceid)
                logline = jsonlog
        }
        return logline
    }
}
