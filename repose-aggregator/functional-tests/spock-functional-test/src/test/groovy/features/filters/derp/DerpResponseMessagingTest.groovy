package features.filters.derp

import framework.ReposeValveTest
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain

class DerpResponseMessagingTest extends ReposeValveTest {

    def setupSpec() {
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort)

        def params = properties.getDefaultTemplateParams()
        repose.configurationProvider.applyConfigs('common', params)
        repose.configurationProvider.applyConfigs('features/filters/derp/responsemessaging', params)
        repose.start()
    }

    def "when a request is delegated, then the derp filter response should be processed by the response messaging service"() {
        when:
        MessageChain messageChain = deproxy.makeRequest(url: getReposeEndpoint(), method: 'GET',
                headers: ['X-Delegated': 'status_code=500`component=foo`message=bar;q=1.0'])

        then:
        messageChain.getHandlings().size() + messageChain.getOrphanedHandlings().size() == 0
        messageChain.getReceivedResponse().getBody().equals('Response messaging caught a 5xx response')
    }
}
