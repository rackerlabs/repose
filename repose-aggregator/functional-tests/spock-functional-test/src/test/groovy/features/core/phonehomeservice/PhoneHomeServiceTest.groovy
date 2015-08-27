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
import framework.ReposeValveTest
import framework.mocks.MockIdentityService
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain
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
class PhoneHomeServiceTest extends ReposeValveTest {
    def static originEndpoint
    def static identityEndpoint
    def static phonehomeEndpoint
    def static MockIdentityService fakeIdentityService

    def setupSpec() {
        deproxy = new Deproxy()
        reposeLogSearch.cleanLog()
        // repose start up with no filter
        def logpath = logFile.substring(0, logFile.indexOf("logs"))
        reposeLogSearch.setLogFileLocation(logpath + "logs/phone-home.log")

        def params = properties.getDefaultTemplateParams()
        repose.configurationProvider.cleanConfigDirectory()
        repose.configurationProvider.applyConfigs("common", params);
        repose.configurationProvider.applyConfigs("features/core/phonehomeservice", params);

        repose.start()

        originEndpoint = deproxy.addEndpoint(properties.targetPort, 'origin service')
        phonehomeEndpoint = deproxy.addEndpoint(properties.phonehomePort, 'phone home service')
        //fakeIdentityService = new MockIdentityService(properties.identityPort, properties.targetPort)
        //identityEndpoint = deproxy.addEndpoint(properties.identityPort, 'identity service', null, fakeIdentityService.handler)
        //fakeIdentityService.checkTokenValid = true
    }

    def cleanupSpec() {
        if (deproxy) {
            deproxy.shutdown()
        }

        if (repose) {
            repose.stop()
        }
    }

    def "Verify Phone home service when start repose without any filter"() {
        given:
        // repose start up with no filter
        def file = reposeLogSearch.getLogFileLocation()


        when: "send request"
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint, method: 'GET')
        println(file)
        println(reposeLogSearch.printLog())

        then: "request will pass with simple config"
        reposeLogSearch.printLog() != null
    }

    def "Start Repose with some filters"() {
        given: "repose is started using a non-uri path for the wadl, in this case the path generic_pass.wadl"
        def params = properties.getDefaultTemplateParams()
        repose.configurationProvider.applyConfigs("features/core/phonehomeservice", params);
        repose.configurationProvider.applyConfigs("features/core/phonehomeservice/somefilters", params, sleep(5000));
        def file = reposeLogSearch.getLogFileLocation()

        when: "send request"
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint, method: 'GET')

        then: "request will pass with config"
        reposeLogSearch.printLog() != null
    }
}