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
import scaffold.category.XmlParsing
import spock.lang.Unroll

import java.util.concurrent.TimeoutException

@Category(XmlParsing)
class ValidateCheckerTest extends ReposeValveTest {

    def setupSpec() {
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort)
    }

    @Unroll
    def "don't expect timeout when checker is #configPath"() {
        given:
        repose.stop()

        def params = properties.getDefaultTemplateParams()
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/filters/apivalidator/pcchecker/$configPath", params)
        repose.start()

        when:
        repose.waitForNon500FromUrl(reposeEndpoint, 30)

        then:
        notThrown(TimeoutException.class)

        where:
        configPath << ["validwithoutvalidation", "validwithvalidation"]
    }

    @Unroll
    def "expect request timeout when check is #configPath"() {
        given:
        repose.stop()

        def params = properties.getDefaultTemplateParams()
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/filters/apivalidator/pcchecker/$configPath", params)
        repose.start()

        when:
        repose.waitForNon500FromUrl(reposeEndpoint, 30)

        then:
        thrown(TimeoutException.class) // Thrown when processing a request, not at initialization
        reposeLogSearch.searchByString("Error loading validator for WADL").size() >= 1
        reposeLogSearch.searchByString("key not found: SE9001").size() >= 1

        where:
        configPath << ["invalidwithoutvalidation", "invalidwithvalidation"]
    }
}
