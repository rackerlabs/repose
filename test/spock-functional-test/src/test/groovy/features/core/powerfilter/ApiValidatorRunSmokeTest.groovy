package features.core.powerfilter

import framework.ReposeValveTest
import framework.category.Smoke
import org.rackspace.gdeproxy.Deproxy
import org.junit.experimental.categories.Category
import org.rackspace.gdeproxy.MessageChain
import org.rackspace.gdeproxy.Response


@Category(Smoke.class)
class ApiValidatorRunSmokeTest extends ReposeValveTest {


    def setupSpec() {
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.getProperty("target.port").toInteger())
    }

    def cleanup() {
        repose.stop()
    }

    def cleanupSpec() {
        deproxy.shutdown()
    }


    def "when request is sent check to make sure it goes through ip-identity and API-Validator filters"() {

        given:
        repose.applyConfigs("/features/core/smoke")
        repose.start()
       // def xmlResp = { request -> return new Response(200, "OK")}

        when:

        MessageChain mc1 =  deproxy.makeRequest([url: reposeEndpoint + "/resource", method: "get",headers:['X-Roles':'role-1','x-trace-request': 'true']])

        then:
        mc1.receivedResponse.getHeaders().names.contains("x-api-validator-time")
        mc1.receivedResponse.getHeaders().names.contains("x-ip-identity-time")

     }



}
