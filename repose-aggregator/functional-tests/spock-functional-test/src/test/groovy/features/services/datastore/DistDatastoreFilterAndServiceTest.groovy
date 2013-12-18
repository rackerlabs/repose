package features.services.datastore

import framework.ReposeValveTest
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain

class DistDatastoreFilterAndServiceTest extends ReposeValveTest {
    boolean isFailedStart = false

     def setupSpec() {
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.getProperty("target.port").toInteger())
    }

    def cleanupSpec() {
        if (deproxy)
            deproxy.shutdown()

    }

    def cleanup(){
        if(!getIsFailedStart())
            repose.stop()
        setIsFailedStart(false)
    }

    def "when configured with DD service and filter, repose should not start" () {
        given:
        repose.applyConfigs("features/services/datastore/badconfig")
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
        repose.updateConfigs("features/services/datastore/badconfig")
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
        repose.updateConfigs("features/services/datastore/badconfig")
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
