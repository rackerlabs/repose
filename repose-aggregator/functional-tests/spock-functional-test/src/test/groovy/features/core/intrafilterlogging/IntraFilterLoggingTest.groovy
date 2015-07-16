package features.core.intrafilterlogging
import framework.ReposeValveTest
import org.rackspace.deproxy.Deproxy
/**
 * Created by jennyvo on 7/16/15.
 * Verify Repose no longer logging 'null' as part of currunt Filter description
 */
class IntraFilterLoggingTest extends ReposeValveTest {
    String url

    def setupSpec() {
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort)
        reposeLogSearch.cleanLog()
        this.url = properties.reposeEndpoint

        def params = properties.defaultTemplateParams
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs('features/core/intrafilterlogging', params)
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

    def "Verify intra filter log for current filter no longer have 'null' in the description" () {
        given:
        // repose start up

        when:
        deproxy.makeRequest(url: url)

        then:
        reposeLogSearch.searchByString("null-rate-limiting").size() == 0
    }
}
