package features.filters.herp

import framework.ReposeValveTest
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain
import org.rackspace.deproxy.Request
import org.rackspace.deproxy.Response
import spock.lang.Ignore
import spock.util.concurrent.PollingConditions

import static org.hamcrest.Matchers.greaterThan
import static org.hamcrest.Matchers.is
import static org.hamcrest.Matchers.notNullValue
import static spock.util.matcher.HamcrestSupport.expect

class HerpPublishingTest extends ReposeValveTest {

    private static final Set<String> sentRequestGuids = new HashSet<>()
    private static final Set<String> processedRequestGuids = new HashSet<>()
    private static final Set<String> failedRequestGuids = new HashSet<>()
    private static final Random random = new Random()

    private static boolean consumerReprocessed = false

    private final PollingConditions conditions = new PollingConditions(timeout: 60, delay: 1)

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

    @Ignore
    def "HERP sends a proper request to the consuming service"() {
        when:
        MessageChain messageChain = sendRequest()

        then:
        conditions.eventually {
            expect messageChain.getOrphanedHandlings().find {
                it.getEndpoint().getDefaultHandler().equals(consumerService)
            }, is(notNullValue())
            // todo: further request validation
        }
    }

    def "HERP at-least-once semantics"() {
        when:
        20.times {
            sendRequest()
        }

        then:
        conditions.eventually {
            processedRequestGuids.containsAll(sentRequestGuids)
        }
        expect processedRequestGuids.size(), is(greaterThan(19))
    }

    def "HERP exactly-once semantics"() {
        when:
        20.times {
            sendRequest()
        }

        then:
        conditions.eventually {
            processedRequestGuids.containsAll(sentRequestGuids)
        }
        expect processedRequestGuids.size(), is(greaterThan(19))
        !consumerReprocessed
    }

    synchronized static def consumerService = { Request request ->
        // Simulate a 15% failure rate in the origin service
        boolean shouldFail = random.nextInt(101) < 15

        String guid = request.getBody().toString()

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
