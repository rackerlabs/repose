package features.core.powerfilter
import framework.ReposeValveTest
import org.rackspace.deproxy.Deproxy

class EmptyRequestBodyTest extends ReposeValveTest {
    def setupSpec() {
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort)

        def params = properties.getDefaultTemplateParams()
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/core/proxy", params)
        repose.start()

        repose.waitForNon500FromUrl(reposeEndpoint)
    }

    def "Repose should not remove request bodies unless filters do so explicitly"() {
        when:
        def mc = deproxy.makeRequest(url: reposeEndpoint, method: method, requestBody: "body content")

        then:
        mc.handlings[0].request.body == "body content"

        where:
        method << ["GET", "PUT", "POST", "PATCH", "DELETE"]
    }
}
