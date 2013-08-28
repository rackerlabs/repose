package features.services.datastore

import framework.ReposeValveTest
import org.rackspace.gdeproxy.Deproxy
import org.rackspace.gdeproxy.MessageChain

/**

 TODO: change from ReposeValveTest to ReposeGlassfishTest.  In it, set the following:
 reposeGlassfishEndpoint1 = properties.getProperty("repose.endpoint_glassfish_1")
 reposeGlassfishEndpoint2 = properties.getProperty("repose.endpoint_glassfish_2")

 */
class DistDatastoreServiceGlassfishTest  extends ReposeValveTest {
    boolean isFailedStart = false
    def reposeGlassfishEndpoint1
    def reposeGlassfishEndpoint2

    def setup(){
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.getProperty("target.port").toInteger())
    }

    def cleanup(){
        if (deproxy)
            deproxy.shutdown()

        if(!getIsFailedStart())
            repose.stop()
        setIsFailedStart(false)
        sleep(5000)
    }

    def "when configured with DD service on Glassfish, repose should start and successfully execute calls" () {
        given:
        repose.applyConfigs("features/services/datastore")
        repose.start()
        waitUntilReadyToServiceRequests()

        when:
        MessageChain mc = deproxy.makeRequest([url:reposeEndpoint + "/cluster",headers:['x-trace-request': 'true']])

        then:
        mc.receivedResponse.code == '200'
        mc.handlings.size() == 1
    }

    def "when configured with DD service and filter on Glassfish, repose should not start" () {
        given:
        repose.applyConfigs("features/badconfig/datastore/servicefilter")
        setIsFailedStart(true)

        def MessageChain mc


        when:
        try{
            repose.start()
            mc = deproxy.makeRequest([url:reposeEndpoint + "/cluster",headers:['x-trace-request': 'true']])
        } catch(Exception e){

        }

        then:
        mc == null
    }

    def "when configured with at least 2 nodes, limits are shared and no 'damaged node' errors are recorded"() {
        given:
        repose.applyConfigs("features/services/glassfish_datastore")
        repose.start()
        waitUntilReadyToServiceRequests()
        def user= UUID.randomUUID().toString();

        when:
        //rate limiting is set to 3 an hour
        for (int i = 0; i < 3; i++) {
            def path= UUID.randomUUID().toString();
            MessageChain mc = deproxy.makeRequest(reposeGlassfishEndpoint1+ "/test", 'GET', ['X-PP-USER': user])
            mc.receivedResponse.code == 200
        }
        //this call should rate limit when calling the second node
        MessageChain mc = deproxy.makeRequest(reposeGlassfishEndpoint2+ "/test", 'GET', ['X-PP-USER': user])

        then:
        mc.receivedResponse.code == 413

        def List<String> logMatches = reposeLogSearch.searchByString("damaged node");
        logMatches.size() == 0

    }

}