package features.filters.herp

import framework.ReposeValveTest
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.Request
import org.rackspace.deproxy.Response

class HerpPublishingTest extends ReposeValveTest {

    private static final String GUID_HEADER = "X-GUID-HEADER"

    private final Set<String> sentRequestGuids = new HashSet<>() // todo
    private final Set<String> processedRequestGuids = new HashSet<>()
    private final Set<String> failedRequestGuids= new HashSet<>()

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

    // todo: tests go here

    def "sample test"() {
        when:
        deproxy.makeRequest(url: reposeEndpoint, method: "GET", headers: defaultHeaders)
    }

    def defaultHeaders = { [GUID_HEADER: UUID.randomUUID().toString()] }

    def consumerService = { Request request ->
        def shouldFail = {
            // Simulate a 15% failure rate in the origin service
            new Random().nextInt(101) < 15
        }

        String guid = request.getHeaders().getFirstValue(GUID_HEADER)
        if (shouldFail) {
            failedRequestGuids.add(guid)
            new Response("500")
        } else {
            failedRequestGuids.remove(guid)
            processedRequestGuids.add(guid)
            new Response("200")
        }
    }
}
