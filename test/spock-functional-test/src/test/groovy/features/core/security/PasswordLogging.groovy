package features.core.security

import framework.ReposeValveTest
import org.rackspace.gdeproxy.Deproxy

/**
 * D-15183 Ensure passwords are not logged when in DEBUG mode and config files are updated.
 */
class PasswordLogging extends ReposeValveTest {

    def setupSpec() {
        cleanLogDirectory()
        repose.applyConfigs("features/core/security/before")
        repose.start()
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.getProperty("target.port").toInteger())
    }

    def cleanupSpec() {
        deproxy.shutdown()
        repose.stop()
    }

    def "identity passwords in auth configs are not logged in plaintext"() {

        given: "Repose configs are updated"
        repose.updateConfigs("features/core/security/after")

        when: "I search for DEBUG logs for the configuration updated log entry"
        List<String> logs = reposeLogSearch.searchByString("Configuration Updated")

        then: "passwords in the DEBUG log are not logged in plaintext"
        logs.size() == 4
        logs[0].contains("password=*******") == true
        logs[1].contains("password=*******") == true
        logs[2].contains("password=*******") == true
        logs[3].contains("password=*******") == true
    }


}
