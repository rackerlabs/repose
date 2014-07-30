package features.filters.ratelimiting
import framework.ReposeValveTest
import org.rackspace.deproxy.Deproxy
import spock.lang.Unroll

class GlobalLimitsTest extends ReposeValveTest {

    def setupSpec() {
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort)

        def params = properties.getDefaultTemplateParams()
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/filters/ratelimiting/globalRateLimit", params)
        repose.start()
    }

    def cleanupSpec() {
        if (repose)
            repose.stop()
        if (deproxy)
            deproxy.shutdown()
    }

    @Unroll("Request should be limited by the global limit, regardless of #user or #group.")
    def "All requests should be limited by the global limit."() {

        given:
        def response
        def headers = ['X-PP-User': user, 'X-PP-Groups': group]

        when: "we make multiple requests"
        response = deproxy.makeRequest(method: method, url: reposeEndpoint + url, headers: headers)

        then: "it should limit based off of the global rate limit"
        response.receivedResponse.code == responseCode


        where:
        url                      | user       | group      | method     | responseCode

        "/test1"                 | "user1"    | "group1"   | "GET"      | "200"
        "/test1"                 | "user1"    | "group1"   | "GET"      | "200"
        "/test1"                 | "user1"    | "group1"   | "GET"      | "200"
        "/test1"                 | "user1"    | "group1"   | "GET"      | "503"
        "/test1"                 | "user2"    | "group1"   | "GET"      | "503"
        "/test1"                 | "user3"    | "group2"   | "GET"      | "503"
        "/test1"                 | "user1"    | "group1"   | "POST"     | "503"
        "/test1"                 | "user2"    | "group1"   | "POST"     | "503"
        "/test1"                 | "user3"    | "group2"   | "POST"     | "503"

        "/test2"                 | "user1"    | "group1"   | "GET"      | "200"
        "/test2"                 | "user2"    | "group2"   | "POST"     | "200"
        "/test2"                 | "user3"    | "group3"   | "PUT"      | "200"
        "/test2"                 | "user4"    | "group4"   | "PATCH"    | "503"

        "/test3"                 | "user1"    | "group1"   | "POST"     | "200"
        "/test3"                 | "user1"    | "group1"   | "PATCH"    | "200"
        "/test3"                 | "user2"    | "group2"   | "PUT"      | "200"
        "/test3"                 | "user3"    | "group3"   | "POST"     | "200"
        "/test3"                 | "user3"    | "group3"   | "POST"     | "200"
        "/test3"                 | "user4"    | "group4"   | "GET"      | "200"
        "/test3"                 | "user3"    | "group3"   | "GET"      | "200"
        "/test3"                 | "user2"    | "group2"   | "GET"      | "200"
        "/test3"                 | "user1"    | "group1"   | "GET"      | "503"
    }
}
