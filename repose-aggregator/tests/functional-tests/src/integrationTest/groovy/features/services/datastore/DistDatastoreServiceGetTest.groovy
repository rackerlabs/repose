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

import org.apache.commons.lang3.RandomStringUtils
import org.junit.experimental.categories.Category
import org.openrepose.commons.utils.io.ObjectSerializer
import org.openrepose.framework.test.PortFinder
import org.openrepose.framework.test.ReposeValveTest
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain
import scaffold.category.Services

import static javax.servlet.http.HttpServletResponse.*

@Category(Services)
class DistDatastoreServiceGetTest extends ReposeValveTest {

    //Since we're serializing objects here for the dist datastore, we must have the dist datastore objects in our classpath
    final ObjectSerializer objectSerializer = new ObjectSerializer(this.getClass().getClassLoader())

    def DD_URI
    def DD_HEADERS = ['X-TTL': '10']
    def KEY
    def DD_PATH = "/powerapi/dist-datastore/objects/"
    static def distDatastoreEndpoint

    def setupSpec() {
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort)
        int dataStorePort1 = PortFinder.instance.getNextOpenPort()
        int dataStorePort2 = PortFinder.instance.getNextOpenPort()

        distDatastoreEndpoint = "http://localhost:${dataStorePort1}"

        def params = properties.getDefaultTemplateParams()
        params += [
            'datastorePort1': dataStorePort1,
            'datastorePort2': dataStorePort2
        ]
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/services/datastore/", params)
        repose.start([clusterId: 'repose', nodeId: 'nofilters'])
        repose.waitForNon500FromUrl(reposeEndpoint, 120)
    }

    def setup() {
        DD_URI = distDatastoreEndpoint + DD_PATH
        KEY = UUID.randomUUID().toString()
    }

    def "GET with no key returns 404 NOT FOUND"() {
        when:
        MessageChain mc = deproxy.makeRequest([method: 'GET', url: DD_URI, headers: DD_HEADERS])

        then:
        mc.receivedResponse.code as Integer == SC_NOT_FOUND
        mc.receivedResponse.body.toString().contains("Cache key specified is invalid")
    }

    def "GET of invalid key fails with 404 NOT FOUND"() {
        given:
        def badKey = "////////" + UUID.randomUUID().toString()

        when: "I attempt to get the value from cache"
        MessageChain mc = deproxy.makeRequest([method: 'GET', url: distDatastoreEndpoint, path: DD_PATH + badKey, headers: DD_HEADERS])

        then:
        mc.receivedResponse.code as Integer == SC_NOT_FOUND
    }

    def "GET with a really large key returns a 414"() {
        given:
        def tooLargeKey = RandomStringUtils.random(2097139, ('A'..'Z') as char[])

        when: "I attempt to get the value from cache"
        MessageChain mc = deproxy.makeRequest([method: 'GET', url: distDatastoreEndpoint, path: DD_PATH + tooLargeKey, headers: DD_HEADERS])

        then:
        mc.receivedResponse.code as Integer == SC_REQUEST_URI_TOO_LONG
    }

    def "GET of key after time to live has expired should return a 404"() {
        given:
        def body = objectSerializer.writeObject('foo')
        MessageChain mc = deproxy.makeRequest([method: 'PUT', url: DD_URI + KEY, headers: ['X-TTL': '2'], requestBody: body])

        when:
        mc = deproxy.makeRequest([method: 'GET', url: DD_URI + KEY, headers: DD_HEADERS])

        then:
        mc.receivedResponse.code as Integer == SC_OK
        mc.receivedResponse.body == body

        when: "I wait long enough for the TTL of the item to expire"
        Thread.sleep(3000)

        and: "I get the key again"
        mc = deproxy.makeRequest([method: 'GET', url: DD_URI + KEY, headers: DD_HEADERS])

        then:
        mc.receivedResponse.code as Integer == SC_NOT_FOUND
    }
}
