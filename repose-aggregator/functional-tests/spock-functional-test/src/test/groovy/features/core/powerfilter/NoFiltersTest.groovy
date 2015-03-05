package features.core.powerfilter

import framework.ReposeValveTest
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain
import spock.lang.Unroll

import javax.servlet.http.HttpServletResponse

class NoFiltersTest extends ReposeValveTest {

    def setupSpec() {
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort)

        def params = properties.getDefaultTemplateParams()
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/core/proxy/noFilters", params)
        repose.start()
    }

    def cleanupSpec() {
        if (deproxy) {
            deproxy.shutdown()
        }
        if (repose) {
            repose.stop()
        }
    }

    @Unroll("Repose should act as a basic reverse proxy (pass thru) for HTTP method #method")
    def "Repose should act as a basic reverse proxy (pass thru) for HTTP methods"() {
        given:
        String requestBody = "request body"
        String deproxyEndpoint = "http://localhost:${properties.targetPort}"

        when:
        MessageChain mc = deproxy.makeRequest(url: deproxyEndpoint, requestBody: requestBody)

        then:
        mc.getReceivedResponse().getCode() == HttpServletResponse.SC_OK.toString()

        where:
        method << ["GET", "HEAD", "PUT", "POST", "PATCH", "DELETE"]
    }
}
