package features.filters.datastore

import framework.ReposeValveTest
import org.rackspace.gdeproxy.Deproxy
import org.rackspace.gdeproxy.MessageChain
import spock.lang.Unroll

/**
 * User: dimi5963
 * Date: 9/9/13
 * Time: 10:55 AM
 */
class DistDatastoreFilterTest  extends ReposeValveTest {
    boolean isFailedStart = false


    def setupSpec() {
        repose.applyConfigs(
                "features/filters/datastore/")
        repose.start()
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.getProperty("target.port").toInteger())

    }

    def cleanupSpec() {
        repose.stop()
        deproxy.shutdown()
    }

    def "when putting cache objects" () {
        given:
        def headers = ['X-PP-Host-Key':'temp', 'X-TTL':'5']
        def objectkey = '8e969a44-990b-de49-d894-cf200b7d4c11'
        def body = "test data"

        when:
        MessageChain mc =
            deproxy.makeRequest(
                    [
                            method: 'PUT',
                            url:reposeEndpoint + "/powerapi/dist-datastore/objects/" + objectkey,
                            headers:headers,
                            body: body
                    ])

        then:
        mc.receivedResponse.code == '202'
    }

    def "when checking cache object time to live"(){
        given:
        def headers = ['X-PP-Host-Key':'temp', 'X-TTL':'5']
        def objectkey = '8e969a44-990b-de49-d894-cf200b7d4c11'
        def body = "test data"
        MessageChain mc =
            deproxy.makeRequest(
                    [
                            method: 'PUT',
                            url:reposeEndpoint + "/powerapi/dist-datastore/objects/" + objectkey,
                            headers:headers,
                            requestBody: body
                    ])
        mc =
            deproxy.makeRequest(
                    [
                            method: 'GET',
                            url:reposeEndpoint + "/powerapi/dist-datastore/objects/" + objectkey,
                            headers:headers
                    ])
        mc.receivedResponse.code == '200'

        when:
        Thread.sleep(7500)
        mc =
            deproxy.makeRequest(
                    [
                            method: 'GET',
                            url:reposeEndpoint + "/powerapi/dist-datastore/objects/" + objectkey,
                            headers:headers
                    ])

        then:
        mc.receivedResponse.code == '404'

    }

    def "when deleting cache objects"(){
        given:
        def headers = ['X-PP-Host-Key':'temp', 'x-ttl':'1000']
        def objectkey = '8e969a44-990b-de49-d894-cf200b7d4c11'
        def body = "test data"
        def url = reposeEndpoint + "/powerapi/dist-datastore/objects/" + objectkey



        when: "Adding the object to the datastore"
        MessageChain mc =
            deproxy.makeRequest(
                    [
                            method: "PUT",
                            url:url,
                            headers:headers,
                            requestBody: body
                    ])

        then: "should report success"
        mc.receivedResponse.code == "202"
        mc.receivedResponse.body == ""



        when: "checking that it's there"
        mc =
            deproxy.makeRequest(
                    [
                            method: "GET",
                            url:url,
                            headers:headers
                    ])

        then: "should report that it is"
        mc.receivedResponse.code == "200"
        mc.receivedResponse.body == body



        when: "deleting the object from the datastore"
        mc =
            deproxy.makeRequest(
                    [
                            method: "DELETE",
                            url:url,
                            headers:headers,
//                            body: body
                    ])

        then: "should report that it was successfully deleted"
        mc.receivedResponse.code == "202"
        mc.receivedResponse.body == ""



        when: "checking that it's gone"
        mc =
            deproxy.makeRequest(
                    [
                            method: "GET",
                            url:url,
                            headers:headers,
//                            body: body
                    ])

        then: "should report it missing"
        mc.receivedResponse.code == "404"
        mc.receivedResponse.body == ""
    }
}