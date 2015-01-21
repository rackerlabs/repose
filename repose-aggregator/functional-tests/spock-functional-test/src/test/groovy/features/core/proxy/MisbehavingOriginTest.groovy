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

//    @Unroll("When sending a #reqMethod through repose")
//    def "should return 413 on request body that is too large"(){
//        given: "I have a request body that exceed the header size limit"
//        def body = makeLargeString(32100)
//
//        when: "I send a request to REPOSE with my request body"
//        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint, requestBody: body, method: reqMethod)
//
//        then: "I get a response of 413"
//        mc.receivedResponse.code == "413"
//        mc.handlings.size() == 0
//
//        where:
//        reqMethod | _
//        "POST"    | _
//        "PUT"     | _
//        "DELETE"  | _
//        "PATCH"   | _
//    }

    def "returns a 502 when the origin service doesn't respond with proper http"() {
        given: "something is going to do bad things"

        when: "Request goes through repose"
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint)

        then: "repose should return a 502 bad gateway"
        mc.receivedResponse != null
        mc.receivedResponse.code == "502"
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
