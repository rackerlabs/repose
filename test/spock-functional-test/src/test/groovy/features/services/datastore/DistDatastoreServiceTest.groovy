package features.services.datastore

import framework.ReposeValveTest
import org.rackspace.gdeproxy.Deproxy
import org.rackspace.gdeproxy.MessageChain

/**
 * Created with IntelliJ IDEA.
 * User: dimi5963
 * Date: 6/26/13
 * Time: 2:45 PM
 */
class DistDatastoreServiceTest extends ReposeValveTest {
    boolean isFailedStart = false

    def setupSpec(){
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.getProperty("target.port").toInteger())
    }

    def cleanupSpec() {
        if (deproxy)
            deproxy.shutdown()
    }

    def cleanup(){
        sleep(5000)
        if(!getIsFailedStart())
            repose.stop()
        setIsFailedStart(false)

    }

    def "when configured with DD service, repose should start and successfully execute calls" () {
        given:
        repose.applyConfigs("features/services/datastore")
        repose.start()
        sleep(15000)


        when:
        MessageChain mc = deproxy.makeRequest([url:reposeEndpoint + "/cluster",headers:['x-trace-request': 'true']])

        then:
        mc.receivedResponse.code == '200'
        mc.handlings.size() == 1
    }

    def "when configured with DD service and filter, repose should not start" () {
        given:
        repose.applyConfigs("features/badconfig/datastore/servicefilter")
        setIsFailedStart(true)
        try{
            repose.start()
        } catch(java.util.concurrent.TimeoutException e){

        }
        sleep(15000)


        when:
        MessageChain mc = deproxy.makeRequest([url:reposeEndpoint + "/cluster",headers:['x-trace-request': 'true']])

        then:
        mc.receivedResponse.code == '503'
        mc.handlings.size() == 0
    }

    def "when configured with DD service, number of ports listened by repose process should equal to DD service nodes" () {
        given:
        repose.applyConfigs("features/services/datastore")
        repose.start()
        sleep(15000)
        def port_3999 = "lsof -i :3999".execute()
        port_3999.waitFor()
        def port_4999 = "lsof -i :4999".execute()
        port_4999.waitFor()

        when:
        MessageChain mc = deproxy.makeRequest([url:reposeEndpoint + "/cluster",headers:['x-trace-request': 'true']])

        then:
        mc.receivedResponse.code == '200'
        mc.handlings.size() == 1
        !port_3999.in.text.isEmpty()
        !port_4999.in.text.isEmpty()

    }

    def "when configured with DD filter, repose should start and log a warning" () {
        given:
        repose.applyConfigs("features/filters/datastore")
        repose.start()
        sleep(15000)

        when:
        MessageChain mc = deproxy.makeRequest([url:reposeEndpoint + "/cluster",headers:['x-trace-request': 'true']])

        then:
        mc.receivedResponse.code == '200'
        mc.handlings.size() == 1

    }

    def "when configured with DD filter and adding a service, repose should log a warning and continue running with previous config" () {

    }

    def "when configured with DD service and adding a filter, repose should log a warning and continue running with previous config" () {

    }
}
