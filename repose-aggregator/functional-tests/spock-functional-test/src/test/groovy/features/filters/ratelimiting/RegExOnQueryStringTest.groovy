package features.filters.ratelimiting

import framework.ReposeValveTest
import org.rackspace.deproxy.Deproxy
import spock.lang.Unroll

/**
 * Created by jennyvo on 6/26/14.
 */
class RegExOnQueryStringTest extends ReposeValveTest {

    def setupSpec() {
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort)

        def params = properties.getDefaultTemplateParams()
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/filters/ratelimiting/regexquerystring", params)
        repose.start()
    }

    def cleanupSpec() {
        if (repose)
            repose.stop()
        if (deproxy)
            deproxy.shutdown()
    }

    @Unroll("Request with uri query #url through RL group #group should resp #respcode")
    def "Requests to urls with query string should apply limit on capture group"() {

        given:
        def mc
        def headers = ['X-PP-User': 'user1', 'X-PP-Groups': group]


        when: "we make one request to the first url"
        mc = deproxy.makeRequest(url: reposeEndpoint + url, headers: headers)
        then: "it should make it to the origin service"
        mc.receivedResponse.code == respcode
        if (respcode == "200")
            mc.handlings.size() == 1


        where:
        url                             | group         | respcode
        "/domain?name=query1"           |"dbaas"        |"200"
        "/domain?name=query1"           |"dbaas"        |"200"
        "/domain?name=query1"           |"dbaas"        |"200"
        "/domain?name=query1"           |"dbaas"        |"413"
        "/domain?search=query1"         |"dbaas"        |"200"
        "/domain?search=query2"         |"dbaas"        |"200"
        "/domain?name=query2"           |"dbaas"        |"413"
        "/domain?name=query1"           |"test"         |"200"
        "/domain?name=query2"           |"test"         |"200"
        "/domain?search=query1"         |"dbaas2"       |"200"
        "/domain?search=query2"         |"dbaas2"       |"200"
        "/domain?search=query3"         |"dbaas2"       |"200"
        "/domain?search=query4"         |"dbaas2"       |"413"
        //multiple queries
        "/domain?name=query1&search=query2"    |"dbaas2"      |"413"
        "/domain?time=query2&name=query1"      |"dbaas2"      |"200"
        "/domain?name=query1&search=query2"    |"dbaas"       |"413"
        "/domain?name=query1%26search=query2"  |"dbaas"       |"413"
        /* So I don't think the next test is valid. I believe the question mark that delimits the query string cannot
         * be percent encoded. Again, this is referencing http://tools.ietf.org/html/rfc3986#appendix-A where an
         * explicit question mark precedes the query element. */
        //"/domain%3Fname=query2&search=query3"  |"dbaas"       |"413"
        "/domain?n%61me=query2&search=query3"  |"dbaas"       |"413"
    }
}