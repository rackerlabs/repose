package features.services.datastore
import com.rackspace.papi.commons.util.io.ObjectSerializer
import framework.ReposeValveTest
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain
import org.rackspace.deproxy.PortFinder

class DistDatastoreServiceGetTest extends ReposeValveTest {

    def DD_URI
    def DD_HEADERS = ['X-PP-Host-Key':'temp', 'X-TTL':'10']
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

    def "GET with no key returns 500 INTERNAL SERVER ERROR"() {
        when:
        MessageChain mc = deproxy.makeRequest([method: 'GET', url:DD_URI, headers:DD_HEADERS])

        then:
        mc.receivedResponse.code == '500'
        mc.receivedResponse.body.toString().contains("Cache key specified is invalid")
    }

    def "GET with missing X-PP-Host-Key returns a 500 INTERNAL SERVER ERROR"() {

        when:
        MessageChain mc = deproxy.makeRequest([method: 'GET', url:DD_URI + KEY])

        then:
        mc.receivedResponse.code == '500'
        mc.receivedResponse.body.toString().contains("No host key specified in header x-pp-host-key")
    }

    def "GET of invalid key fails with 500 INTERNAL SERVER ERROR"() {

        given:
        def badKey = "////////" + UUID.randomUUID().toString()

        when: "I attempt to get the value from cache"
        MessageChain mc = deproxy.makeRequest([method: 'GET', url:distDatastoreEndpoint, path:DD_PATH + badKey, headers:DD_HEADERS])

        then:
        mc.receivedResponse.code == '500'
    }

    def "GET of key after time to live has expired should return a 404"(){

        def body = ObjectSerializer.instance().writeObject('foo')
        given:
        MessageChain mc = deproxy.makeRequest([method: 'PUT', url:DD_URI + KEY, headers:['X-PP-Host-Key':'temp', 'X-TTL':'2'], requestBody: body])

        when:
        mc = deproxy.makeRequest([method: 'GET', url:DD_URI + KEY, headers:DD_HEADERS])

        then:
        mc.receivedResponse.code == '200'
        mc.receivedResponse.body == body

        when: "I wait long enough for the TTL of the item to expire"
        Thread.sleep(3000)

        and: "I get the key again"
        mc = deproxy.makeRequest([method: 'GET', url:DD_URI + KEY, headers:DD_HEADERS])

        then:
        mc.receivedResponse.code == '404'
    }


}
