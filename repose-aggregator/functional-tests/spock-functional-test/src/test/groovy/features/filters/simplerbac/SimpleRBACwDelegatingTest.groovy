package features.filters.simplerbac
import framework.ReposeValveTest
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain
import spock.lang.Unroll

import static javax.servlet.http.HttpServletResponse.*
/**
 * Created by jennyvo on 6/2/15.
 */
class SimpleRBACwDelegatingTest extends ReposeValveTest {

    def setupSpec() {
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort)

        def params = properties.getDefaultTemplateParams()
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/filters/simplerbac", params)
        repose.configurationProvider.applyConfigs("features/filters/simplerbac/notmasked", params)
        repose.start()
    }

    def cleanupSpec() {
        if (repose)
            repose.stop()
        if (deproxy)
            deproxy.shutdown()
    }

    @Unroll("Test with #path, #method, #roles")
    def "Test simple RBAC with single role"() {
        when:
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint + path, method: method, headers: ["X-Roles": roles])

        then:
        mc.receivedResponse.code == "200"
        mc.handlings[0].request.headers.contains("X-Delegated")
        mc.handlings[0].request.headers.getFirstValue("X-Delegated") == delegateMsg

        where:
        path                 | method   | roles       | delegateMsg
        "/path/to/this"      | "GET"    | "super"     | SC_OK
        "/path/to/this"      | "PUT"    | "super"     | SC_OK
        "/path/to/this"      | "POST"   | "super"     | SC_OK
        "/path/to/this"      | "DELETE" | "super"     | SC_OK
        "/path/to/this"      | "GET"    | "useradmin" | SC_OK
        "/path/to/this"      | "PUT"    | "useradmin" | SC_OK
        "/path/to/this"      | "POST"   | "useradmin" | SC_OK
        "/path/to/this"      | "DELETE" | "useradmin" | SC_FORBIDDEN
        "/path/to/this"      | "GET"    | "admin"     | SC_OK
        "/path/to/this"      | "PUT"    | "admin"     | SC_OK
        "/path/to/this"      | "POST"   | "admin"     | SC_FORBIDDEN
        "/path/to/this"      | "DELETE" | "admin"     | SC_FORBIDDEN
        "/path/to/this"      | "GET"    | "user"      | SC_OK
        "/path/to/this"      | "PUT"    | "user"      | SC_FORBIDDEN
        "/path/to/this"      | "POST"   | "user"      | SC_FORBIDDEN
        "/path/to/this"      | "DELETE" | "user"      | SC_FORBIDDEN
        "/path/to/this"      | "GET"    | "none"      | SC_FORBIDDEN
        "/path/to/this"      | "PUT"    | "none"      | SC_FORBIDDEN
        "/path/to/this"      | "POST"   | "none"      | SC_FORBIDDEN
        "/path/to/this"      | "DELETE" | "none"      | SC_FORBIDDEN
        "/path/to/that"      | "GET"    | "super"     | SC_OK
        "/path/to/that"      | "PUT"    | "super"     | SC_OK
        "/path/to/that"      | "POST"   | "super"     | SC_OK
        "/path/to/that"      | "DELETE" | "super"     | SC_OK
        "/path/to/that"      | "GET"    | "useradmin" | SC_OK
        "/path/to/that"      | "PUT"    | "useradmin" | SC_OK
        "/path/to/that"      | "POST"   | "user"      | SC_FORBIDDEN
        "/path/to/that"      | "DELETE" | "admin"     | SC_FORBIDDEN
        "/path/to/that"      | "POST"   | "super"     | SC_OK
        "/path/to/that"      | "DELETE" | "super"     | SC_OK
        "/path/to/test"      | "GET"    | "user"      | SC_OK
        "/path/to/test"      | "POST"   | "useradmin" | SC_OK
        "/path/to/test"      | "GET"    | "admin"     | SC_FORBIDDEN
        "/path/to/test"      | "POST"   | "super"     | SC_FORBIDDEN
        "/path/to/test"      | "PUT"    | "user"      | SC_METHOD_NOT_ALLOWED
        "/path/to/test"      | "DELETE" | "useradmin" | SC_METHOD_NOT_ALLOWED
        "/path/to/something" | "GET"    | "user"      | SC_NOT_FOUND
        "/path/to/something" | "GET"    | "super"     | SC_NOT_FOUND
        "/path/to/something" | "GET"    | "admin"     | SC_NOT_FOUND
        "/path/to/something" | "POST"   | "none"      | SC_NOT_FOUND
        "/path/to/something" | "PUT"    | "useradmin" | SC_NOT_FOUND
    }
}
