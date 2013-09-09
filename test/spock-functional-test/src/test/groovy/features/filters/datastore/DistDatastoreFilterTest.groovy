package features.filters.datastore

import framework.ReposeValveTest
import org.rackspace.gdeproxy.Deproxy
import org.rackspace.gdeproxy.MessageChain

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
                            body: body
                    ])

        when:
        Thread.sleep(7500)
        mc =
            deproxy.makeRequest(
                    [
                            method: 'GET',
                            url:reposeEndpoint + "/powerapi/dist-datastore/objects/" + objectkey,
                            headers:headers,
                            body: body
                    ])

        then:
        mc.receivedResponse.code == '404'

    }

    def "when deleting cache objects"(){
        given:
        def headers = ['X-PP-Host-Key':'temp', 'X-TTL':'50']
        def objectkey = '8e969a44-990b-de49-d894-cf200b7d4c11'
        def body = "test data"

        when:
        MessageChain mc =
            deproxy.makeRequest(
                    [
                            method: method,
                            url:reposeEndpoint + "/powerapi/dist-datastore/objects/" + objectkey,
                            headers:headers,
                            body: body
                    ])

        then:
        mc.receivedResponse.code == expectedCode
        mc.receivedResponse.body == responseBody

        where:
        method   | expectedCode | responseBody
        "PUT"    | '202'        | ""
        "GET"    | '200'        | "test data"
        "DELETE" | '202'        | ""
        "GET"    | '404'        | ""
    }
}
