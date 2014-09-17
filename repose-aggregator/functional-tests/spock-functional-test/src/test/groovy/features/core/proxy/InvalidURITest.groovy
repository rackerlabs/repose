package features.core.proxy

import framework.ReposeValveTest
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain
import spock.lang.Unroll

class InvalidURITest extends ReposeValveTest {

    def setupSpec() {

        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort)

        def params = properties.getDefaultTemplateParams()
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/services/httpconnectionpool/common", params)
        repose.configurationProvider.applyConfigs("features/services/httpconnectionpool/chunkedfalse", params)
        repose.start()

    }

    @Unroll("when given a uri with invalid characters, Repose should return a 400: #uriSuffixGiven with #method")
    def "when given a uri with a invalid characters, Repose should return a 400"() {
        when:
        MessageChain messageChain = deproxy.makeRequest(url: reposeEndpoint, path: "/path/"+uriSuffixGiven, method: method)
        then:
        messageChain.receivedResponse.code == "400"

        where:
        // Deproxy currently does not support non-UTF-8 characters, so only invalid UTF-8 characters are tested
        [uriSuffixGiven, method] <<
                [['[', ']', '{', '}', '`', '^', '|', '\\', '<', '>'],
                ["POST", "GET", "PUT", "DELETE", "TRACE", "OPTIONS", "PATCH"]].combinations()
    }

    def cleanupSpec() {
        if (repose) {
            repose.stop()
        }
        if (deproxy) {
            deproxy.shutdown()
        }
    }
}
