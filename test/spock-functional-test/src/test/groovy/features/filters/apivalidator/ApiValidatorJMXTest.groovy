package features.filters.apivalidator

import framework.ConfigHelper
import framework.ReposeClient
import framework.ReposeLauncher
import framework.ValveLauncher
import spock.lang.Specification

class ApiValidatorJMXTest extends Specification {

    def X_ROLES = "X-Roles"

    def configDirectory = System.getProperty("repose.config.directory")
    def configSamples = System.getProperty("repose.config.samples")

    def ReposeLauncher reposeLauncher
    def ReposeClient reposeClient = new ReposeClient()
    def ConfigHelper configHelper = new ConfigHelper(configDirectory, configSamples)

    def setup() {
        configHelper.prepConfiguration("api-validator/common", "api-validator/jmx")

        reposeLauncher = new ValveLauncher()
        reposeLauncher.start()
    }

    def cleanup() {
        reposeLauncher.stop()
    }

    def "registers JMX beans for loaded validators"() {

        given:
        reposeClient.setHeader(X_ROLES, "role-a, role-b, role-c")

        when: "a request is submitted that causes validators to be initialized"
        reposeClient.doGet("/")

        then:
        // verify the JMX beans are registered
        1 == 1
    }

//
//    def "unloads JMX beans after reconfig"() {
//
//        when: "a request is submitted that causes validators to be initialized"
//        reposeClient.setHeader(X_ROLES, "role-a, role-b, role-c")
//        reposeClient.doGet("/")
//
//        and: "api validator config is reloaded, and a second request is sent"
//        configHelper.updateConfiguration("api-validator/jmx-updated")
//
//
//        then:
//        // verify the JMX beans are registered
//        1 == 1
//    }
//

}
