package features.filters
import framework.ReposeValveTest
import framework.client.http.HttpRequestParams
import framework.client.jmx.JmxClient

class ApiValidatorJMXTest extends ReposeValveTest {

    def X_ROLES = "X-Roles"

    def JmxClient jmxClient

    def setup() {
        configHelper.prepConfiguration("api-validator/common", "api-validator/jmx")

        reposeLauncher.enableJmx(true)
        reposeLauncher.start()

        jmxClient = new JmxClient(properties.getProperty("repose.jmxUrl"))
    }

    def cleanup() {
        reposeLauncher.stop()
    }

    def "registers JMX beans for loaded validators"() {

        given:

        def HttpRequestParams requestParams = new HttpRequestParams()
        requestParams.headers.put(X_ROLES, "role-a, role-b, role-c")

        when: "a request is submitted that causes validators to be initialized"

        reposeClient.doGet("/", requestParams)

        then:
        def mbean = jmxClient.getMBean("repose-node-com.rackspace.papi.jmx")
        mbean.getProperty("bar") == "yah man!"

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
