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
package features.services.datastore

import org.openrepose.commons.utils.io.ObjectSerializer
import org.openrepose.core.services.datastore.types.StringValue
import org.openrepose.framework.test.PortFinder
import org.openrepose.framework.test.ReposeValveTest
import org.rackspace.deproxy.ApacheClientConnector
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain
import spock.lang.Shared

import javax.net.ssl.SSLHandshakeException
import java.nio.file.Files
import java.util.concurrent.TimeUnit

import static org.openrepose.framework.test.ReposeLauncher.MAX_STARTUP_TIME

class DistDatastoreNoClientAuthTest extends ReposeValveTest {
    //Since we're serializing objects here for the dist datastore, we must have the dist datastore objects in our classpath
    final ObjectSerializer objectSerializer = new ObjectSerializer(this.getClass().getClassLoader())

    @Shared
    def params
    @Shared
    def String distDatastoreEndpoint
    @Shared
    def File singleFile

    def setupSpec() {
        int dataStorePort = PortFinder.instance.getNextOpenPort()

        distDatastoreEndpoint = "https://localhost:${dataStorePort}"

        params = properties.getDefaultTemplateParams()
        params += [
                'datastorePort': dataStorePort
        ]

        reposeLogSearch.cleanLog()
        repose.configurationProvider.cleanConfigDirectory()

        repose.configurationProvider.applyConfigs("common", params)
        // Have to manually copy binary files, because the applyConfigs() attempts to substitute template parameters
        // when they are found and it breaks everything. :(
        def singleFileOrig = new File(repose.configurationProvider.configTemplatesDir, "common/single.jks")
        singleFile = new File(repose.configDir, "single.jks")
        def singleFileDest = new FileOutputStream(singleFile)
        Files.copy(singleFileOrig.toPath(), singleFileDest)
        repose.configurationProvider.applyConfigs("features/services/datastore/clientauth", params)
        repose.configurationProvider.applyConfigs("features/services/datastore/clientauth/notruststore", params)

        deproxy = new Deproxy(null, new ApacheClientConnector())
        deproxy.addEndpoint(properties.targetPort)

        repose.start()
        reposeLogSearch.awaitByString("Repose ready", 1, MAX_STARTUP_TIME, TimeUnit.SECONDS)
    }

    def "should not be able to put an object in the datastore as an anonymous client"() {
        given:
        def headers = ['X-TTL': '5']
        def objectkey = UUID.randomUUID().toString()
        def body = objectSerializer.writeObject(new StringValue("test data"))

        when:
        MessageChain mc =
                deproxy.makeRequest(
                        [
                                method     : 'PUT',
                                url        : distDatastoreEndpoint + "/powerapi/dist-datastore/objects/" + objectkey,
                                headers    : headers,
                                requestBody: body
                        ])

        then:
        thrown(SSLHandshakeException)
    }
}
