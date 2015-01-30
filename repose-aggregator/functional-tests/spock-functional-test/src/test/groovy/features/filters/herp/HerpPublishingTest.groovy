package features.filters.herp

import framework.ReposeValveTest
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain
import org.rackspace.deproxy.Request
import org.rackspace.deproxy.Response
import spock.util.concurrent.PollingConditions

import java.util.concurrent.Callable
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

import static org.hamcrest.Matchers.*
import static spock.util.matcher.HamcrestSupport.expect

class HerpPublishingTest extends ReposeValveTest {

    private static final List<String> sentRequestGuids = new Vector<>()
    private static final List<String> processedRequestGuids = new Vector<>()
    private static final List<String> failedRequestGuids = new Vector<>()
    private static final Random random = new Random()

    private final ExecutorService executorService = Executors.newCachedThreadPool()
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

    def setup() {
        sentRequestGuids.clear()
        processedRequestGuids.clear()
        failedRequestGuids.clear()
    }

    def "HERP sends a proper request to the consuming service"() {
        when:
        sendAsyncRequest()

        then:
        conditions.eventually {
            expect processedRequestGuids.size() + failedRequestGuids.size(), is(greaterThan(0))
            // todo: further request validation
        }
    }

    def "HERP at-least-once semantics"() {
        given:
        int numRequests = 1000

        when:
        println "\n****************Started " + new Date(System.currentTimeMillis())
        numRequests.times {
            sendAsyncRequest()
        }

        println "\n****************Submitted all jobs " + new Date(System.currentTimeMillis())
        executorService.shutdown()
        executorService.awaitTermination(60, TimeUnit.SECONDS)
        println "\n****************All jobs completed " + new Date(System.currentTimeMillis())

        then:
        conditions.eventually {
            expect sentRequestGuids.size(), is(equalTo(1000))
            expect sentRequestGuids.size(), is(lessThanOrEqualTo(processedRequestGuids.size()))
            expect new HashSet<String>(processedRequestGuids).containsAll(sentRequestGuids), is(equalTo(true))
        }
        println "\n****************Done " + new Date(System.currentTimeMillis())
//        sentRequestGuids.size() == 1000
//        sentRequestGuids.size() <= processedRequestGuids.size()
//        new HashSet<String>(processedRequestGuids).containsAll(sentRequestGuids)
    }

    def "HERP exactly-once semantics"() {
        given:
        int numRequests = 1000

        when:
        numRequests.times {
            sendAsyncRequest()
        }

        then:
        conditions.eventually {
            expect sentRequestGuids.size(), is(equalTo(1000))
            expect sentRequestGuids.size(), is(equalTo(processedRequestGuids.size()))
            expect processedRequestGuids.containsAll(sentRequestGuids), is(equalTo(true))
        }
    }

    static def consumerService = { Request request ->
        // Simulate a 15% failure rate in the origin service
        boolean shouldFail = random.nextInt(101) < 15

        String guid = request.getBody().toString()

        if (shouldFail) {
            failedRequestGuids.add(guid)
            new Response("500")
        } else {
            processedRequestGuids.add(guid)
            new Response("200")
        }
    }

    def sendAsyncRequest() {
        executorService.submit({
            String guid = UUID.randomUUID().toString()
            MessageChain messageChain = deproxy.makeRequest(url: reposeEndpoint, method: "GET", headers: ["X-User-Name": guid])
            sentRequestGuids.add(guid)
            messageChain
        } as Callable)
    }
}
