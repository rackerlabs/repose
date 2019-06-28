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
package features.filters.ratelimiting

import org.junit.experimental.categories.Category
import org.openrepose.framework.test.PortFinder
import org.openrepose.framework.test.ReposeConfigurationProvider
import org.openrepose.framework.test.ReposeValveLauncher
import org.openrepose.framework.test.ReposeValveTest
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain
import org.rackspace.deproxy.Response
import org.w3c.dom.Document
import org.xml.sax.InputSource
import scaffold.category.Filters
import spock.lang.Shared

import javax.xml.parsers.DocumentBuilder
import javax.xml.parsers.DocumentBuilderFactory

@Category(Filters)
class RateLimitingTwoNodeTest extends ReposeValveTest {
    final handler = { return new Response(200, "OK") }

    final Map<String, String> userHeaderDefault = ["X-PP-User": "user"]
    final Map<String, String> groupHeaderDefault = ["X-PP-Groups": "customer"]
    final Map<String, String> acceptHeaderDefault = ["Accept": "application/xml"]

    static int userCount = 0
    static int datastorePort
    static int datastorePort2

    @Shared
    ReposeValveLauncher repose2

    String getNewUniqueUser() {
        String name = "user-${userCount}"
        userCount++
        return name
    }

    def setupSpec() {
        int repose2Port = properties.portFinder.nextOpenPort
        String repose2ConfigDir = properties.configDirectory + "2"
        ReposeConfigurationProvider configurationProvider2 = new ReposeConfigurationProvider(
            repose2ConfigDir,
            properties.configTemplates
        )
        repose2 = new ReposeValveLauncher(
            configurationProvider2,
            properties.reposeJar,
            "http://${properties.targetHostname}:$repose2Port", // properties.reposeEndpoint
            repose2ConfigDir,
            repose2Port
        )
        repose2.configurationProvider.cleanConfigDirectory()

        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort)

        datastorePort = PortFinder.instance.getNextOpenPort()
        datastorePort2 = PortFinder.instance.getNextOpenPort()

        def params = properties.getDefaultTemplateParams()
        params += [
            reposePort2   : repose2.reposePort,
            datastorePort : datastorePort,
            datastorePort2: datastorePort2
        ]

        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/filters/ratelimiting/twonodes/common", params)
        repose.configurationProvider.applyConfigs("features/filters/ratelimiting/twonodes/nodeOne", params)

        repose2.configurationProvider.applyConfigs("common", params)
        repose2.configurationProvider.applyConfigs("features/filters/ratelimiting/twonodes/common", params)
        repose2.configurationProvider.applyConfigs("features/filters/ratelimiting/twonodes/nodeTwo", params)

        repose.start(killOthersBeforeStarting: false)
        repose.waitForNon500FromUrl(repose.reposeEndpoint)

        repose2.start(killOthersBeforeStarting: false)
        repose2.waitForNon500FromUrl(repose2.reposeEndpoint)
    }

    def "When Repose is configured with multiple nodes, rate-limiting info should be shared"() {
        given: "load the configs for multiple nodes, and use all remaining requests"
        useAllRemainingRequests()

        when: "the user sends their request and the rate-limit has not been reached"
        MessageChain messageChain = deproxy.makeRequest(url: repose2.reposeEndpoint, method: "GET",
            headers: userHeaderDefault + groupHeaderDefault, defaultHandler: handler)

        then: "the request is rate-limited, and does not pass to the origin service"
        messageChain.receivedResponse.code.equals("413")
        messageChain.handlings.size() == 0
    }

    def "when a burst of limits is sent for an execution, only 2x-1 requests can get through"() {
        given:
        def user = getNewUniqueUser()
        def group = "customer"
        def headers = ['X-PP-User': user, 'X-PP-Groups': group]

        List<Thread> clientThreads = new ArrayList<Thread>()
        def totalSuccessfulCount = 0
        def totalFailedCount = 0
        List<String> requests = new ArrayList()
        int rate_limiting_count = 5

        long start = System.currentTimeMillis()

        numClients.times { x ->

            def thread = Thread.start {
                def threadNum = x

                callsPerClient.times { i ->
                    requests.add("spock-thread-$threadNum-request-$i")
                    def messageChain = null
                    if ((i + x) % 2 == 0) {
                        messageChain = deproxy.makeRequest(url: reposeEndpoint, method: "GET", headers: headers)
                        println reposeEndpoint
                    } else {
                        messageChain = deproxy.makeRequest(url: repose2.reposeEndpoint, method: "GET", headers: headers)
                        println repose2.reposeEndpoint
                    }
                    println messageChain.receivedResponse.code
                    if (messageChain.receivedResponse.code.equals("200")) {
                        totalSuccessfulCount = totalSuccessfulCount + 1
                    } else {
                        totalFailedCount = totalFailedCount + 1
                    }

                    Thread.sleep(1000)

                }
            }
            clientThreads.add(thread)
        }

        when:
        clientThreads*.join()

        then:
        long stop = System.currentTimeMillis()
        assert totalSuccessfulCount <= (rate_limiting_count * 2 - 1)
        println totalSuccessfulCount
        println rate_limiting_count

        where:
        numClients | callsPerClient
        10         | 50
        50         | 10
    }

    private int parseRemainingFromXML(String s, int limit) {
        DocumentBuilder documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder()
        Document document = documentBuilder.parse(new InputSource(new StringReader(s)))

        document.getDocumentElement().normalize()

        return Integer.parseInt(document.getElementsByTagName("limit").item(limit).getAttributes().getNamedItem("remaining").getNodeValue())
    }

    private int parseAbsoluteFromXML(String s, int limit) {
        DocumentBuilder documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder()
        Document document = documentBuilder.parse(new InputSource(new StringReader(s)))

        document.getDocumentElement().normalize()

        return Integer.parseInt(document.getElementsByTagName("limit").item(limit).getAttributes().getNamedItem("value").getNodeValue())
    }

    private String getLimits() {
        MessageChain messageChain = deproxy.makeRequest(url: reposeEndpoint + "/service/limits", method: "GET",
            headers: userHeaderDefault + groupHeaderDefault + acceptHeaderDefault);

        return messageChain.receivedResponse.body
    }

    private void waitForLimitReset() {
        while (parseRemainingFromXML(getLimits(), 1) != 5) {
            sleep(10000)
        }
    }

    private void useAllRemainingRequests() {
        MessageChain messageChain = deproxy.makeRequest(url: reposeEndpoint, method: "GET",
            headers: userHeaderDefault + groupHeaderDefault, defaultHandler: handler);

        while (!messageChain.receivedResponse.code.equals("413")) {
            messageChain = deproxy.makeRequest(url: reposeEndpoint, method: "GET",
                headers: userHeaderDefault + groupHeaderDefault, defaultHandler: handler);
        }
    }
}
