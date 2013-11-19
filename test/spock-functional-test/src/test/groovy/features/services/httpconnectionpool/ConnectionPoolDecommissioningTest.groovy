package features.services.httpconnectionpool

import framework.ReposeValveTest
import framework.category.Slow
import org.junit.experimental.categories.Category
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.Handlers
import org.rackspace.deproxy.MessageChain

class ConnectionPoolDecommissioningTest extends ReposeValveTest {

    /*
    TODO:    1. log when pools are created or removed
    TODO:    2. add mbeans for pools with current open connections
    TODO:    3. time spent on calls within connections,
    */

    def setup() {
        cleanLogDirectory()
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.getProperty("target.port").toInteger())
    }

    def cleanup() {
        if (deproxy)
            deproxy.shutdown()
    }

    def "on startup, HttpClientService should log out when pools are created"() {

        given:
        repose.applyConfigs("features/services/httpconnectionpool/common",
                "features/services/httpconnectionpool/decommissioned/first")
        repose.start()

        when: "Repose is up and the HTTPClientService has been configured"
        waitUntilReadyToServiceRequests()

        then: "HTTPClientService has logged that the conn pool is created"
        def logLines = reposeLogSearch.searchByString("HTTP connection pool default-1 with unique id .* has been created") //default-1 comes from connection pool config
        logLines.size() == 1

        cleanup:
        repose.stop()
    }

    def "when Repose is reconfigured and a pool is decommissioned, then destroyed after no registered users"() {

        given:
        repose.applyConfigs("features/services/httpconnectionpool/common",
                "features/services/httpconnectionpool/decommissioned/first")
        repose.start()

        when: "Repose is up and the HTTPClientService has been reconfigured"
        waitUntilReadyToServiceRequests()
        def createdLog = reposeLogSearch.searchByString("HTTP connection pool default-1 with unique id .* has been created") //default-1 comes from connection pool config

        and: "The HttpClientService is reconfigured"
        repose.updateConfigs("features/services/httpconnectionpool/decommissioned/second")

        then: "The HttpClientService should log the first pool as destroyed"
        def uuid = createdLog.get(0).tokenize(" ").reverse().get(3) //reverse done to account for different log formatting
        def logLines = reposeLogSearch.searchByString("HTTP connection pool " + uuid + " has been destroyed.")
        logLines.size() == 1
    }

    @Category(Slow)
    def "active connections should stay alive during config changes"() {
        given:
        def MessageChain messageChain

        repose.applyConfigs("features/services/httpconnectionpool/common",
                "features/services/httpconnectionpool/decommissioned/first")
        repose.start()
        waitUntilReadyToServiceRequests()

        when:
        def thread = Thread.start {
            messageChain = deproxy.makeRequest(url: reposeEndpoint, method: 'GET', defaultHandler: Handlers.Delay(25000))
        }

        and:
        repose.updateConfigs("features/services/httpconnectionpool/decommissioned/second")
        thread.join()

        then:
        messageChain.receivedResponse.code == "200"
    }

    /*
   * need a user to do these actions
   * what is considered inactive?
   * what constitutes being decommissioned?
   * config
   * make a connection
   * reconfig
   * check connection liveliness (connection "leasing value")
   * check new connection liveliness jic
   * */

    def "connections that have been decommissioned but still in use should log an error"() {
/*
* as above but also check logs for right error
* */
    }

    /*probably test that connections close right when not in use jic*/
}
