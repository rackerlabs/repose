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
package features.core.configloadingandreloading

import org.junit.experimental.categories.Category
import org.openrepose.framework.test.PortFinder
import org.openrepose.framework.test.ReposeValveTest
import org.rackspace.deproxy.Deproxy
import scaffold.category.Core
import spock.lang.Shared
import spock.lang.Unroll

import java.util.concurrent.TimeUnit

@Category(Core)
class TransitionBadToGoodConfigsTest extends ReposeValveTest {

    @Shared
    Map params = [:]

    def setupSpec() {

        int dataStorePort = PortFinder.instance.getNextOpenPort()
        params = properties.getDefaultTemplateParams()

        params += [
                'datastorePort': dataStorePort,
        ]

        // start a deproxy
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort)
    }

    @Unroll("start with bad #componentLabel configs, change to good, should get #expectedResponseCode")
    def "start with bad #componentLabel configs, change to good, should get #expectedResponseCode"() {

        given: "set the component-specific bad configs"
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/core/configloadingandreloading/${componentLabel}-common", params)
        repose.configurationProvider.applyConfigs("features/core/configloadingandreloading/${componentLabel}-bad", params)

        and: "start repose"
        repose.start(killOthersBeforeStarting: false)

        expect: "starting Repose with bad configs should yield 503's"
        deproxy.makeRequest(url: reposeEndpoint).receivedResponse.code == "503"


        when: "the configs are changed to good ones and we wait for Repose to pick up the change"
        repose.configurationProvider.applyConfigs("features/core/configloadingandreloading/${componentLabel}-good", params)
        repose.waitForNon500FromUrl(reposeEndpoint, 120)

        then: "Repose should start returning #expectedResponseCode"
        deproxy.makeRequest(url: reposeEndpoint).receivedResponse.code == "$expectedResponseCode"


        where:
        componentLabel       | expectedResponseCode
        "rate-limiting"      | 200
        "versioning"         | 200
        "translation"        | 200
        "keystone-v2"        | 200
        "dist-datastore"     | 200
        "uri-user"           | 200
        "header-user"        | 200
        "ip-user"            | 200
        "validator"          | 200
        "metrics"            | 200
        "connectionPooling"  | 200
    }

    @Unroll("start with bad #componentLabel configs, change to good (for configs that lead to connection errors)")
    def "start with bad #componentLabel configs, change to good (for configs that lead to connection errors)"() {

        given: "set the component-specific bad configs"
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/core/configloadingandreloading/${componentLabel}-common", params)
        repose.configurationProvider.applyConfigs("features/core/configloadingandreloading/${componentLabel}-bad", params)

        and: "start repose"
        repose.start(killOthersBeforeStarting: false, waitOnJmxAfterStarting: false)
        reposeLogSearch.awaitByString(
            "Configuration update error. Reason: Parsed object from XML does not match the expected configuration class. " +
                "Expected: org.openrepose.core.systemmodel.config.SystemModel",
            1,
            35,
            TimeUnit.SECONDS,
        )

        when: "starting Repose with bad configs should lead to a connection exception"
        deproxy.makeRequest(url: reposeEndpoint)

        then:
        thrown(ConnectException)


        when: "the configs are changed to good ones and we wait for Repose to pick up the change"
        repose.configurationProvider.applyConfigs("features/core/configloadingandreloading/${componentLabel}-good", params)
        repose.waitForDesiredResponseCodeFromUrl(reposeEndpoint, [200])

        then: "Repose should start returning 200's"
        deproxy.makeRequest(url: reposeEndpoint).receivedResponse.code == "200"


        where:
        componentLabel | _
        "system-model" | _
        // TODO: This is a known bug that can be tracked at: https://repose.atlassian.net/browse/REP-7505
        // "container"    | _
    }

    def cleanup() {
        repose?.stop(throwExceptionOnKill: false)
    }
}
