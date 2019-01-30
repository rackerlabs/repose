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

import org.apache.commons.io.FileUtils
import org.junit.experimental.categories.Category
import org.openrepose.framework.test.PortFinder
import org.openrepose.framework.test.ReposeValveTest
import org.rackspace.deproxy.Deproxy
import scaffold.category.Core

import java.util.concurrent.TimeoutException

/**
 * D-15183 Ensure passwords are not logged when in DEBUG mode and config files are updated.
 */
@Category(Core)
class ReposeStartupTest extends ReposeValveTest {

    def setupSpec() {
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort)
    }

    static def params

    def "repose should start with installation configs"() {
        setup:
        def params = properties.getDefaultTemplateParams()
        def nextPort = PortFinder.instance.getNextOpenPort()

        //note: Order matters here. The common directory overwrites some of the configs from the core directory.
        //      This means that the core configs we provide may not get tested, but due to the structure of our tests,
        //      this is currently "hard" to fix.
        repose.configurationProvider.applyConfigs("../../../../artifacts/valve/src/config/filters", params)
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("../../../../artifacts/extensions-filter-bundle/src/config/filters", params)
        repose.configurationProvider.applyConfigs("../../../../artifacts/filter-bundle/src/config/filters", params)
        String systemModelTemp = "${repose.configurationProvider.reposeConfigDir}/system-model.cfg.xml.${nextPort}"
        String systemModelSource = "${repose.configurationProvider.reposeConfigDir}/system-model.cfg.xml"
        new File(systemModelTemp).withWriter {
            out ->
                new File(systemModelSource).eachLine {
                    line ->
                        out << line.replaceAll("http-port=\"8080\"", "http-port=\"${nextPort}\"")
                }
        }
        FileUtils.copyFile(new File(systemModelTemp), new File(systemModelSource))

        repose.start()


        when:
        //todo: use a dynamic port (will require tinkering with [a copy of] the installation system-model).
        repose.waitForNon500FromUrl("http://localhost:${nextPort}")

        then:
        notThrown(TimeoutException)

        cleanup:
        repose.stop([throwExceptionOnKill: false])
    }
}

