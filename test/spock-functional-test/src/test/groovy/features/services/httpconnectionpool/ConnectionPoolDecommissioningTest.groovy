package features.services.httpconnectionpool
import framework.ReposeValveTest
import framework.category.Slow
import org.junit.experimental.categories.Category
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.Handlers
import org.rackspace.deproxy.MessageChain

class ConnectionPoolDecommissioningTest extends ReposeValveTest {

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
                "features/services/httpconnectionpool/decommissioned/onepool")
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
                "features/services/httpconnectionpool/decommissioned/onepool")
        repose.start()

        when: "Repose is up and the HTTPClientService has been reconfigured"
        waitUntilReadyToServiceRequests()
        def createdLog = reposeLogSearch.searchByString("HTTP connection pool default-1 with instance id .* has been created") //default-1 comes from connection pool config

        and: "The HttpClientService is reconfigured"
        repose.updateConfigs("features/services/httpconnectionpool/decommissioned/onepool_reconfig")

        then: "The HttpClientService should log the first pool as destroyed"
        def uuid = createdLog.get(0).tokenize(" ").reverse().get(3) //reverse done to account for different log formatting
        def logLines = reposeLogSearch.searchByString("HTTP connection pool " + uuid + " has been destroyed.")
        logLines.size() == 1

        cleanup:
        repose.stop()
    }

    @Category(Slow)
    def "active connections should stay alive during config changes and log an error"() {
        given:
        def MessageChain messageChain

        repose.applyConfigs("features/services/httpconnectionpool/common",
                "features/services/httpconnectionpool/decommissioned/onepool")
        repose.start()
        waitUntilReadyToServiceRequests()

        when:
        def thread = Thread.start {
            messageChain = deproxy.makeRequest(url: reposeEndpoint, method: 'GET', defaultHandler: Handlers.Delay(25000))
        }

        and:
        repose.updateConfigs("features/services/httpconnectionpool/decommissioned/onepool_reconfig")
        thread.join()

        then:
        messageChain.receivedResponse.code == "200"

        and:
        def logLines = reposeLogSearch.searchByString("Failed to shutdown connection pool client")
        logLines.size() > 0

        cleanup:
        repose.stop()
    }

    @Category(Slow)
    def "under heavy load and constant HTTPClientService reconfigures, should not drop in use connections"() {

        given: "Repose is up and the HTTPClientService has been configured"
        repose.applyConfigs("features/services/httpconnectionpool/common",
                "features/services/httpconnectionpool/decommissioned/" + firstConfig)
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
                    MessageChain messageChain = deproxy.makeRequest(url:reposeEndpoint, defaultHandler: Handlers.Delay(500 + rand.nextInt(1000)))
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
                    repose.updateConfigs("features/services/httpconnectionpool/decommissioned/" + secondConfig)
                } else {
                    repose.updateConfigs("features/services/httpconnectionpool/decommissioned/" + firstConfig)
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

        cleanup:
        repose.stop()

        where:
        firstConfig | secondConfig
        "onepool"   | "onepool_reconfig"
        "twopool"   | "twopool_reconfig"
    }
}
