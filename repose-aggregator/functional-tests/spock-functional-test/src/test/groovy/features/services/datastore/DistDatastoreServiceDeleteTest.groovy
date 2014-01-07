package features.services.datastore

import framework.ReposeValveTest
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain


class DistDatastoreServiceDeleteTest extends ReposeValveTest {

    def DD_URI
    def DD_HEADERS = ['X-PP-Host-Key':'temp', 'X-TTL':'10']
    def BODY = "test body"
    def KEY
    def DD_PATH = "/powerapi/dist-datastore/objects/"
    static def distDatastoreEndpoint = "http://localhost:4999"

    def setupSpec() {
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.getProperty("target.port").toInteger())

        repose.applyConfigs("features/services/datastore")
        repose.start()
        waitUntilReadyToServiceRequests()
    }

    def setup() {
        DD_URI = distDatastoreEndpoint + DD_PATH
        KEY = UUID.randomUUID().toString()
    }

    def cleanupSpec() {
        repose.stop()
        deproxy.shutdown()
    }


    def "DELETE of existing item in datastore should return 202 and no longer be available"(){
        when: "Adding the object to the datastore"
        MessageChain mc = deproxy.makeRequest([method: 'PUT', url:DD_URI + KEY, headers:DD_HEADERS, requestBody: BODY])

        and: "checking that it's there"
        mc = deproxy.makeRequest([method: 'GET', url:DD_URI + KEY, headers:DD_HEADERS])

        then: "should report that it is"
        mc.receivedResponse.code == "200"
        mc.receivedResponse.body == BODY

        when: "deleting the object from the datastore"
        mc = deproxy.makeRequest(method: "DELETE", url:DD_URI + KEY, headers:DD_HEADERS)

        then: "should report that it was successfully deleted"
        mc.receivedResponse.code == "202"
        mc.receivedResponse.body == ""

        when: "checking that it is no longer available in the datastore"
        mc = deproxy.makeRequest([method: 'GET', url:DD_URI + KEY, headers:DD_HEADERS])

        then: "should report it missing"
        mc.receivedResponse.code == "404"
        mc.receivedResponse.body == ""
    }

    def "DELETE an item that is not in the datastore returns a 202"() {
        when:
        MessageChain mc = deproxy.makeRequest(method: "DELETE", url:DD_URI + UUID.randomUUID().toString(), headers:DD_HEADERS)

        then:
        mc.receivedResponse.code == "202"
    }

}