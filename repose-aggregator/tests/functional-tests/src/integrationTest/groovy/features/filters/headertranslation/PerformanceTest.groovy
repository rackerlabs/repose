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
package features.filters.headertranslation

import framework.ReposeValveTest
import framework.category.Flaky
import org.joda.time.DateTime
import org.junit.experimental.categories.Category
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain

@Category(Flaky)
class PerformanceTest extends ReposeValveTest {

    def setupSpec() {
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort)
    }

    def "performance test configs produce expected responses"() {
        when: "I make a request through header translation filter"
        def params = properties.defaultTemplateParams
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/filters/headertranslation/common", params)
        repose.start()
        MessageChain mcHdrXlate = deproxy.makeRequest(url: reposeEndpoint, method: "GET", headers: ["X-OneToMany-A": "12345", "X-OneToMany-B": "abcde"])
        repose.stop()

        and: "I make a request through header translation filter"
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/filters/headertranslation/perftest", params)
        repose.start()
        MessageChain mcTranslation = deproxy.makeRequest(url: reposeEndpoint, method: "GET", headers: ["X-OneToMany-A": "12345", "X-OneToMany-B": "abcde"])
        repose.stop()

        then: "Headers are translated correctly by HDR XLATE"
        def mcHdrHandling = mcHdrXlate.handlings.get(0)
        mcHdrHandling.request.headers.contains("X-OneToMany-A")
        mcHdrHandling.request.headers.contains("X-OneToMany-C")
        mcHdrHandling.request.headers.contains("X-OneToMany-D")
        mcHdrHandling.request.headers.getFirstValue("X-OneToMany-C") == mcHdrXlate.sentRequest.headers.getFirstValue("X-OneToMany-A")
        mcHdrHandling.request.headers.getFirstValue("X-OneToMany-D") == mcHdrXlate.sentRequest.headers.getFirstValue("X-OneToMany-A")

        then: "Headers are translated correctly by Translation Filter"
        def mcTransHandling = mcTranslation.handlings.get(0)
        mcTransHandling.request.headers.contains("X-OneToMany-A")
        mcTransHandling.request.headers.contains("X-OneToMany-C")
        mcTransHandling.request.headers.contains("X-OneToMany-D")
        mcTransHandling.request.headers.getFirstValue("X-OneToMany-C") == mcTranslation.sentRequest.headers.getFirstValue("X-OneToMany-A")
        mcTransHandling.request.headers.getFirstValue("X-OneToMany-D") == mcTranslation.sentRequest.headers.getFirstValue("X-OneToMany-A")
    }

    def "performance test to ensure header translation is on par or better than translation filter"() {
        given:
        int totalRequests = 1000

        when: "I make 100 requests through header translation filter"
        def params = properties.getDefaultTemplateParams()
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/filters/headertranslation/common", params)
        repose.start()
        def averageWithHdrXlate = makeRequests(totalRequests)
        repose.stop()

        and: "I make 100 requests through translation filter"
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/filters/headertranslation/perftest", params)
        repose.start()
        def averageWithTranslationFilter = makeRequests(totalRequests)
        repose.stop()

        then: "Average response time for header translation is <= avg response time for translation filter"
        println("HDR XLATE: " + averageWithHdrXlate + " Translation: " + averageWithTranslationFilter)
        averageWithHdrXlate <= averageWithTranslationFilter * 1.1
    }


    def makeRequests(int totalRequests) {
        long totalMillis = 0

        // warm up, ignore response times
        for (int i : 1..100) {
            deproxy.makeRequest(url: reposeEndpoint, method: "GET", headers: ["X-Header-A": "12345", "X-Header-B": "abcde"])
        }

        // now let's capture response times
        for (int i : 1..totalRequests) {
            // start time
            def timeStart = new DateTime()
            MessageChain mc = deproxy.makeRequest(url: reposeEndpoint, method: "GET", headers: ["X-Header-A": "12345", "X-Header-B": "abcde"])
            def timeStop = new DateTime()
            def elapsedMillis = timeStop.millis - timeStart.millis
            totalMillis += elapsedMillis
        }

        return totalMillis / totalRequests
    }

}
