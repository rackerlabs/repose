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
import org.rackspace.deproxy.Deproxy

/**
 * This is a rather low level test to verify the actual merging of headers. Recommend that other tests regarding header
 * merging not go in here, but perhaps where they make more sense, if possible
 */
class MergeHeaderTest extends ReposeValveTest {

    static volatile boolean running = true

    def cleanupSpec() {
        running = false
    }

    def setupSpec() {
        BasicLoop loop = new BasicLoop()

        running = true
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

    // There's only a certain list of headers that get split
    // See SplittableHeaderUtil for that list
    public static def requestHeaders = [
            'accept-charset': "value1, value2;q=1, value3, value4", //Will be split
            'X-User-Name'   : "value1, value2, value3, value4", //Won't be split
            'x-singlevalue' : "value1"
    ]

    def "merges the specified headers in the response before returning to the client"() {
        when: "I just make a request that comes back"
        Socket client = new Socket("localhost", properties.reposePort)
        def pw = new PrintWriter(client.getOutputStream())

        pw.println("GET / HTTP/1.0")
        pw.println("HOST: localhost")
        //Have to include the accept charset so the socket server doesn't get mad
        pw.println("accept-charset: ${requestHeaders["accept-charset"]}")
        pw.println()

        pw.flush()

        //Now I should read stuff until the socket closes
        def lines = client.inputStream.readLines()

        //All done!
        client.close()

        then:
        lines.contains("Everything is peachy") == true
        lines.contains("HTTP/1.1 200 OK") == true
        lines.count { l ->
            l.matches("(?i)^x-split-header.*")
        } == 1

        lines.count { l ->
            l.matches("(?i)^x-random.*")
        } == 0
    }

    def "merges the specified headers, but not unspecified ones, in the request before sending to the origin service"() {
        given: "I start up a jetty to try to figure out if the header is split or not"

        when: "request contains headers with multiple values and so are split"
        def messageChain = deproxy.makeRequest([url: reposeEndpoint, headers: requestHeaders, method: 'GET'])

        then: "the origin service will see merged headers as configured"
        //TODO: can deproxy even understand that they're split or not split? No, no it cannot
        messageChain.receivedResponse.body.contains("Everything is peachy")
        messageChain.receivedResponse.code == "200"
    }

    /**
     * I have to implement my own stupid HTTP server for the moment, in order to actually verify split headers and such
     */
    private class BasicLoop implements Runnable {
        ServerSocket serverSocket = null
        public int port = 0

        @Override
        public void run() {
            serverSocket = new ServerSocket(0)
            port = serverSocket.getLocalPort()
            println("STARTING UP SERVER ON PORT: $port")

            int count = 1
            while (running) {
                try {
                    println("WAITING FOR CONNECTION $count")
                    count += 1

                    Socket server = serverSocket.accept()
                    Thread.start {
                        println("IN THREAD DOIN TEH WORK")

                        //Cannot just read all the lines, because yeah
                        boolean readHeader = false
                        def headerLines = []
                        def reader = new BufferedReader(new InputStreamReader(server.inputStream))
                        while (!readHeader) {
                            //Read lines until I get a blank line, then we're done
                            def line = reader.readLine()
                            headerLines << line
                            if (line == "")
                                readHeader = true
                        }
                        println("ORIGIN RECEIVED HTTP HEADER: \n" + headerLines.join("\n"))

                        def headers = headerLines.collect { line ->
                            if (line.matches(".+:.*")) line
                        }
                        headers.removeAll([null])

                        def headerKeys = headers.collect { header ->
                            def things = header.split(":")
                            things[0]
                        }

                        //Collect all the header values into a map to compare stuff
                        def headerValues = [:]
                        headers.collect { header ->
                            def things = header.split(":")
                            def key = things[0].toLowerCase()
                            def value = things[1].toString().trim()

                            if (headerValues[key] == null)
                                headerValues[key] = []

                            headerValues[key].addAll(value.split(", "))
                        }

                        println("header Keys:\n" + headerKeys.join("\n"))

                        def randomHeaderCount = headerKeys.count { it.equalsIgnoreCase("x-random") }
                        def acceptCharsetCount = headerKeys.count { it.equalsIgnoreCase("accept-charset") }
                        def userNameCount = headerKeys.count { it.equalsIgnoreCase("x-user-name") }

                        def acceptCharsetProper = headerValues['accept-charset']?.containsAll(requestHeaders['accept-charset'].split(", "))

                        def responseString = "HTTP/1.0 200 OK"
                        def body = "Everything is peachy"
                        if (randomHeaderCount != 0) {
                            //FAIL
                            responseString = "HTTP/1.0 500 INTERNAL SERVER ERROR"
                            body = "X-random header was added, shouldn't be there"
                        } else if (!acceptCharsetProper) {
                            //FAIL
                            responseString = "HTTP/1.0 400 BAD REQUEST"
                            body = "accept-charset values didn't contain all: ${headerValues['accept-charset']?.join(", ")}"
                        } else if (acceptCharsetCount != 1) {
                            //FAIL
                            responseString = "HTTP/1.0 400 BAD REQUEST"
                            body = "accept-charset not merged"
                        } else if (userNameCount == 1) {
                            //ALSO FAIL
                            responseString = "HTTP/1.0 400 BAD REQUEST"
                            body = "x-user-name was merged"
                        }

                        //Craft my http response
                        def response = new PrintStream(server.outputStream)
                        response.println(responseString)
                        response.println("Content-type: text/plain")
                        //Stick some split headers in there for use during the response merging
                        response.println("x-split-header: value1")
                        response.println("x-split-header: value2;q=0.3")
                        response.println("x-split-header: value3;q=0.4")
                        response.println("x-split-header: value4")
                        response.println("x-split-header: value5")

                        response.println("content-length: ${body.bytes.length}")
                        response.println()
                        response.println(body)

                        server.close()
                    }
                } catch (Exception e) {
                    println("SOCKET SERVER EXCEPTION: $e")
                    e.printStackTrace()
                }
            } //End of running loop
        }
    }
}
