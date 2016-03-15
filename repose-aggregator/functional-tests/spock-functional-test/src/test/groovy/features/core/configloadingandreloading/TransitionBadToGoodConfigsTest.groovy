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

import framework.ReposeConfigurationProvider
import framework.ReposeLogSearch
import framework.ReposeValveLauncher
import framework.TestProperties
import framework.category.Slow
import org.junit.experimental.categories.Category
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.PortFinder
import spock.lang.Specification
import spock.lang.Unroll

@Category(Slow.class)
class TransitionBadToGoodConfigsTest extends Specification {

    static int targetPort
    static Deproxy deproxy

    int reposePort
    String url
    TestProperties properties
    ReposeConfigurationProvider reposeConfigProvider
    ReposeLogSearch reposeLogSearch
    ReposeValveLauncher repose
    Map params = [:]

    def setup() {

        properties = new TestProperties()
        this.reposePort = properties.reposePort
        this.targetPort = properties.targetPort
        this.url = properties.reposeEndpoint

        int dataStorePort = PortFinder.Singleton.getNextOpenPort()
        params = properties.getDefaultTemplateParams()

        params += [
                'datastorePort': dataStorePort,
        ]

        // start a deproxy
        deproxy = new Deproxy()
        deproxy.addEndpoint(this.targetPort)

        // setup config provider
        reposeConfigProvider = new ReposeConfigurationProvider(properties.getConfigDirectory(), properties.getConfigTemplates())

        // set the common configs
        reposeConfigProvider.cleanConfigDirectory()

        repose = new ReposeValveLauncher(
                reposeConfigProvider,
                properties.getReposeJar(),
                url,
                properties.getConfigDirectory(),
                reposePort
        )
        repose.enableDebug()
        reposeLogSearch = new ReposeLogSearch(properties.getLogFile());
    }

    @Unroll("start with bad #componentLabel configs, change to good, should get #expectedResponseCode")
    def "start with bad #componentLabel configs, change to good, should get #expectedResponseCode"() {

        given:
        // set the component-specific bad configs
        reposeConfigProvider.applyConfigs("features/core/configloadingandreloading/${componentLabel}-common", params)
        reposeConfigProvider.applyConfigs("features/core/configloadingandreloading/${componentLabel}-bad", params)

        // start repose
        repose.start(killOthersBeforeStarting: false,
                waitOnJmxAfterStarting: false)
        repose.waitForDesiredResponseCodeFromUrl(url, [503], 120)


        expect: "starting Repose with good configs should yield 503's"
        deproxy.makeRequest(url: url).receivedResponse.code == "503"


        when: "the configs are changed to good ones and we wait for Repose to pick up the change"
        reposeConfigProvider.applyConfigs("features/core/configloadingandreloading/${componentLabel}-good", params)
        sleep 15000
        repose.waitForNon500FromUrl(url, 120)

        then: "Repose should start returning #expectedResponseCode"
        deproxy.makeRequest(url: url).receivedResponse.code == "${expectedResponseCode}"




        where:
        componentLabel            | expectedResponseCode
        "response-messaging"      | 200
        "rate-limiting"           | 200
        "versioning"              | 200
        "translation"             | 200
        "keystone-v2"             | 200
        "dist-datastore"          | 200
        "uri-user"                | 200
        "header-identity"         | 200
        "header-id-mapping"       | 200
        "ip-user"                 | 200
        "validator"               | 200
        "metrics"                 | 200
        "connectionPooling"       | 200
    }

    @Unroll("start with bad #componentLabel configs, change to good (for configs that lead to connection errors)")
    def "start with bad #componentLabel configs, change to good (for configs that lead to connection errors)"() {

        given:
        // set the component-specific bad configs
        reposeConfigProvider.applyConfigs("features/core/configloadingandreloading/${componentLabel}-common", params)
        reposeConfigProvider.applyConfigs("features/core/configloadingandreloading/${componentLabel}-bad", params)

        // start repose
        repose.start(killOthersBeforeStarting: false,
                waitOnJmxAfterStarting: false)
        sleep 35000


        when: "starting Repose with bad configs should lead to a connection exception"
        deproxy.makeRequest(url: url)

        then:
        thrown(ConnectException)


        when: "the configs are changed to good ones and we wait for Repose to pick up the change"
        reposeConfigProvider.applyConfigs("features/core/configloadingandreloading/${componentLabel}-good", params)
        sleep 35000

        then: "Repose should start returning 200's"
        deproxy.makeRequest(url: url).receivedResponse.code == "200"


        where:
        componentLabel | _
        "system-model" | _
        "container"    | _
    }

    def cleanup() {
        if (repose) {
            repose.stop(throwExceptionOnKill: false)
        }
        if (deproxy) {
            deproxy.shutdown()
        }
    }
}
