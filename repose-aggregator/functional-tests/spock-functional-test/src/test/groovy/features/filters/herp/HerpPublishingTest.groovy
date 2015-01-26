package features.filters.herp

import framework.ReposeValveTest
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain
import org.rackspace.deproxy.Request
import org.rackspace.deproxy.Response
import spock.util.concurrent.PollingConditions

import static org.hamcrest.Matchers.is
import static org.hamcrest.Matchers.notNullValue
import static spock.util.matcher.HamcrestSupport.expect

class HerpPublishingTest extends ReposeValveTest {

    private static final String USER_NAME_HEADER = "X-User-Name"

    private final PollingConditions conditions = new PollingConditions(timeout: 60, delay: 1)
    private final Set<String> sentRequestGuids = new HashSet<>()
    private final Set<String> processedRequestGuids = new HashSet<>()
    private final Set<String> failedRequestGuids = new HashSet<>()

    private boolean consumerReprocessed = false

    def setupSpec() {
        deproxy = new Deproxy()
        deproxy.addEndpoint(
                port: 12345,
                defaultHandler: consumerService) // The Cloud Feeds mock service
        deproxy.addEndpoint(port: properties.targetPort) // The origin service

        def params = properties.defaultTemplateParams
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs('features/filters/herp/publishing', params)
        repose.start(true, true, "cluster", "node")
    }

    def "HERP sends a proper request to the consuming service"() {
        when:
        MessageChain messageChain = sendRequest()

        then:
        expect messageChain.getOrphanedHandlings().find {
            it.getEndpoint().getDefaultHandler().equals(consumerService)
        }, is(notNullValue())
        // todo: further request validation
    }

    def "HERP at-least-once semantics"() {
        when:
        1000.each {
            sendRequest()
        }

        then:
        conditions.eventually {
            processedRequestGuids.containsAll(sentRequestGuids)
        }
    }

    def "HERP exactly-once semantics"() {
        when:
        1000.each {
            sendRequest()
        }

        then:
        conditions.eventually {
            processedRequestGuids.containsAll(sentRequestGuids)
        }
        !consumerReprocessed
    }

    static def consumerService = { Request request ->
        boolean shouldFail = {
            // Simulate a 15% failure rate in the origin service
            new Random().nextInt(101) < 15
        }

        String guid = request.getHeaders().getFirstValue(USER_NAME_HEADER) // todo: will probably be sent in the body

        if (processedRequestGuids.contains(guid)) {
            consumerReprocessed = true
        }

        if (shouldFail) {
            failedRequestGuids.add(guid)
            new Response("500")
        } else {
            if (processedRequestGuids.contains(guid)) {
                consumerReprocessed = true
            }
            failedRequestGuids.remove(guid)
            processedRequestGuids.add(guid)
            new Response("200")
        }
    }

    def sendRequest() {
        String guid = UUID.randomUUID().toString()
        MessageChain messageChain = deproxy.makeRequest(url: reposeEndpoint, method: "GET", headers: ["X-User-Name": guid]) // todo: why does USER_NAME_HEADER not resolve here?
        sentRequestGuids.add(guid)
        messageChain
    }
}
