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

    def "when configured with DD service, repose should start and successfully execute calls" () {
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

    def "when configured with DD service and filter, repose should not start" () {
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

/*
    //why are we checking this?
    @Ignore
    def "when configured with DD service, number of ports listened by repose process should equal to DD service nodes" () {
        given:
        repose.applyConfigs("features/services/datastore")
        repose.start()
        //sleep(15000)
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
*/

    def "when configured with DD filter, repose should start and log a warning" () {
        given:
        cleanLogDirectory()
        repose.applyConfigs("features/filters/datastore")
        repose.start()
        def user= UUID.randomUUID().toString();

        when:
        MessageChain mc = deproxy.makeRequest([url:reposeEndpoint,headers:['X-PP-USER': user, 'X-PP-Groups' : "BETA_Group"]])

        then:
        mc.receivedResponse.code == '200'
        mc.handlings.size() == 1
        def List<String> logMatches = reposeLogSearch.searchByString(
                "Use of the dist-datastore filter is deprecated. Please use the distributed datastore service.");
        logMatches.size() == 1
    }

    def "when configured with DD filter and adding a service, repose should log a warning and continue running with previous config" () {
        given:
        def List<String> logMatchesTrue
        def List<String> logMatchesFalse
        cleanLogDirectory()
        repose.applyConfigs("features/filters/datastore")
        repose.start()
        logMatchesFalse = reposeLogSearch.searchByString(
                "The distributed datastore filter and service can not be used at the same time, within the same cluster. Please check your configuration.");
        repose.updateConfigs("features/badconfig/datastore/servicefilter")
        logMatchesTrue = reposeLogSearch.searchByString(
                "The distributed datastore filter and service can not be used at the same time, within the same cluster. Please check your configuration.");
        def user= UUID.randomUUID().toString();

        when:
        MessageChain mc = deproxy.makeRequest([url:reposeEndpoint,headers:['X-PP-USER': user, 'X-PP-Groups' : "BETA_Group"]])

        then:
        mc.receivedResponse.code == '200'
        mc.handlings.size() == 1
        logMatchesTrue.size() > logMatchesFalse.size()
    }

    def "when configured with DD service and adding a filter, repose should log a warning and continue running with previous config" () {
        given:
        def List<String> logMatchesTrue
        def List<String> logMatchesFalse
        cleanLogDirectory()
        repose.applyConfigs("features/services/datastore")
        repose.start()
        waitUntilReadyToServiceRequests()
        logMatchesFalse = reposeLogSearch.searchByString(
                "The distributed datastore filter and service can not be used at the same time, within the same cluster. Please check your configuration.");
        repose.updateConfigs("features/badconfig/datastore/servicefilter")
        logMatchesTrue = reposeLogSearch.searchByString(
                "The distributed datastore filter and service can not be used at the same time, within the same cluster. Please check your configuration.");
        def user= UUID.randomUUID().toString();

        when:
        MessageChain mc = deproxy.makeRequest([url:reposeEndpoint + "/cluster",headers:['X-PP-USER': user, 'X-PP-Groups' : "BETA_Group"]])

        then:
        mc.receivedResponse.code == '200'
        mc.handlings.size() == 1
        logMatchesTrue.size() > logMatchesFalse.size()
    }
}
