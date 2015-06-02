package features.filters.simplerbac
import framework.ReposeValveTest
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain
import spock.lang.Unroll
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
        repose.configurationProvider.applyConfigs("features/filters/simplerbac/delegating", params)
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
        "/path/to/this"      | "GET"    | "super"     | "status_code=200`component=simple-rbac`message=OK: /{/path/to/this};q=0.5"
        "/path/to/this"      | "PUT"    | "super"     | "status_code=200`component=simple-rbac`message=OK: /{/path/to/this};q=0.5"
        "/path/to/this"      | "POST"   | "super"     | "status_code=200`component=simple-rbac`message=OK: /{/path/to/this};q=0.5"
        "/path/to/this"      | "DELETE" | "super"     | "status_code=200`component=simple-rbac`message=OK: /{/path/to/this};q=0.5"
        "/path/to/this"      | "GET"    | "useradmin" | "status_code=200`component=simple-rbac`message=OK: /{/path/to/this};q=0.5"
        "/path/to/this"      | "PUT"    | "useradmin" | "status_code=200`component=simple-rbac`message=OK: /{/path/to/this};q=0.5"
        "/path/to/this"      | "POST"   | "useradmin" | "status_code=200`component=simple-rbac`message=OK: /{/path/to/this};q=0.5"
        "/path/to/this"      | "DELETE" | "useradmin" | "status_code=403`component=simple-rbac`message=FORBIDDEN: /{/path/to/this};q=0.5"
        "/path/to/this"      | "GET"    | "admin"     | "status_code=200`component=simple-rbac`message=OK: /{/path/to/this};q=0.5"
        "/path/to/this"      | "PUT"    | "admin"     | "status_code=200`component=simple-rbac`message=OK: /{/path/to/this};q=0.5"
        "/path/to/this"      | "POST"   | "admin"     | "status_code=403`component=simple-rbac`message=FORBIDDEN: /{/path/to/this};q=0.5"
        "/path/to/this"      | "DELETE" | "admin"     | "status_code=403`component=simple-rbac`message=FORBIDDEN: /{/path/to/this};q=0.5"
        "/path/to/this"      | "GET"    | "user"      | "status_code=200`component=simple-rbac`message=OK: /{/path/to/this};q=0.5"
        "/path/to/this"      | "PUT"    | "user"      | "status_code=403`component=simple-rbac`message=FORBIDDEN: /{/path/to/this};q=0.5"
        "/path/to/this"      | "POST"   | "user"      | "status_code=403`component=simple-rbac`message=FORBIDDEN: /{/path/to/this};q=0.5"
        "/path/to/this"      | "DELETE" | "user"      | "status_code=403`component=simple-rbac`message=FORBIDDEN: /{/path/to/this};q=0.5"
        "/path/to/this"      | "GET"    | "none"      | "status_code=403`component=simple-rbac`message=FORBIDDEN: /{/path/to/this};q=0.5"
        "/path/to/this"      | "PUT"    | "none"      | "status_code=403`component=simple-rbac`message=FORBIDDEN: /{/path/to/this};q=0.5"
        "/path/to/this"      | "POST"   | "none"      | "status_code=403`component=simple-rbac`message=FORBIDDEN: /{/path/to/this};q=0.5"
        "/path/to/this"      | "DELETE" | "none"      | "status_code=403`component=simple-rbac`message=FORBIDDEN: /{/path/to/this};q=0.5"
        "/path/to/that"      | "GET"    | "super"     | "status_code=200`component=simple-rbac`message=OK: /{/path/to/that};q=0.5"
        "/path/to/that"      | "PUT"    | "super"     | "status_code=200`component=simple-rbac`message=OK: /{/path/to/that};q=0.5"
        "/path/to/that"      | "POST"   | "super"     | "status_code=200`component=simple-rbac`message=OK: /{/path/to/that};q=0.5"
        "/path/to/that"      | "DELETE" | "super"     | "status_code=200`component=simple-rbac`message=OK: /{/path/to/that};q=0.5"
        "/path/to/that"      | "GET"    | "useradmin" | "status_code=200`component=simple-rbac`message=OK: /{/path/to/that};q=0.5"
        "/path/to/that"      | "PUT"    | "useradmin" | "status_code=200`component=simple-rbac`message=OK: /{/path/to/that};q=0.5"
        "/path/to/that"      | "POST"   | "user"      | "status_code=403`component=simple-rbac`message=Forbidden: /{/path/to/that};q=0.5"
        "/path/to/that"      | "DELETE" | "admin"     | "status_code=403`component=simple-rbac`message=Forbidden: /{/path/to/that};q=0.5"
        "/path/to/that"      | "POST"   | "super"     | "status_code=200`component=simple-rbac`message=OK: /{/path/to/that};q=0.5"
        "/path/to/that"      | "DELETE" | "super"     | "status_code=200`component=simple-rbac`message=OK: /{/path/to/that};q=0.5"
        "/path/to/test"      | "GET"    | "user"      | "status_code=200`component=simple-rbac`message=OK: /{/path/to/test};q=0.5"
        "/path/to/test"      | "POST"   | "useradmin" | "status_code=200`component=simple-rbac`message=OK: /{/path/to/test};q=0.5"
        "/path/to/test"      | "GET"    | "admin"     | "status_code=403`component=simple-rbac`message=Forbidden: /{/path/to/test};q=0.5"
        "/path/to/test"      | "POST"   | "super"     | "status_code=403`component=simple-rbac`message=Forbidden: /{/path/to/test};q=0.5"
        "/path/to/test"      | "PUT"    | "user"      | "status_code=403`component=simple-rbac`message=Forbidden: /{/path/to/test};q=0.5"
        "/path/to/test"      | "DELETE" | "useradmin" | "status_code=403`component=simple-rbac`message=Forbidden: /{/path/to/test};q=0.5"
        "/path/to/something" | "GET"    | "user"      | "status_code=404`component=simple-rbac`message=Not Found: /{/path/to/something};q=0.5"
        "/path/to/something" | "GET"    | "super"     | "status_code=404`component=simple-rbac`message=Not Found: /{/path/to/something};q=0.5"
        "/path/to/something" | "GET"    | "admin"     | "status_code=404`component=simple-rbac`message=Not Found: /{/path/to/something};q=0.5"
        "/path/to/something" | "POST"   | "none"      | "status_code=404`component=simple-rbac`message=Not Found: /{/path/to/something};q=0.5"
        "/path/to/something" | "PUT"    | "useradmin" | "status_code=404`component=simple-rbac`message=Not Found: /{/path/to/something};q=0.5"
    }
}
