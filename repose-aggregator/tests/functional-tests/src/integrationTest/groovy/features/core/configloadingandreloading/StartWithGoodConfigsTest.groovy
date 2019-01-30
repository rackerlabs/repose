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

@Category(Core)
class StartWithGoodConfigsTest extends ReposeValveTest {

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

    @Unroll("start with good #componentLabel configs, should get #expectedResponseCode")
    def "start with good #componentLabel configs, should get #expectedResponseCode"() {

        given:
        // set the common and good configs
        repose.configurationProvider.cleanConfigDirectory()
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/core/configloadingandreloading/${componentLabel}-common", params)
        repose.configurationProvider.applyConfigs("features/core/configloadingandreloading/${componentLabel}-good", params)

        repose.start(killOthersBeforeStarting: false,
                waitOnJmxAfterStarting: false)
        repose.waitForNon500FromUrl(reposeEndpoint)


        expect: "starting Repose with good configs should yield 200's"
        deproxy.makeRequest(url: reposeEndpoint).receivedResponse.code == "${expectedResponseCode}"


        where:
        componentLabel       | expectedResponseCode
        "system-model"       | 200
        "container"          | 200
        "rate-limiting"      | 200
        "versioning"         | 200
        "translation"        | 200
        "keystone-v2"        | 200
        "dist-datastore"     | 200
        "uri-user"           | 200
        "header-user"        | 200
        "ip-user"            | 200
        "validator"          | 200
    }
}

