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
class TransitionGoodToBadConfigsTest extends ReposeValveTest {

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

    @Unroll("start with good #componentLabel configs, change to bad, should still get 200")
    def "start with good #componentLabel configs, change to bad, should still get 200"() {

        given:
        // set the common and good configs
        repose.configurationProvider.cleanConfigDirectory()
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/core/configloadingandreloading/${componentLabel}-common", params)
        repose.configurationProvider.applyConfigs("features/core/configloadingandreloading/${componentLabel}-good", params)

        repose.start(killOthersBeforeStarting: false,
                waitOnJmxAfterStarting: false)
        repose.waitForNon500FromUrl(reposeEndpoint)


        expect: "starting Repose with good configs should yield #expectedResponseCode"
        deproxy.makeRequest(url: reposeEndpoint).receivedResponse.code == "200"


        when: "the configs are changed to bad ones and we wait for Repose to pick up the change"
        repose.configurationProvider.applyConfigs(
                "features/core/configloadingandreloading/${componentLabel}-bad",
                params)
        reposeLogSearch.awaitByString(
            "Configuration update error. Reason: Parsed object from XML does not match the expected configuration class. " +
                "Expected: ${errorMessageBit}",
            1,
            15,
            TimeUnit.SECONDS,
        )

        then: "Repose should still return #expectedResponseCode"
        deproxy.makeRequest(url: reposeEndpoint).receivedResponse.code == "200"

        where:
        componentLabel      | errorMessageBit
        "system-model"      | "org.openrepose.core.systemmodel.config.SystemModel"
        "container"         | "org.openrepose.core.container.config.ContainerConfiguration"
        "rate-limiting"     | "org.openrepose.core.services.ratelimit.config.RateLimitingConfiguration"
        "versioning"        | "org.openrepose.filters.versioning.config.ServiceVersionMappingList"
        "translation"       | "org.openrepose.filters.translation.config.TranslationConfig"
        "keystone-v2"       | "org.openrepose.filters.keystonev2.config.KeystoneV2AuthenticationConfig"
        "dist-datastore"    | "org.openrepose.core.services.datastore.distributed.config.DistributedDatastoreConfiguration"
        "uri-user"          | "org.openrepose.filters.uriuser.config.UriUserConfig"
        "header-user"       | "org.openrepose.filters.headeruser.config.HeaderUserConfig"
        "ip-user"           | "org.openrepose.filters.ipuser.config.IpUserConfig"
        "validator"         | "org.openrepose.filters.apivalidator.config.ValidatorConfiguration"
        "metrics"           | "org.openrepose.core.services.reporting.metrics.config.MetricsConfiguration"
        "connectionPooling" | "org.openrepose.core.services.httpclient.config.HttpConnectionPoolsConfig"
    }

    def cleanup() {
        repose?.stop()
    }
}

