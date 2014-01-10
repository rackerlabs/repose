package features.services.httpconnectionpool

import framework.ReposeValveTest
import spock.lang.Unroll

/**
 * With the HTTPClientService and our HTTPConnectionPooled implementation, the previous features to support jersey and
 * ning as connection frameworks is being deprecated.
 *
 * This test suite is to verify that the appropriate logging is performed to indicate to a REPOSE user that their
 * intended behavior has been deprecated.
 */
class DefaultConnectionFrameworkTest extends ReposeValveTest {

    def setup() {
        cleanLogDirectory()
    }

    def "DEPRECATED: jersey as default connection framework"() {

        given: "Repose is configured with no connection framework specified"
        def params = properties.getDefaultTemplateParams()
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/services/httpconnectionpool/common", params)
        repose.connFramework = ""

        when: "Repose is started"
        repose.start()

        then: "Repose logs should reflect that the default connection framework has changed"
        def List<String> logs = reposeLogSearch.searchByString("DEPRECATED... The default connection framework has changed from Jersey to Apache HttpClient!")
        logs.size() == 1
    }

    @Unroll("DEPRECATED: ability to specify the connection framework of #connFramework via cmdline")
    def "DEPRECATED: ability to specify the connection framework"() {

        given: "Repose is configured with a connection framework specified on cmdline"
        def params = properties.getDefaultTemplateParams()
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/services/httpconnectionpool/common", params)
        repose.connFramework = connFramework

        when: "Repose is started"
        repose.start()

        then: "Repose logs should reflect that the ability to define connection framework is deprecated"
        def List<String> logs = reposeLogSearch.searchByString("DEPRECATED... The ability to define the connection framework of jersey, ning, or apache has been deprecated! The default and only available connection framework is Apache HttpClient")
        logs.size() == 1

        where:
        connFramework << ["apache","jersey","ning"]
    }
}
