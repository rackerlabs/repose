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
package features.core.startup

import org.openrepose.framework.test.*
import org.rackspace.deproxy.Deproxy
import spock.lang.Ignore
import spock.lang.Specification

/**
 * This test was written to verify the startup behavior of Repose running in a container. It would leverage the
 * embedded container testing framework. Unfortunately, that framework does not currently support logging of Repose
 * startup events, and thus is not sufficient to test startup conditions.
 */

@Ignore
class ReposeStartupGlassfishTest extends Specification {
    static def reposeGlassfishEndpoint

    static int reposePort
    static String rootWar
    static Deproxy deproxy
    static ReposeLauncher repose
    static ReposeLogSearch reposeLogSearch
    static ReposeConfigurationProvider configProvider

    static TestProperties properties = new TestProperties(this.getClass().canonicalName.replace('.', '/'))

    def setupSpec() {
        def params = properties.getDefaultTemplateParams()
        def configDirectory = properties.getConfigDirectory()
        def configTemplates = properties.getConfigTemplates()
        def logFile = properties.logFile

        rootWar = properties.getReposeRootWar()
        reposePort = properties.reposePort

        configProvider = new ReposeConfigurationProvider(configDirectory, configTemplates)

        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort)

        reposeGlassfishEndpoint = "http://localhost:${reposePort}"

        params += ['reposePort': reposePort]

        configProvider.applyConfigs("features/core/systemprops", params)
        configProvider.applyConfigs("common", params)

        reposeLogSearch = new ReposeLogSearch(logFile)
    }

    def setup() {
        reposeLogSearch.cleanLog()
    }

    def cleanupSpec() {
        repose?.stop()
        deproxy?.shutdown()
    }

    def "when Repose is started without the repose-cluster-id property, a message should be logged and Repose should stop"() {
        given:
        repose = new ReposeContainerLauncher(configProvider, properties.getGlassfishJar(), null, "node", rootWar, reposePort)

        when:
        repose.enableDebug()
        repose.start()
        sleep(10000)

        then:
        reposeLogSearch.searchByString("repose-cluster-id not provided -- Repose cannot start").size() == 1
    }
}
