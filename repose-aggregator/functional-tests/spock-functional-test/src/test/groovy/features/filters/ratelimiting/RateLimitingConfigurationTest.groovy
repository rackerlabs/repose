package features.filters.ratelimiting

import framework.ReposeValveTest
import org.rackspace.deproxy.Deproxy

class RateLimitingConfigurationTest extends ReposeValveTest {

    def "when starting Repose with a rate limiting config without the request-endpoint element, should not throw a NPE"() {
        given:
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort)

        def params = properties.getDefaultTemplateParams()
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/filters/ratelimiting/config", params)

        when:
        repose.start()

        def mc = deproxy.makeRequest(url: reposeEndpoint, headers: ['X-PP-User': 'test-user', 'X-PP-Groups': 'test-group'])

        then:
        mc.getHandlings().size() + mc.getOrphanedHandlings().size() == 1
        notThrown(NullPointerException)
    }
}
