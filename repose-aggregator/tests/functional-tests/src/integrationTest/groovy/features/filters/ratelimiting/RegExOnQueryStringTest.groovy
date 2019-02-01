/*
 * _=_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_=
 * Repose
 * _-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-
 * Copyright (C) 2010 - 2015 Rackspace US, Inc.
 * _-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * =_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_=_
 */
package features.filters.ratelimiting

import org.junit.experimental.categories.Category
import org.openrepose.framework.test.ReposeValveTest
import org.rackspace.deproxy.Deproxy
import scaffold.category.Filters
import spock.lang.Unroll

/**
 * Created by jennyvo on 6/26/14.
 */
@Category(Filters)
class RegExOnQueryStringTest extends ReposeValveTest {

    def setupSpec() {
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort)

        def params = properties.getDefaultTemplateParams()
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/filters/ratelimiting/regexquerystring", params)
        repose.start()
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
        url                     | group    | respcode
        "/domain?name=query1"   | "dbaas"  | "200"
        "/domain?name=query1"   | "dbaas"  | "200"
        "/domain?name=query1"   | "dbaas"  | "200"
        "/domain?name=query1"   | "dbaas"  | "413"
        "/domain?search=query1" | "dbaas"  | "200"
        "/domain?search=query2" | "dbaas"  | "200"
        "/domain?name=query2"   | "dbaas"  | "413"
        "/domain?name=query1"   | "test"   | "200"
        "/domain?name=query2"   | "test"   | "200"
        "/domain?search=query1" | "dbaas2" | "200"
        "/domain?search=query2" | "dbaas2" | "200"
        "/domain?search=query3" | "dbaas2" | "200"
        "/domain?search=query4" | "dbaas2" | "413"
        //multiple queries
        "/domain?name=query1&search=query2" | "dbaas2" | "413"
        "/domain?time=query2&name=query1" | "dbaas2" | "200"
        "/domain?name=query1&search=query2" | "dbaas" | "413"
        "/domain?name=query1%26search=query2" | "dbaas" | "413"
        "/domain?n%61me=query2&search=query3" | "dbaas" | "413"
        /* So I don't think the next test is valid. I believe the question mark that delimits the query string cannot
         * be percent encoded. Again, this is referencing http://tools.ietf.org/html/rfc3986#appendix-A where an
         * explicit question mark precedes the query element. */
        //"/domain%3Fname=query2&search=query3"  | "dbaas"       | "413"
        //multi query parameters
        "/info?name='test'&age=100&query='test'" | "dbaas3" | "200"
        "/info?name='test2'&age=56&query='test'" | "dbaas3" | "200"
        "/info?name='test2'&age=99&query='test'" | "dbaas3" | "200"
        "/info?name='test2'&age=99&query='test'" | "dbaas3" | "413"
        "/info?age=99&query='test'&name='test2'" | "dbaas3" | "413"
        "/info?name='test2'&age=99" | "dbaas3" | "200"
        "/info?id=234" | "dbaas3" | "200"
        "/info?name='test'" | "dbaas3" | "200"
        "/info?age=69&name='repose'" | "dbaas3" | "200"
        "/info?id=123&name='test'" | "dbaas3" | "200"
        "/info?query='test'" | "dbaas3" | "200"
        "/info?age=100" | "dbaas3" | "200"
        "/info?name='test'" | "dbaas3" | "200"
        //working with %20 as space in config
        "/info?time%20search='06-30-2014%2012:00.00.000'&other='test'" | "dbaas4" | "200"
        "/info?time%20search='06-30-2014%2012:59.00.000'&other='test'" | "dbaas4" | "200"
        "/info?time%20search='06-30-2014%2012:00.00.000'&other='test'&search='test'" | "dbaas4" | "413"
        "/info?time='06-30-2014%2012:00.00.000'" | "dbaas4" | "200"
    }

    def "Body parameters in requests should be ignored and the body should not be modified"() {
        given:
        def headers = ['X-PP-User': 'user1', 'X-PP-Groups': 'body', 'Content-Type': 'application/x-www-form-urlencoded']
        String body = 'name=one'

        when:
        def mc = deproxy.makeRequest(
                method: 'POST',
                url: reposeEndpoint + '/path?foo=bar',
                headers: headers,
                requestBody: body)

        then:
        mc.receivedResponse.code.toInteger() == 200
        mc.handlings.size() == 1
        new String(mc.handlings[0].request.body) == body
    }
}
