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

import org.junit.experimental.categories.Category
import org.openrepose.commons.utils.io.ObjectSerializer
import org.openrepose.framework.test.PortFinder
import org.openrepose.framework.test.ReposeValveTest
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain
import scaffold.category.Services

@Category(Services)
class DistDatastoreServiceDeleteTest extends ReposeValveTest {

    //Since we're serializing objects here for the dist datastore, we must have the dist datastore objects in our classpath
    final ObjectSerializer objectSerializer = new ObjectSerializer(this.getClass().getClassLoader())

    def DD_URI
    def DD_HEADERS = ['X-TTL': '10']
    def BODY = objectSerializer.writeObject('test body')
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
        repose.start([clusterId: "repose", nodeId: "nofilters"])
        repose.waitForNon500FromUrl(reposeEndpoint, 120)
    }

    def setup() {
        DD_URI = distDatastoreEndpoint + DD_PATH
        KEY = UUID.randomUUID().toString()
    }

    def "DELETE of existing item in datastore should return 204 and no longer be available"() {
        when: "Adding the object to the datastore"
        MessageChain mc = deproxy.makeRequest([method: 'PUT', url: DD_URI + KEY, headers: DD_HEADERS, requestBody: BODY])

        and: "checking that it's there"
        mc = deproxy.makeRequest([method: 'GET', url: DD_URI + KEY, headers: DD_HEADERS])

        then: "should report that it is"
        mc.receivedResponse.code == "200"
        mc.receivedResponse.body == BODY

        when: "deleting the object from the datastore"
        mc = deproxy.makeRequest(method: "DELETE", url: DD_URI + KEY, headers: DD_HEADERS)

        then: "should report that it was successfully deleted"
        mc.receivedResponse.code == "204"
        mc.receivedResponse.body == ""

        when: "checking that it is no longer available in the datastore"
        mc = deproxy.makeRequest([method: 'GET', url: DD_URI + KEY, headers: DD_HEADERS])

        then: "should report it missing"
        mc.receivedResponse.code == "404"
        mc.receivedResponse.body == ""
    }

    def "DELETE an item that is not in the datastore returns a 204"() {
        when:
        MessageChain mc = deproxy.makeRequest(method: "DELETE", url: DD_URI + UUID.randomUUID().toString(), headers: DD_HEADERS)

        then:
        mc.receivedResponse.code == "204"
    }

    def "DELETE to invalid target will return 404"() {

        when:
        MessageChain mc = deproxy.makeRequest([method: 'DELETE', url: distDatastoreEndpoint + "/invalid/target", headers: DD_HEADERS])

        then:
        mc.receivedResponse.code == '404'
    }

    def "DELETE of invalid key fails 204 No Content"() {

        given:
        def badKey = "////////" + UUID.randomUUID().toString()

        when: "I attempt to get the value from cache"
        MessageChain mc = deproxy.makeRequest([method: 'DELETE', url: distDatastoreEndpoint, path: DD_PATH + badKey, headers: DD_HEADERS])

        then:
        mc.receivedResponse.code == '204'
    }


}
