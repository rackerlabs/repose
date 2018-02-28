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
package features.core.wrappers.response

import org.openrepose.framework.test.ReposeConfigurationProvider
import org.openrepose.framework.test.ReposeContainerLauncher
import org.openrepose.framework.test.TestProperties
import org.rackspace.deproxy.Deproxy
import spock.lang.Shared
import spock.lang.Specification

class GlassfishResponseWrapperTest extends Specification {

    static final int ERROR_CODE = 419
    static final String ERROR_HEADER_NAME = "Error"
    static final String ERROR_MESSAGE = "OHJEEZ"
    static final String ERROR_BODY = "Your request is bad and you should feel bad!"

    @Shared
    Deproxy deproxy

    @Shared
    ReposeContainerLauncher glassfishLauncher

    @Shared
    TestProperties properties = new TestProperties(this.getClass().canonicalName.replace('.', '/'))

    def setupSpec() {
        def configurationProvider = new ReposeConfigurationProvider(properties)

        def params = properties.defaultTemplateParams
        params += [
            clusterId   : 'cluster1',
            nodeId      : 'node1',
            errorCode   : ERROR_CODE,
            errorMessage: ERROR_MESSAGE,
            errorBody   : ERROR_BODY,
            headerName  : ERROR_HEADER_NAME
        ]
        configurationProvider.applyConfigs("common", params)
        configurationProvider.applyConfigs("features/core/wrappers/response/war", params)

        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort)

        glassfishLauncher = new ReposeContainerLauncher(
            configurationProvider,
            properties.glassfishJar,
            params.clusterId as String,
            params.nodeId as String,
            properties.reposeGlassfishRootWar,
            properties.reposePort)
        glassfishLauncher.start()
        glassfishLauncher.waitForDesiredResponseCodeFromUrl(properties.getReposeEndpoint(), 200)
    }

    def cleanupSpec() {
        deproxy?.shutdown()
        glassfishLauncher?.stop()
    }

    def "a filter writes a header and response body then calls sendError on the response"() {
        when: "a request is made to Repose"
        def messageChain = deproxy.makeRequest(properties.getReposeEndpoint() + "/scripting")

        then: "the response code should be the error code, not a 500"
        messageChain.receivedResponse.code as Integer == ERROR_CODE

        and: "the status line message should be the error message"
        messageChain.receivedResponse.message == ERROR_MESSAGE

        and: "the response body should be the container-server error page"
        (messageChain.receivedResponse.body as String).length() != 0
        messageChain.receivedResponse.body as String != ERROR_BODY

        and: "the header should be written"
        messageChain.receivedResponse.headers.findAll(ERROR_HEADER_NAME).size() == 1
    }
}
