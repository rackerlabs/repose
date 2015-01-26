package features.filters.herp

import framework.ReposeValveTest
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain
import org.rackspace.deproxy.Request
import org.rackspace.deproxy.Response
import spock.util.concurrent.PollingConditions

class HerpPublishingTest extends ReposeValveTest {

    private static final String USER_NAME_HEADER = "X-User-Name"

    private final PollingConditions conditions = new PollingConditions(timeout: 60, delay: 1)
    private final Set<String> sentRequestGuids = new HashSet<>()
    private final Set<String> processedRequestGuids = new HashSet<>()
    private final Set<String> failedRequestGuids = new HashSet<>()

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
        messageChain.getOrphanedHandlings().find {
            it.getEndpoint().getDefaultHandler().equals(consumerService)
        } != null
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

    def "HERP exactly once semantics"() {
        // todo: extend consumer service to count requests
    }

    def consumerService = { Request request ->
        def shouldFail = {
            // Simulate a 15% failure rate in the origin service
            new Random().nextInt(101) < 15
        }

        String guid = request.getHeaders().getFirstValue(USER_NAME_HEADER) // todo: will probably be sent in the body
        if (shouldFail) {
            failedRequestGuids.add(guid)
            new Response("500")
        } else {
            failedRequestGuids.remove(guid)
            processedRequestGuids.add(guid)
            new Response("200")
        }
    }

    def sendRequest() {
        String guid = UUID.randomUUID().toString()
        deproxy.makeRequest(url: reposeEndpoint, method: "GET", headers: [USER_NAME_HEADER: guid])
        sentRequestGuids.add(guid)
    }
}
