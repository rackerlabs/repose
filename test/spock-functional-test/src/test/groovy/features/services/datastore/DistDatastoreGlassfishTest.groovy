package features.services.datastore

import framework.ReposeGlassfishLauncher
import framework.ReposeTest
import org.rackspace.gdeproxy.Deproxy
import org.rackspace.gdeproxy.MessageChain


class DistDatastoreGlassfishTest extends ReposeTest {

    // TODO
    // 1. Pull root war to spock directory
    // 2.


    def setupSpec() {
    }


    def "should startup in glassfish"() {
        given:
        repose.start()
        deproxy = new Deproxy()
        deproxy.addEndpoint(12345)

        when:
        MessageChain mc = deproxy.makeRequest([url:reposeEndpoint + "/cluster",headers:['x-trace-request': 'true']])

        then:
        mc.receivedResponse.code == "200"
    }


}
