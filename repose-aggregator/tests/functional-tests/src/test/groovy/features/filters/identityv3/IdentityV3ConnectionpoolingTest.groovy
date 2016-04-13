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
package features.filters.identityv3

import framework.ReposeConfigurationProvider
import framework.ReposeValveLauncher
import framework.TestProperties
import framework.mocks.MockIdentityV3Service
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.Endpoint
import org.rackspace.deproxy.Handling
import spock.lang.Shared
import spock.lang.Specification

/**
 * Created by jennyvo on 9/5/14.
 * Test Repose re-use connection to identity service
 */
class IdentityV3ConnectionpoolingTest extends Specification {

    @Shared
    int reposePort
    @Shared
    int originServicePort
    @Shared
    int identityServicePort
    @Shared
    String urlBase

    @Shared
    MockIdentityV3Service fakeIdentityV3Service

    @Shared
    Deproxy deproxy
    @Shared
    Endpoint originEndpoint
    @Shared
    Endpoint identityEndpoint

    @Shared
    TestProperties properties
    @Shared
    def logFile
    @Shared
    ReposeConfigurationProvider reposeConfigProvider
    @Shared
    ReposeValveLauncher repose

    def setupSpec() {
        // get ports
        properties = new TestProperties()
        reposePort = properties.reposePort
        originServicePort = properties.targetPort
        identityServicePort = properties.identityPort

        fakeIdentityV3Service = new MockIdentityV3Service(identityServicePort, originServicePort)

        // start deproxy
        deproxy = new Deproxy()
        originEndpoint = deproxy.addEndpoint(originServicePort)
        identityEndpoint = deproxy.addEndpoint(identityServicePort,
                "identity", "localhost", fakeIdentityV3Service.handler)

        // configure and start repose
        def targetHostname = properties.targetHostname
        urlBase = "http://${targetHostname}:${reposePort}"
        logFile = properties.logFile

        def configDirectory = properties.configDirectory
        def configTemplates = properties.configTemplates
        reposeConfigProvider = new ReposeConfigurationProvider(configDirectory, configTemplates)

        repose = new ReposeValveLauncher(
                reposeConfigProvider,
                properties.reposeJar,
                urlBase,
                configDirectory,
                reposePort
        )
        repose.enableDebug()

        def params = properties.getDefaultTemplateParams()
        reposeConfigProvider.applyConfigs("common", params)
        reposeConfigProvider.applyConfigs("features/filters/identityv3/connectionpooling", params)
        reposeConfigProvider.applyConfigs("features/filters/identityv3/connectionpooling2", params)
        repose.start()
    }

    def cleanup() {
        if (deproxy)
            deproxy.shutdown()

        if (repose)
            repose.stop()
    }

    def "when a client makes requests, Repose should re-use the connection to the Identity service"() {

        setup: "craft an url to a resource that requires authentication"
        def url = "${urlBase}/servers/domainid/resource"

        when: "making two authenticated requests to Repose"
        def mc1 = deproxy.makeRequest(url: url, headers: ['X-Subject-Token': 'token1'])
        def mc2 = deproxy.makeRequest(url: url, headers: ['X-Subject-Token': 'token2'])

        // collect all of the handlings that make it to the identity endpoint into one list
        def allOrphanedHandlings = mc1.orphanedHandlings + mc2.orphanedHandlings
        List<Handling> identityHandlings = allOrphanedHandlings.findAll { it.endpoint == identityEndpoint }
        def commons = allOrphanedHandlings.intersect(identityHandlings)
        def diff = allOrphanedHandlings.plus(identityHandlings)
        diff.removeAll(commons)


        then: "the connections for Repose's request to Identity should have the same id"
        mc1.orphanedHandlings.size() > 0
        mc2.orphanedHandlings.size() > 0
        identityHandlings.size() > 0
        // there should be no requests to auth with a different connection id
        diff.size() == 0
    }
}
