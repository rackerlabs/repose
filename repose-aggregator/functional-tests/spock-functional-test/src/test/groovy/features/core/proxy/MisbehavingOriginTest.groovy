package features.core.proxy

import framework.ReposeValveTest
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain

class MisbehavingOriginTest extends ReposeValveTest {

    volatile boolean running = true;
    NullLoop loop = new NullLoop()

    def setupSpec() {
    }

    def cleanupSpec() {
    }

    def setup() {
        //Create a runloop thing

        Thread t = new Thread(loop)
        t.start()

        deproxy = new Deproxy()

        properties.targetPort = loop.port


        def params = properties.getDefaultTemplateParams()
        repose.configurationProvider.cleanConfigDirectory()
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/core/proxy", params) //just a very simple config
        repose.start([waitOnJmxAfterStarting: true])
    }

    def cleanup() {
        if (repose) {
            repose.stop()
        }
        if (deproxy) {
            deproxy.shutdown()
        }
        running = false
    }

    //TODO: I want this to return a 502, but I cannot, because repose internals can't deal with other exceptions
    def "returns a 500 when the origin service doesn't respond with proper http"() {
        given: "something is going to do bad things"

        when: "Request goes through repose"
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint)

        then: "repose should return a 500 Internal Server Error"
        mc.receivedResponse != null
        mc.receivedResponse.code == "500"
    }

    private class NullLoop implements Runnable {

        ServerSocket serverSocket = null;
        public int port = 0

        @Override
        void run() {

            serverSocket = new ServerSocket(0)
            port = serverSocket.getLocalPort()

            while(running) {
                try {
                    Socket server =serverSocket.accept()
                    println("OPERATING")
                    new PrintStream(server.outputStream).println("null")
                    server.close()
                } catch(Exception e) {
                    println("OH NOES SOMETHING: $e")
                }
            }
        }
    }
}
