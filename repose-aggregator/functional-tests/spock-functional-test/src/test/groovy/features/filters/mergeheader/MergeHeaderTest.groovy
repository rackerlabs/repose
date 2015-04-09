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
package features.filters.mergeheader

import framework.ReposeValveTest
import org.eclipse.jetty.server.Handler
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.server.ServerConnector
import org.eclipse.jetty.server.handler.DefaultHandler
import org.eclipse.jetty.server.handler.HandlerList
import org.eclipse.jetty.server.session.SessionHandler
import org.eclipse.jetty.servlet.ServletHandler
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain
import spock.lang.Ignore

import javax.servlet.ServletException
import javax.servlet.http.HttpServlet
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

class MergeHeaderTest extends ReposeValveTest {

    volatile boolean running = true
    BasicLoop loop = new BasicLoop()

    def setup() {
        Thread t = new Thread(loop)
        t.start()

        deproxy = new Deproxy()
        properties.targetPort = loop.port
        def params = properties.getDefaultTemplateParams()
        repose.configurationProvider.cleanConfigDirectory()
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/filters/mergeheader", params) //just a very simple config
        repose.start([waitOnJmxAfterStarting: true])

    }

    def cleanupSpec() {

    }
    def setupSpec() {

    }

    // There's only a certain list of headers that get split
    // See SplittableHeaderUtil for that list
    public static def headers = [
            'accept-charset': "value1, value2;q=1, value3, value4", //Will be split
            'X-User-Name'   : "value1, value2, value3, value4", //Won't be split
            'x-singlevalue' : "value1"
    ]

    def "merges the specified headers, but not unspecified ones, in the request before sending to the origin service"() {
        given: "I start up a jetty to try to figure out if the header is split or not"

        when: "request contains headers with multiple values and so are split"
        def messageChain = deproxy.makeRequest([url: reposeEndpoint, headers: headers, method: 'GET'])
        def sentRequest = ((MessageChain) messageChain).getHandlings()[0]

        then: "the origin service will see merged headers as configured"
        //TODO: can deproxy even understand that they're split or not split? No, no it cannot
        messageChain.receivedResponse.code == "200"
    }


    private class BasicLoop implements Runnable {
        ServerSocket serverSocket = null
        public int port = 0

        @Override
        public void run() {
            serverSocket = new ServerSocket(0)
            port = serverSocket.getLocalPort()

            while (running) {
                try {
                    Socket server = serverSocket.accept()
                    println("DOIN THE THING")
                    //TODO: DO THE TESTS IN HERE LOOKING FOR REPEATED THINGS
                    //Look for repeated headers in the body
                    //Look for the merged one to be only once
                    //extract the value of the header to make sure it's the same...
                    new PrintStream(server.outputStream).println("HTTP/1.1 400 BAD REQUEST")
                    server.close()
                } catch (Exception e) {
                    println("EXCEPTION: $e")
                }
            }
        }
    }

    /**
     * TODO: cannot use a jetty, going to have to read raw socket data.
     */
    public static class SimpleServlet extends HttpServlet {
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            resp.setContentType("text/plain")

            def acceptCharsetList = req.getHeaders("accept-charset").toList()
            def userNameList = req.getHeaders("x-user-name").toList()

            println("accept-charset: " + acceptCharsetList.join(", "))
            println("x-user-name: " + userNameList.join(", "))

            resp.addHeader("x-split-header", "value1")
            resp.addHeader("x-split-header", "value2")
            resp.addHeader("x-split-header", "value3")
            resp.addHeader("x-split-header", "value4")

            println("Header Names: ")

            //TODO: make sure the value of the single header has all the things
            def acceptHeaderValues = new LinkedList<String>()
            acceptHeaderValues.addAll(req.getHeader("accept-charset").split(","))

            if (!acceptHeaderValues.containsAll(headers['accept-charset'])) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST)
                resp.writer.println("accept-charset didn't contain the right values: " + acceptHeaderValues)
            } else if (acceptCharsetList.size() != 1) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST)
                resp.writer.println("accept-charset was not merged!")
            } else if (userNameList.size() == 1) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST)
                resp.writer.println("X-User-Name was merged!")
            } else {
                resp.setStatus(HttpServletResponse.SC_OK)
                resp.writer.println("EVERYTHING IS PEACHY")
            }
        }
    }


    @Ignore
    def "merges the specified headers in the response before returning to the client"() {
    }
}
