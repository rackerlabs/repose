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
import org.openrepose.framework.test.ReposeValveTest
import scaffold.category.Core
import scaffold.category.Slow
import org.rackspace.deproxy.Deproxy
import spock.lang.Shared
import spock.lang.Unroll

@Category(Core)
class StartWithBadConfigsTest extends ReposeValveTest {

    @Shared
    Map params = [:]
    boolean expectCleanShutdown = true

    def setupSpec() {

        params = properties.getDefaultTemplateParams()

        // start a deproxy
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort)
    }

    @Unroll("start with bad #componentLabel configs, should get 503")
    def "start with bad #componentLabel configs, should get 503"() {

        given:
        // set the common and good configs
        repose.configurationProvider.cleanConfigDirectory()
        repose.configurationProvider.applyConfigs("features/core/configloadingandreloading/${componentLabel}-common", params)
        repose.configurationProvider.applyConfigs("features/core/configloadingandreloading/${componentLabel}-bad", params)
        expectCleanShutdown = true

        repose.enableDebug()
        repose.start(killOthersBeforeStarting: false,
                waitOnJmxAfterStarting: false)
        repose.waitForDesiredResponseCodeFromUrl(reposeEndpoint, [503])


        expect: "starting Repose with good configs should yield 503's"
        deproxy.makeRequest(url: reposeEndpoint).receivedResponse.code == "503"


        where:
        componentLabel            | _
        "rate-limiting"           | _
        "versioning"              | _
        "translation"             | _
        "keystone-v2"             | _
        "dist-datastore"          | _
        "uri-user"                | _
        "header-user"             | _
        "ip-user"                 | _
        "validator"               | _
        "metrics"                 | _
        "connectionPooling"       | _
    }


    @Unroll("start with bad #componentLabel configs, should fail to connect")
    def "start with bad #componentLabel configs, should fail to connect"() {

        given:
        // set the common and good configs
        repose.configurationProvider.cleanConfigDirectory()
        repose.configurationProvider.applyConfigs("features/core/configloadingandreloading/${componentLabel}-common", params)
        repose.configurationProvider.applyConfigs("features/core/configloadingandreloading/${componentLabel}-bad", params)
        expectCleanShutdown = false

        repose.start(killOthersBeforeStarting: false,
                waitOnJmxAfterStarting: false)
        sleep 35000


        when: "starting Repose with bad configs should lead to a connection exception"
        deproxy.makeRequest(url: reposeEndpoint)

        then:
        thrown(ConnectException)

        where:
        componentLabel | _
        "system-model" | _
        "container"    | _
    }

    def cleanup() {
        repose?.stop(throwExceptionOnKill: expectCleanShutdown)
    }
}
