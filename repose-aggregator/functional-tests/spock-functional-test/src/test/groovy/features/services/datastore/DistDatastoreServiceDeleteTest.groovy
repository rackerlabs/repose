package features.services.datastore

import org.openrepose.commons.utils.io.ObjectSerializer
import framework.ReposeValveTest
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain
import org.rackspace.deproxy.PortFinder


class DistDatastoreServiceDeleteTest extends ReposeValveTest {

    def DD_URI
    def DD_HEADERS = ['X-PP-Host-Key':'temp', 'X-TTL':'10']
    def BODY = ObjectSerializer.instance().writeObject('test body')
    def KEY
    def DD_PATH = "/powerapi/dist-datastore/objects/"
    static def distDatastoreEndpoint

    def setupSpec() {
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort)
        int dataStorePort1 = PortFinder.Singleton.getNextOpenPort()
        int dataStorePort2 = PortFinder.Singleton.getNextOpenPort()

        distDatastoreEndpoint = "http://localhost:${dataStorePort1}"

        def params = properties.getDefaultTemplateParams()
        params += [
                'datastorePort1' : dataStorePort1,
                'datastorePort2' : dataStorePort2
        ]
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/services/datastore/", params)
        repose.start()
        repose.waitForNon500FromUrl(reposeEndpoint, 120)
    }

    def setup() {
        DD_URI = distDatastoreEndpoint + DD_PATH
        KEY = UUID.randomUUID().toString()
    }

    def cleanupSpec() {
        repose.stop()
        deproxy.shutdown()
    }


    def "DELETE of existing item in datastore should return 204 and no longer be available"(){
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
        mc.receivedResponse.code == "204"
        mc.receivedResponse.body == ""

        when: "checking that it is no longer available in the datastore"
        mc = deproxy.makeRequest([method: 'GET', url:DD_URI + KEY, headers:DD_HEADERS])

        then: "should report it missing"
        mc.receivedResponse.code == "404"
        mc.receivedResponse.body == ""
    }

    def "DELETE an item that is not in the datastore returns a 204"() {
        when:
        MessageChain mc = deproxy.makeRequest(method: "DELETE", url:DD_URI + UUID.randomUUID().toString(), headers:DD_HEADERS)

        then:
        mc.receivedResponse.code == "204"
    }

    def "DELETE to invalid target will return 404"() {

        when:
        MessageChain mc = deproxy.makeRequest([method: 'DELETE', url:distDatastoreEndpoint+"/invalid/target", headers:DD_HEADERS])

        then:
        mc.receivedResponse.code == '404'
    }

    def "DELETE of invalid key fails 204 No Content"() {

        given:
        def badKey = "////////" + UUID.randomUUID().toString()

        when: "I attempt to get the value from cache"
        MessageChain mc = deproxy.makeRequest([method: 'DELETE', url:distDatastoreEndpoint, path:DD_PATH + badKey, headers:DD_HEADERS])

        then:
        mc.receivedResponse.code == '204'
    }


}
