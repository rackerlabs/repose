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
import org.openrepose.core.services.datastore.types.StringValue
import org.openrepose.framework.test.PortFinder
import org.openrepose.framework.test.ReposeValveTest
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain
import scaffold.category.Services

@Category(Services)
class DistDatastoreServicePatchTest extends ReposeValveTest {
    //Since we're serializing objects here for the dist datastore, we must have the dist datastore objects in our classpath
    final ObjectSerializer objectSerializer = new ObjectSerializer(this.getClass().getClassLoader())

    String DD_URI
    def DD_HEADERS = ['X-TTL': '10']
    def BODY = objectSerializer.writeObject(new StringValue.Patch("test data"))
    def INVALID_BODY = objectSerializer.writeObject("test data")
    static def KEY
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

    def "PATCH a new cache object should return 200 response"() {
        when:
        MessageChain mc = deproxy.makeRequest([method: 'PATCH', url: DD_URI + KEY, headers: DD_HEADERS, requestBody: BODY])

        then:
        mc.receivedResponse.code == '200'
    }

    def "PATCH a new cache object with invalid Patch should return 400 response"() {
        when:
        MessageChain mc = deproxy.makeRequest([method: 'PATCH', url: DD_URI + KEY, headers: DD_HEADERS, requestBody: INVALID_BODY])

        then:
        mc.receivedResponse.code == '400'
    }

    def "PATCH with query parameters should ignore query params and return 200"() {
        when:
        MessageChain mc = deproxy.makeRequest([method: 'PATCH', url: DD_URI + KEY + "?foo=bar", headers: DD_HEADERS, requestBody: BODY])

        then:
        mc.receivedResponse.code == '200'

        when:
        mc = deproxy.makeRequest([method: 'GET', url: DD_URI + KEY, headers: DD_HEADERS])

        then:
        objectSerializer.readObject(mc.receivedResponse.body as byte[]).value == "test data"

    }

    def "PATCH a cache object to an existing key should append onto the cached value"() {

        when: "I make 2 PATCH calls for 2 different values for the same key"
        def newBody = objectSerializer.writeObject(new StringValue.Patch("MY NEW VALUE"))
        deproxy.makeRequest([method: 'PATCH', url: DD_URI + KEY, headers: DD_HEADERS, requestBody: BODY])
        deproxy.makeRequest([method: 'PATCH', url: DD_URI + KEY, headers: DD_HEADERS, requestBody: newBody])

        and: "I get the value for the key"
        MessageChain mc = deproxy.makeRequest([method: 'GET', url: DD_URI + KEY, headers: DD_HEADERS])

        then: "The body of the get response should be the appended value"
        objectSerializer.readObject(mc.receivedResponse.body as byte[]).value == "test dataMY NEW VALUE"
    }

    def "PATCH with missing X-TTL is allowed"() {
        when:
        MessageChain mc = deproxy.makeRequest([method: 'PATCH', url: DD_URI + KEY, headers: [], requestBody: BODY])

        then:
        mc.receivedResponse.code == '200'

        when: "I get the value for the key"
        mc = deproxy.makeRequest([method: 'GET', url: DD_URI + KEY, headers: DD_HEADERS])

        then:
        objectSerializer.readObject(mc.receivedResponse.body as byte[]).value == "test data"
    }

    def "PATCH with empty string as body is allowed, and GET will return it"() {
        when:
        MessageChain mc = deproxy.makeRequest([method: 'PATCH', url: DD_URI + KEY, headers: DD_HEADERS, requestBody: objectSerializer.writeObject(new StringValue.Patch(""))])

        then:
        mc.receivedResponse.code == '200'

        when: "I get the value from cache with the empty body"
        mc = deproxy.makeRequest([method: 'GET', url: DD_URI + KEY, headers: DD_HEADERS])

        then:
        mc.receivedResponse.code == '200'
        objectSerializer.readObject(mc.receivedResponse.body as byte[]).value == ""
    }

    def "PATCH with no key should return 400 Bad Request"() {
        when:
        MessageChain mc = deproxy.makeRequest([method: 'PATCH', url: DD_URI, headers: DD_HEADERS, requestBody: BODY])

        then:
        mc.receivedResponse.code == '400'
        mc.receivedResponse.body.toString().contains("Cache key specified is invalid")
    }

    def "PATCH of invalid key should fail with 400 Bad Request"() {

        when:
        MessageChain mc = deproxy.makeRequest([method : 'PATCH', url: distDatastoreEndpoint, path: DD_PATH + key,
                                               headers: DD_HEADERS, requestBody: BODY])

        then:
        mc.receivedResponse.code == '400'

        when: "I attempt to get the value from cache"
        mc = deproxy.makeRequest([method : 'GET', url: distDatastoreEndpoint, path: DD_PATH + key,
                                  headers: DD_HEADERS])

        then:
        mc.receivedResponse.code == '404'
        mc.receivedResponse.body.toString().contains("Cache key specified is invalid")

        where:
        key                                       | scenario
        UUID.randomUUID().toString() + "///"      | "unnecessary slashes on path"
        "foo/bar/?assd=adff"                      | "includes query parameters"
        "foo"                                     | "less than 36 chars"
        UUID.randomUUID().toString() + "a"        | "more than 36 chars"
        "////////" + UUID.randomUUID().toString() | "leading slashes on path"
        ""                                        | "empty key"
        "%20foo%20"                               | "spaces"
        "%2F%2D%20"                               | "random encoded characters"
    }

    //Stolen from: http://stackoverflow.com/a/2474496/423218
    def makeLargeString(int size) {
        StringBuilder sb = new StringBuilder(size)
        (0..size).each { count ->
            sb.append(randomChar())
        }
        sb.toString()
    }

    //Stolen from http://stackoverflow.com/a/2627897/423218
    def randomChar() {
        int rnd = (int) (Math.random() * 52)
        char base = (rnd < 26) ? 'A' : 'a'
        return (char) (base + rnd % 26)
    }

    def "PATCH with really large body within limit (2MEGS 2096139) should return 200"() {
        given:
        def largeBodyContent = makeLargeString(2096139)
        def largeBody = objectSerializer.writeObject(new StringValue.Patch(largeBodyContent))

        when:
        MessageChain mc = deproxy.makeRequest([method: 'PATCH', url: DD_URI + KEY, headers: DD_HEADERS, requestBody: largeBody])

        then:
        mc.receivedResponse.code == "200"

        when: "I attempt to get the value from cache"
        mc = deproxy.makeRequest([method: 'GET', url: DD_URI + KEY, headers: DD_HEADERS])

        then:
        mc.receivedResponse.code == "200"
        largeBodyContent.size() == objectSerializer.readObject(mc.receivedResponse.body as byte[]).value.size()
    }

    def "PATCH with really large body outside limit (2MEGS 2097152) should return 413 Entity Too Large"() {
        given:
        def largeBody = objectSerializer.writeObject(new StringValue.Patch(makeLargeString(2097152)))

        when:
        MessageChain mc = deproxy.makeRequest([method: 'PATCH', url: DD_URI + KEY, headers: DD_HEADERS, requestBody: largeBody])

        then:
        mc.receivedResponse.code == "413"
        mc.receivedResponse.body.toString().contains("Object is too large to store into the cache")

        when: "I attempt to get the value from cache"
        mc = deproxy.makeRequest([method: 'GET', url: DD_URI + KEY, headers: DD_HEADERS])

        then:
        mc.receivedResponse.code == "404"
    }
}
