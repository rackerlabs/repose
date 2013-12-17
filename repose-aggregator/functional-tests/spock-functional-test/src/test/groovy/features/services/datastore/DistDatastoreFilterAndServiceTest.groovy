package features.services.datastore
import framework.ReposeValveTest
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain

import java.util.concurrent.TimeoutException

class DistDatastoreFilterAndServiceTest extends ReposeValveTest {
    boolean isFailedStart = false
    def String warningLog = "The distributed datastore filter and service can not be used at the same time, within the same cluster. Please check your configuration."

    def setupSpec() {
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort)
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

        when:
        repose.start()

        then:
        thrown TimeoutException
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

        when: "I start Repose with a DD Filter"
        repose.applyConfigs("features/filters/datastore")
        repose.start()

        and: "I check for warning log messages"
        logMatchesFalse = reposeLogSearch.searchByString(warningLog)

        then: "I should not see a warning log about usage of filter and service"
        logMatchesFalse.size() == 0

        when: "I update Repose with a config that includes DD service and DD filter"
        repose.updateConfigs("features/services/datastore/badconfig")

        and: "I check for warning log messages"
        logMatchesTrue = reposeLogSearch.searchByString(warningLog)

        then: "I should have warning logs telling me the DD service and filter can't be used"
        logMatchesTrue.size() > 1

        when: "I make a request to Repose"
        def user= UUID.randomUUID().toString();
        MessageChain mc = deproxy.makeRequest([url:reposeEndpoint,headers:['X-PP-USER': user, 'X-PP-Groups' : "BETA_Group"]])

        then: "Repose is still in a valid state and returns a 200"
        mc.receivedResponse.code == '200'
        mc.handlings.size() == 1
    }

    def "when configured with DD service and adding a filter, repose should log a warning and continue running with previous config" () {
        given:
        def List<String> logMatchesTrue
        def List<String> logMatchesFalse
        cleanLogDirectory()

        when: "I start Repose with a DD Service"
        repose.applyConfigs("features/services/datastore")
        repose.start()
        waitUntilReadyToServiceRequests()

        and: "I check for warning log messages"
        logMatchesFalse = reposeLogSearch.searchByString(warningLog)

        then: "I should not see a warning log about usage of filter and service"
        logMatchesFalse.size() == 0

        when: "I update Repose with a config that includes DD service and DD filter"
        repose.updateConfigs("features/services/datastore/badconfig")

        and: "I check for warning log messages"
        logMatchesTrue = reposeLogSearch.searchByString(warningLog)

        then: "I should have warning logs telling me the DD service and filter can't be used"
        logMatchesTrue.size() > 1

        when: "I make a request to Repose"
        def user= UUID.randomUUID().toString();
        MessageChain mc = deproxy.makeRequest([url:reposeEndpoint,headers:['X-PP-USER': user, 'X-PP-Groups' : "BETA_Group"]])

        then: "Repose is still in a valid state and returns a 200"
        mc.receivedResponse.code == '200'
        mc.handlings.size() == 1
    }
}
