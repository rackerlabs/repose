package features.services.httpconnectionpool
import framework.ReposeValveTest
import framework.category.Slow
import org.junit.experimental.categories.Category
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.Handlers
import org.rackspace.deproxy.MessageChain

class ConnectionPoolDecommissioningTest extends ReposeValveTest {

    /*
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
        def logLines = reposeLogSearch.searchByString("HTTP connection pool default-1 with instance id .* has been created") //default-1 comes from connection pool config
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
        def createdLog = reposeLogSearch.searchByString("HTTP connection pool default-1 with instance id .* has been created") //default-1 comes from connection pool config

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

    @Category(Slow)
    def "under heavy load and constant HTTPClientService reconfigures, should not drop inflight connections"() {

        given: "Repose is up and the HTTPClientService has been configured"
        repose.applyConfigs("features/services/httpconnectionpool/common",
                "features/services/httpconnectionpool/decommissioned/first")
        repose.start()
        waitUntilReadyToServiceRequests()

        and: "Alot of concurrent users are making requests to Repose"
        List<Thread> clientThreads = new ArrayList<Thread>()

        Random rand = new Random()
        int totalErrors = 0

        for (x in 1..50) {
            println("Starting client: " + x)
            def thread = Thread.start {
                for (y in 1..5) {
                    MessageChain messageChain = deproxy.makeRequest(url:reposeEndpoint, defaultHandler: Handlers.Delay(rand.nextInt(15000)))
                    if (messageChain.receivedResponse.code != "200") {
                        println("ERROR: call received an error response")
                        println("RESPONSE: " + messageChain.receivedResponse.body)
                        totalErrors++
                    }
                }
            }
            clientThreads.add(thread)
        }

        and: "The HTTP Client Service is continuously being reconfigured"
        def keepReconfiguring = true
        def reconfigureCount = 0
        def reconfigureThread = Thread.start {
            while (keepReconfiguring) {
                println("Reconfiguring...")
                sleep(16000) //TODO: better strategy to know when Repose has been reconfigured
                if (reconfigureCount % 2) {
                    repose.updateConfigs("features/services/httpconnectionpool/decommissioned/second")
                } else {
                    repose.updateConfigs("features/services/httpconnectionpool/decommissioned/first")
                }
                reconfigureCount++
            }
        }

        when: "All clients have completed their calls"
        clientThreads*.join()

        and: "We stop reconfiguring madness in Repose"
        keepReconfiguring = false
        reconfigureThread.join()

        then: "All client calls should have succeeded"
        totalErrors == 0
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
