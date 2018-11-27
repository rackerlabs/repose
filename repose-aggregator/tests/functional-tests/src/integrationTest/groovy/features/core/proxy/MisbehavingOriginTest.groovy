/*
 * _=_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_=
 * Repose
 * _-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-
 * Copyright (C) 2010 - 2015 Rackspace US, Inc.
 * _-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * =_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_=_
 */
package features.core.proxy

import org.openrepose.framework.test.ReposeValveTest
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain

class MisbehavingOriginTest extends ReposeValveTest {

    static volatile boolean running = true
    static NullLoop loop = new NullLoop()

    def setupSpec() {
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

    def cleanupSpec() {
        running = false
    }

    def "returns a 502 when the origin service doesn't respond with proper http"() {
        given: "something is going to do bad things"

        when: "Request goes through repose"
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint)

        then: "repose should return a 502 Internal Server Error"
        mc.receivedResponse != null
        mc.receivedResponse.code == "502"
    }

    private static class NullLoop implements Runnable {

        ServerSocket serverSocket = null;
        public int port = 0

        @Override
        void run() {

            serverSocket = new ServerSocket(0)
            port = serverSocket.getLocalPort()

            while (running) {
                try {
                    Socket server = serverSocket.accept()
                    println("OPERATING")
                    new PrintStream(server.outputStream).println("null")
                    server.close()
                } catch (Exception e) {
                    println("OH NOES SOMETHING: $e")
                }
            }
        }
    }
}
