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
import scaffold.category.Slow
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain
import org.springframework.http.HttpHeaders

@Category(Slow.class)
class DistDatastoreServiceTest extends ReposeValveTest {
    //Since we're serializing objects here for the dist datastore, we must have the dist datastore objects in our classpath
    final ObjectSerializer objectSerializer = new ObjectSerializer(this.getClass().getClassLoader())

    static def params
    static def distDatastoreEndpoint

    def setupSpec() {
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort)
        int dataStorePort1 = PortFinder.instance.getNextOpenPort()
        int dataStorePort2 = PortFinder.instance.getNextOpenPort()

        distDatastoreEndpoint = "http://localhost:${dataStorePort1}"

        params = properties.getDefaultTemplateParams()
        params += [
                'datastorePort1': dataStorePort1,
                'datastorePort2': dataStorePort2
        ]

        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/services/datastore", params)
        repose.start([clusterId: "repose", nodeId: "nofilters"])
        waitUntilReadyToServiceRequests()
    }

    def "when configured with DD service, repose should start and successfully execute calls"() {
        when:
        MessageChain mc = deproxy.makeRequest([url: reposeEndpoint + "/cluster", headers: ['x-trace-request': 'true']])

        then:
        mc.receivedResponse.code == '200'
        mc.handlings.size() == 1
    }

    def "TRACE should return 405 response"() {
        given:
        def headers = ['X-TTL': '5']
        def objectkey = UUID.randomUUID().toString()
        def body = objectSerializer.writeObject(new StringValue.Patch("test data"))

        when:
        MessageChain mc =
                deproxy.makeRequest(
                        [
                                method     : 'TRACE',
                                url        : distDatastoreEndpoint + "/powerapi/dist-datastore/objects/" + objectkey,
                                headers    : headers,
                                requestBody: body
                        ])

        then:
        mc.receivedResponse.code == '405'
        mc.receivedResponse.headers.getCountByName(HttpHeaders.SERVER) == 0
    }

    def "PATCH a new cache object should return 200 response"() {
        given:
        def headers = ['X-TTL': '5']
        def objectkey = UUID.randomUUID().toString()
        def body = objectSerializer.writeObject(new StringValue.Patch("test data"))

        when:
        MessageChain mc =
                deproxy.makeRequest(
                        [
                                method     : 'PATCH',
                                url        : distDatastoreEndpoint + "/powerapi/dist-datastore/objects/" + objectkey,
                                headers    : headers,
                                requestBody: body
                        ])

        then:
        mc.receivedResponse.code == '200'
        mc.receivedResponse.headers.getCountByName(HttpHeaders.SERVER) == 0
    }

    def "PATCH a cache object to an existing key should patch the cached value"() {
        given:
        def headers = ['X-TTL': '5']
        def objectkey = UUID.randomUUID().toString()
        def body = objectSerializer.writeObject(new StringValue.Patch("original value"))
        def newBody = objectSerializer.writeObject(new StringValue.Patch(" patched on value"))

        when: "I make 2 PATCH calls for 2 different values for the same key"
        MessageChain mc1 = deproxy.makeRequest(
                [
                        method     : 'PATCH',
                        url        : distDatastoreEndpoint + "/powerapi/dist-datastore/objects/" + objectkey,
                        headers    : headers,
                        requestBody: body
                ])
        MessageChain mc2 = deproxy.makeRequest(
                [
                        method     : 'PATCH',
                        url        : distDatastoreEndpoint + "/powerapi/dist-datastore/objects/" + objectkey,
                        headers    : headers,
                        requestBody: newBody
                ])

        and: "I get the value for the key"
        MessageChain mc3 = deproxy.makeRequest(
                [
                        method : 'GET',
                        url    : distDatastoreEndpoint + "/powerapi/dist-datastore/objects/" + objectkey,
                        headers: headers
                ])

        then: "The body of the get response should be my second request body"
        mc1.receivedResponse.code == "200"
        mc2.receivedResponse.code == "200"
        objectSerializer.readObject(mc2.receivedResponse.body as byte[]).value == "original value patched on value"
        objectSerializer.readObject(mc3.receivedResponse.body as byte[]).value == "original value patched on value"
        mc1.receivedResponse.headers.getCountByName(HttpHeaders.SERVER) == 0
        mc2.receivedResponse.headers.getCountByName(HttpHeaders.SERVER) == 0
        mc3.receivedResponse.headers.getCountByName(HttpHeaders.SERVER) == 0
    }

    def "when putting cache objects"() {
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
        mc.receivedResponse.code == '202'
        mc.receivedResponse.headers.getCountByName(HttpHeaders.SERVER) == 0
    }

    def "when checking cache object time to live"() {
        given:
        def headers = ['X-TTL': '5']
        def objectkey = UUID.randomUUID().toString()
        def body = objectSerializer.writeObject(new StringValue("test data"))
        MessageChain mc =
                deproxy.makeRequest(
                        [
                                method     : 'PUT',
                                url        : distDatastoreEndpoint + "/powerapi/dist-datastore/objects/" + objectkey,
                                headers    : headers,
                                requestBody: body
                        ])
        mc.receivedResponse.headers.getCountByName(HttpHeaders.SERVER) == 0

        mc =
                deproxy.makeRequest(
                        [
                                method : 'GET',
                                url    : distDatastoreEndpoint + "/powerapi/dist-datastore/objects/" + objectkey,
                                headers: headers
                        ])
        mc.receivedResponse.code == '200'
        mc.receivedResponse.headers.getCountByName(HttpHeaders.SERVER) == 0

        when:
        Thread.sleep(7500)
        mc =
                deproxy.makeRequest(
                        [
                                method : 'GET',
                                url    : distDatastoreEndpoint + "/powerapi/dist-datastore/objects/" + objectkey,
                                headers: headers
                        ])

        then:
        mc.receivedResponse.code == '404'
        mc.receivedResponse.headers.getCountByName(HttpHeaders.SERVER) == 0
    }

    def "when deleting cache objects"() {
        given:
        def headers = ['x-ttl': '1000']
        def objectkey = UUID.randomUUID().toString()
        def body = objectSerializer.writeObject(new StringValue("test data"))
        def url = distDatastoreEndpoint + "/powerapi/dist-datastore/objects/" + objectkey


        when: "Adding the object to the datastore"
        MessageChain mc =
                deproxy.makeRequest(
                        [
                                method     : "PUT",
                                url        : url,
                                headers    : headers,
                                requestBody: body
                        ])

        then: "should report success"
        mc.receivedResponse.code == "202"
        mc.receivedResponse.body == ""
        mc.receivedResponse.headers.getCountByName(HttpHeaders.SERVER) == 0

        when: "checking that it's there"
        mc =
                deproxy.makeRequest(
                        [
                                method : "GET",
                                url    : url,
                                headers: headers
                        ])

        then: "should report that it is"
        mc.receivedResponse.code == "200"
        mc.receivedResponse.body == body
        mc.receivedResponse.headers.getCountByName(HttpHeaders.SERVER) == 0

        when: "deleting the object from the datastore"
        mc =
                deproxy.makeRequest(
                        [
                                method : "DELETE",
                                url    : url,
                                headers: headers,
                        ])

        then: "should report that it was successfully deleted"
        mc.receivedResponse.code == "204"
        mc.receivedResponse.body == ""
        mc.receivedResponse.headers.getCountByName(HttpHeaders.SERVER) == 0

        when: "checking that it's gone"
        mc =
                deproxy.makeRequest(
                        [
                                method : "GET",
                                url    : url,
                                headers: headers,
                        ])

        then: "should report it missing"
        mc.receivedResponse.code == "404"
        mc.receivedResponse.body == ""
        mc.receivedResponse.headers.getCountByName(HttpHeaders.SERVER) == 0
    }

    def "Should not split request headers according to rfc"() {
        def userAgentValue = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_8_4) " +
                "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/29.0.1547.65 Safari/537.36"
        def reqHeaders =
                [
                        "user-agent": userAgentValue,
                        "x-pp-user" : "usertest1, usertest2, usertest3",
                        "accept"    : "application/xml;q=1 , application/json;q=0.5"
                ]

        when: "User sends a request through repose"
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint + "/test", method: 'GET', headers: reqHeaders)

        then:
        mc.handlings.size() == 1
        mc.handlings[0].request.getHeaders().findAll("user-agent").size() == 1
        mc.handlings[0].request.headers['user-agent'] == userAgentValue
        mc.handlings[0].request.getHeaders().findAll("x-pp-user").size() == 1
        mc.handlings[0].request.getHeaders().findAll("accept").size() == 1
    }
}
