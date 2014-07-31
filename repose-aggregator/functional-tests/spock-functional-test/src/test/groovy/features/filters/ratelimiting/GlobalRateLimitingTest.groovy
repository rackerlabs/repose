package features.filters.ratelimiting

import framework.ReposeValveTest
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain
import org.rackspace.deproxy.Response
import static org.junit.Assert.*
import org.w3c.dom.Document

/**
 * Created by jennyvo on 7/30/14.
 */
class GlobalRateLimitingTest extends ReposeValveTest {
    final handler = {return new Response(200, "OK")}

    final Map<String, String> userHeaderDefault = ["X-PP-User" : "user"]
    final Map<String, String> groupHeaderDefault = ["X-PP-Groups" : "customer"]
    final Map<String, String> acceptHeaderDefault = ["Accept" : "application/xml"]

    static int userCount = 0;
    String getNewUniqueUser() {

        String name = "user-${userCount}"
        userCount++;
        return name;
    }

    def setupSpec() {
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort)

        def params = properties.getDefaultTemplateParams()
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/filters/ratelimiting/oneNode", params)
        repose.configurationProvider.applyConfigs("features/filters/ratelimiting/globalratelimit", params)
        repose.start()
        repose.waitForNon500FromUrl(reposeEndpoint)
    }

    def cleanupSpec() {
        if (repose)
            repose.stop()
        if (deproxy)
            deproxy.shutdown()
    }

    def "When Repose config with Global Rate Limit, user limit should hit first" () {
        given: "the rate-limit has not been reached"
        //waitForLimitReset()

        (1..5).each{
            i ->
                when: "the user sends their request and the rate-limit has not been reached"
                MessageChain messageChain = deproxy.makeRequest(url: reposeEndpoint + "/service/test", method: "GET",
                        headers: userHeaderDefault + ['X-PP-Groups': 'all-limits-small'], defaultHandler: handler)

                then: "the request is not rate-limited, and passes to the origin service"
                assertTrue(messageChain.receivedResponse.code.equals("200"))
                assertTrue(messageChain.handlings.size() == 1)
        }

        when: "the user hit the rate-limit"
        MessageChain messageChain = deproxy.makeRequest(url: reposeEndpoint + "/service/test", method: "GET",
                headers: userHeaderDefault + ['X-PP-Groups': 'all-limits-small'], defaultHandler: handler)

        then: "the request is not rate-limited, and passes to the origin service"
        messageChain.receivedResponse.code.equals("413")
    }

    def "When Run with different users, hit the same resource, global limit share between users" () {
        given:"the rate-limit has not been reached"
        //waitForLimitReset
        sleep(60000)
        def group = "customer"
        def headers1 = ['X-PP-User': "user1", 'X-PP-Groups': group]
        def headers2 = ['X-PP-User': "user2", 'X-PP-Groups': group]

        (1..2).each {
            i ->
                when: "user1 hit the same resource"
                MessageChain messageChain = deproxy.makeRequest(url: reposeEndpoint + "/service/test", method: "GET",
                        headers: headers1, defaultHandler: handler)

                then: "the request is not rate-limited, and passes to the origin service"
                assertTrue(messageChain.receivedResponse.code.equals("200"))
                assertTrue(messageChain.handlings.size() == 1)
        }

        (1..3).each {
            i ->
                when: "user2 hit the same resource"
                MessageChain messageChain = deproxy.makeRequest(url: reposeEndpoint + "/service/test", method: "GET",
                        headers: headers2, defaultHandler: handler)

                then: "the request is not rate-limited, and passes to the origin service"
                assertTrue(messageChain.receivedResponse.code.equals("200"))
                assertTrue(messageChain.handlings.size() == 1)
        }

        when: "user2 hit the same resource, rate limitted"
        MessageChain messageChain = deproxy.makeRequest(url: reposeEndpoint + "/service/test", method: "GET",
                headers: headers1, defaultHandler: handler)

        then: "the request is rate-limited, and passes to the origin service"
        messageChain.receivedResponse.code.equals("503")

        when: "user2 hit the same resource, rate limitted"
        messageChain = deproxy.makeRequest(url: reposeEndpoint + "/service/test", method: "GET",
                headers: headers2, defaultHandler: handler)

        then: "the request is rate-limited, and passes to the origin service"
        messageChain.receivedResponse.code.equals("503")
    }
}
