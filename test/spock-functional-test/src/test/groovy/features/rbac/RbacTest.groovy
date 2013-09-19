package features.rbac

import framework.ReposeValveTest
import org.rackspace.gdeproxy.Deproxy
import org.rackspace.gdeproxy.MessageChain
import spock.lang.Unroll

/**
 * RBAC tests ported from python
 */
class RbacTest extends ReposeValveTest {

    def setupSpec() {
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.getProperty("target.port").toInteger())

        repose.applyConfigs("features/rbac")
        repose.start()

        sleep(5000)
    }

    def cleanupSpec() {
        if (repose)
            repose.stop()
        if (deproxy)
            deproxy.shutdown()
    }

    @Unroll
    def "When interacting with Repose and using RBAC"() {
        setup:
        MessageChain messageChain
        boolean isCodeValid

        when:
        messageChain = deproxy.makeRequest(url: reposeEndpoint + path, method: method, headers: headers)
        isCodeValid = false
        for (String vcr in validResponseCodes) {
            if (messageChain.receivedResponse.code.equals(vcr)) {
                isCodeValid = true
                break
            }
        }

        then:
        isCodeValid
        messageChain.handlings.size() == numHandlings

        where:
        path                        | method    | headers                                                 | validResponseCodes         | numHandlings
        "/widgets"                  | "GET"     | ["X-Roles": "cwaas:observer,cwaas:creator,cwaas:admin"] | ["200"]                    | 1
        "/widgets"                  | "GET"     | ["X-Roles": "cwaas:observer,cwaas:creator"]             | ["200"]                    | 1
        "/widgets"                  | "GET"     | ["X-Roles": "cwaas:observer,cwaas:admin"]               | ["200"]                    | 1
        "/widgets"                  | "GET"     | ["X-Roles": "cwaas:observer"]                           | ["200"]                    | 1
        "/widgets"                  | "GET"     | ["X-Roles": "cwaas:creator,cwaas:admin"]                | ["200"]                    | 1
        "/widgets"                  | "GET"     | ["X-Roles": "cwaas:creator"]                            | ["200"]                    | 1
        "/widgets"                  | "GET"     | ["X-Roles": "cwaas:admin"]                              | ["200"]                    | 1
        "/widgets"                  | "GET"     | ["X-Roles": ""]                                         | ["200"]                    | 1
        "/widgets"                  | "POST"    | ["X-Roles": "cwaas:observer,cwaas:creator,cwaas:admin"] | ["200"]                    | 1
        "/widgets"                  | "POST"    | ["X-Roles": "cwaas:observer,cwaas:creator"]             | ["200"]                    | 1
        "/widgets"                  | "POST"    | ["X-Roles": "cwaas:observer,cwaas:admin"]               | ["200"]                    | 1
        "/widgets"                  | "POST"    | ["X-Roles": "cwaas:observer"]                           | ["403","404","405"]        | 0
        "/widgets"                  | "POST"    | ["X-Roles": "cwaas:creator,cwaas:admin"]                | ["200"]                    | 1
        "/widgets"                  | "POST"    | ["X-Roles": "cwaas:creator"]                            | ["200"]                    | 1
        "/widgets"                  | "POST"    | ["X-Roles": "cwaas:admin"]                              | ["200"]                    | 1
        "/widgets"                  | "POST"    | ["X-Roles": ""]                                         | ["403","404","405"]        | 0
        "/widgets"                  | "PUT"     | ["X-Roles": "cwaas:observer,cwaas:creator,cwaas:admin"] | ["405"]                    | 0
        "/widgets"                  | "PUT"     | ["X-Roles": "cwaas:observer,cwaas:creator"]             | ["405"]                    | 0
        "/widgets"                  | "PUT"     | ["X-Roles": "cwaas:observer,cwaas:admin"]               | ["405"]                    | 0
        "/widgets"                  | "PUT"     | ["X-Roles": "cwaas:observer"]                           | ["405"]                    | 0
        "/widgets"                  | "PUT"     | ["X-Roles": "cwaas:creator,cwaas:admin"]                | ["405"]                    | 0
        "/widgets"                  | "PUT"     | ["X-Roles": "cwaas:creator"]                            | ["405"]                    | 0
        "/widgets"                  | "PUT"     | ["X-Roles": "cwaas:admin"]                              | ["405"]                    | 0
        "/widgets"                  | "PUT"     | ["X-Roles": ""]                                         | ["405"]                    | 0
        "/widgets"                  | "DELETE"  | ["X-Roles": "cwaas:observer,cwaas:creator,cwaas:admin"] | ["405"]                    | 0
        "/widgets"                  | "DELETE"  | ["X-Roles": "cwaas:observer,cwaas:creator"]             | ["405"]                    | 0
        "/widgets"                  | "DELETE"  | ["X-Roles": "cwaas:observer,cwaas:admin"]               | ["405"]                    | 0
        "/widgets"                  | "DELETE"  | ["X-Roles": "cwaas:observer"]                           | ["405"]                    | 0
        "/widgets"                  | "DELETE"  | ["X-Roles": "cwaas:creator,cwaas:admin"]                | ["405"]                    | 0
        "/widgets"                  | "DELETE"  | ["X-Roles": "cwaas:creator"]                            | ["405"]                    | 0
        "/widgets"                  | "DELETE"  | ["X-Roles": "cwaas:admin"]                              | ["405"]                    | 0
        "/widgets"                  | "DELETE"  | ["X-Roles": ""]                                         | ["405"]                    | 0
        "/widgets/1234"             | "GET"     | ["X-Roles": "cwaas:observer,cwaas:creator,cwaas:admin"] | ["200"]                    | 1
        "/widgets/1234"             | "GET"     | ["X-Roles": "cwaas:observer,cwaas:creator"]             | ["200"]                    | 1
        "/widgets/1234"             | "GET"     | ["X-Roles": "cwaas:observer,cwaas:admin"]               | ["200"]                    | 1
        "/widgets/1234"             | "GET"     | ["X-Roles": "cwaas:observer"]                           | ["200"]                    | 1
        "/widgets/1234"             | "GET"     | ["X-Roles": "cwaas:creator,cwaas:admin"]                | ["200"]                    | 1
        "/widgets/1234"             | "GET"     | ["X-Roles": "cwaas:creator"]                            | ["200"]                    | 1
        "/widgets/1234"             | "GET"     | ["X-Roles": "cwaas:admin"]                              | ["200"]                    | 1
        "/widgets/1234"             | "GET"     | ["X-Roles": ""]                                         | ["200"]                    | 1
        "/widgets/1234"             | "POST"    | ["X-Roles": "cwaas:observer,cwaas:creator,cwaas:admin"] | ["405"]                    | 0
        "/widgets/1234"             | "POST"    | ["X-Roles": "cwaas:observer,cwaas:creator"]             | ["405"]                    | 0
        "/widgets/1234"             | "POST"    | ["X-Roles": "cwaas:observer,cwaas:admin"]               | ["405"]                    | 0
        "/widgets/1234"             | "POST"    | ["X-Roles": "cwaas:observer"]                           | ["405"]                    | 0
        "/widgets/1234"             | "POST"    | ["X-Roles": "cwaas:creator,cwaas:admin"]                | ["405"]                    | 0
        "/widgets/1234"             | "POST"    | ["X-Roles": "cwaas:creator"]                            | ["405"]                    | 0
        "/widgets/1234"             | "POST"    | ["X-Roles": "cwaas:admin"]                              | ["405"]                    | 0
        "/widgets/1234"             | "POST"    | ["X-Roles": ""]                                         | ["405"]                    | 0
        "/widgets/1234"             | "PUT"     | ["X-Roles": "cwaas:observer,cwaas:creator,cwaas:admin"] | ["200"]                    | 1
        "/widgets/1234"             | "PUT"     | ["X-Roles": "cwaas:observer,cwaas:creator"]             | ["200"]                    | 1
        "/widgets/1234"             | "PUT"     | ["X-Roles": "cwaas:observer,cwaas:admin"]               | ["200"]                    | 1
        "/widgets/1234"             | "PUT"     | ["X-Roles": "cwaas:observer"]                           | ["403","404","405"]        | 0
        "/widgets/1234"             | "PUT"     | ["X-Roles": "cwaas:creator,cwaas:admin"]                | ["200"]                    | 1
        "/widgets/1234"             | "PUT"     | ["X-Roles": "cwaas:creator"]                            | ["200"]                    | 1
        "/widgets/1234"             | "PUT"     | ["X-Roles": "cwaas:admin"]                              | ["200"]                    | 1
        "/widgets/1234"             | "PUT"     | ["X-Roles": ""]                                         | ["403","404","405"]        | 0
        "/widgets/1234"             | "DELETE"  | ["X-Roles": "cwaas:observer,cwaas:creator,cwaas:admin"] | ["200"]                    | 1
        "/widgets/1234"             | "DELETE"  | ["X-Roles": "cwaas:observer,cwaas:creator"]             | ["403","404","405"]        | 0
        "/widgets/1234"             | "DELETE"  | ["X-Roles": "cwaas:observer,cwaas:admin"]               | ["200"]                    | 1
        "/widgets/1234"             | "DELETE"  | ["X-Roles": "cwaas:observer"]                           | ["403","404","405"]        | 0
        "/widgets/1234"             | "DELETE"  | ["X-Roles": "cwaas:creator,cwaas:admin"]                | ["200"]                    | 1
        "/widgets/1234"             | "DELETE"  | ["X-Roles": "cwaas:creator"]                            | ["403","404","405"]        | 0
        "/widgets/1234"             | "DELETE"  | ["X-Roles": "cwaas:admin"]                              | ["200"]                    | 1
        "/widgets/1234"             | "DELETE"  | ["X-Roles": ""]                                         | ["403","404","405"]        | 0
        "/gizmos"                   | "GET"     | ["X-Roles": "cwaas:observer,cwaas:creator,cwaas:admin"] | ["200"]                    | 1
        "/gizmos"                   | "GET"     | ["X-Roles": "cwaas:observer,cwaas:creator"]             | ["200"]                    | 1
        "/gizmos"                   | "GET"     | ["X-Roles": "cwaas:observer,cwaas:admin"]               | ["200"]                    | 1
        "/gizmos"                   | "GET"     | ["X-Roles": "cwaas:observer"]                           | ["200"]                    | 1
        "/gizmos"                   | "GET"     | ["X-Roles": "cwaas:creator,cwaas:admin"]                | ["200"]                    | 1
        "/gizmos"                   | "GET"     | ["X-Roles": "cwaas:creator"]                            | ["200"]                    | 1
        "/gizmos"                   | "GET"     | ["X-Roles": "cwaas:admin"]                              | ["200"]                    | 1
        "/gizmos"                   | "GET"     | ["X-Roles": ""]                                         | ["200"]                    | 1
        "/gizmos"                   | "POST"    | ["X-Roles": "cwaas:observer,cwaas:creator,cwaas:admin"] | ["200"]                    | 1
        "/gizmos"                   | "POST"    | ["X-Roles": "cwaas:observer,cwaas:creator"]             | ["200"]                    | 1
        "/gizmos"                   | "POST"    | ["X-Roles": "cwaas:observer,cwaas:admin"]               | ["200"]                    | 1
        "/gizmos"                   | "POST"    | ["X-Roles": "cwaas:observer"]                           | ["403","404","405"]        | 0
        "/gizmos"                   | "POST"    | ["X-Roles": "cwaas:creator,cwaas:admin"]                | ["200"]                    | 1
        "/gizmos"                   | "POST"    | ["X-Roles": "cwaas:creator"]                            | ["200"]                    | 1
        "/gizmos"                   | "POST"    | ["X-Roles": "cwaas:admin"]                              | ["200"]                    | 1
        "/gizmos"                   | "POST"    | ["X-Roles": ""]                                         | ["403","404","405"]        | 0
        "/gizmos"                   | "PUT"     | ["X-Roles": "cwaas:observer,cwaas:creator,cwaas:admin"] | ["405"]                    | 0
        "/gizmos"                   | "PUT"     | ["X-Roles": "cwaas:observer,cwaas:creator"]             | ["405"]                    | 0
        "/gizmos"                   | "PUT"     | ["X-Roles": "cwaas:observer,cwaas:admin"]               | ["405"]                    | 0
        "/gizmos"                   | "PUT"     | ["X-Roles": "cwaas:observer"]                           | ["405"]                    | 0
        "/gizmos"                   | "PUT"     | ["X-Roles": "cwaas:creator,cwaas:admin"]                | ["405"]                    | 0
        "/gizmos"                   | "PUT"     | ["X-Roles": "cwaas:creator"]                            | ["405"]                    | 0
        "/gizmos"                   | "PUT"     | ["X-Roles": "cwaas:admin"]                              | ["405"]                    | 0
        "/gizmos"                   | "PUT"     | ["X-Roles": ""]                                         | ["405"]                    | 0
        "/gizmos"                   | "DELETE"  | ["X-Roles": "cwaas:observer,cwaas:creator,cwaas:admin"] | ["405"]                    | 0
        "/gizmos"                   | "DELETE"  | ["X-Roles": "cwaas:observer,cwaas:creator"]             | ["405"]                    | 0
        "/gizmos"                   | "DELETE"  | ["X-Roles": "cwaas:observer,cwaas:admin"]               | ["405"]                    | 0
        "/gizmos"                   | "DELETE"  | ["X-Roles": "cwaas:observer"]                           | ["405"]                    | 0
        "/gizmos"                   | "DELETE"  | ["X-Roles": "cwaas:creator,cwaas:admin"]                | ["405"]                    | 0
        "/gizmos"                   | "DELETE"  | ["X-Roles": "cwaas:creator"]                            | ["405"]                    | 0
        "/gizmos"                   | "DELETE"  | ["X-Roles": "cwaas:admin"]                              | ["405"]                    | 0
        "/gizmos"                   | "DELETE"  | ["X-Roles": ""]                                         | ["405"]                    | 0
        "/gizmos/5678"              | "GET"     | ["X-Roles": "cwaas:observer,cwaas:creator,cwaas:admin"] | ["200"]                    | 1
        "/gizmos/5678"              | "GET"     | ["X-Roles": "cwaas:observer,cwaas:creator"]             | ["200"]                    | 1
        "/gizmos/5678"              | "GET"     | ["X-Roles": "cwaas:observer,cwaas:admin"]               | ["200"]                    | 1
        "/gizmos/5678"              | "GET"     | ["X-Roles": "cwaas:observer"]                           | ["200"]                    | 1
        "/gizmos/5678"              | "GET"     | ["X-Roles": "cwaas:creator,cwaas:admin"]                | ["200"]                    | 1
        "/gizmos/5678"              | "GET"     | ["X-Roles": "cwaas:creator"]                            | ["200"]                    | 1
        "/gizmos/5678"              | "GET"     | ["X-Roles": "cwaas:admin"]                              | ["200"]                    | 1
        "/gizmos/5678"              | "GET"     | ["X-Roles": ""]                                         | ["200"]                    | 1
        "/gizmos/5678"              | "POST"    | ["X-Roles": "cwaas:observer,cwaas:creator,cwaas:admin"] | ["405"]                    | 0
        "/gizmos/5678"              | "POST"    | ["X-Roles": "cwaas:observer,cwaas:creator"]             | ["405"]                    | 0
        "/gizmos/5678"              | "POST"    | ["X-Roles": "cwaas:observer,cwaas:admin"]               | ["405"]                    | 0
        "/gizmos/5678"              | "POST"    | ["X-Roles": "cwaas:observer"]                           | ["405"]                    | 0
        "/gizmos/5678"              | "POST"    | ["X-Roles": "cwaas:creator,cwaas:admin"]                | ["405"]                    | 0
        "/gizmos/5678"              | "POST"    | ["X-Roles": "cwaas:creator"]                            | ["405"]                    | 0
        "/gizmos/5678"              | "POST"    | ["X-Roles": "cwaas:admin"]                              | ["405"]                    | 0
        "/gizmos/5678"              | "POST"    | ["X-Roles": ""]                                         | ["405"]                    | 0
        "/gizmos/5678"              | "PUT"     | ["X-Roles": "cwaas:observer,cwaas:creator,cwaas:admin"] | ["200"]                    | 1
        "/gizmos/5678"              | "PUT"     | ["X-Roles": "cwaas:observer,cwaas:creator"]             | ["200"]                    | 1
        "/gizmos/5678"              | "PUT"     | ["X-Roles": "cwaas:observer,cwaas:admin"]               | ["200"]                    | 1
        "/gizmos/5678"              | "PUT"     | ["X-Roles": "cwaas:observer"]                           | ["403","404","405"]        | 0
        "/gizmos/5678"              | "PUT"     | ["X-Roles": "cwaas:creator,cwaas:admin"]                | ["200"]                    | 1
        "/gizmos/5678"              | "PUT"     | ["X-Roles": "cwaas:creator"]                            | ["200"]                    | 1
        "/gizmos/5678"              | "PUT"     | ["X-Roles": "cwaas:admin"]                              | ["200"]                    | 1
        "/gizmos/5678"              | "PUT"     | ["X-Roles": ""]                                         | ["403","404","405"]        | 0
        "/gizmos/5678"              | "DELETE"  | ["X-Roles": "cwaas:observer,cwaas:creator,cwaas:admin"] | ["200"]                    | 1
        "/gizmos/5678"              | "DELETE"  | ["X-Roles": "cwaas:observer,cwaas:creator"]             | ["403","404","405"]        | 0
        "/gizmos/5678"              | "DELETE"  | ["X-Roles": "cwaas:observer,cwaas:admin"]               | ["200"]                    | 1
        "/gizmos/5678"              | "DELETE"  | ["X-Roles": "cwaas:observer"]                           | ["403","404","405"]        | 0
        "/gizmos/5678"              | "DELETE"  | ["X-Roles": "cwaas:creator,cwaas:admin"]                | ["200"]                    | 1
        "/gizmos/5678"              | "DELETE"  | ["X-Roles": "cwaas:creator"]                            | ["403","404","405"]        | 0
        "/gizmos/5678"              | "DELETE"  | ["X-Roles": "cwaas:admin"]                              | ["200"]                    | 1
        "/gizmos/5678"              | "DELETE"  | ["X-Roles": ""]                                         | ["403","404","405"]        | 0
        "/widgets/1234/gizmos"      | "GET"     | ["X-Roles": "cwaas:observer,cwaas:creator,cwaas:admin"] | ["200"]                    | 1 
        "/widgets/1234/gizmos"      | "GET"     | ["X-Roles": "cwaas:observer,cwaas:creator"]             | ["200"]                    | 1 
        "/widgets/1234/gizmos"      | "GET"     | ["X-Roles": "cwaas:observer,cwaas:admin"]               | ["200"]                    | 1 
        "/widgets/1234/gizmos"      | "GET"     | ["X-Roles": "cwaas:observer"]                           | ["200"]                    | 1 
        "/widgets/1234/gizmos"      | "GET"     | ["X-Roles": "cwaas:creator,cwaas:admin"]                | ["200"]                    | 1 
        "/widgets/1234/gizmos"      | "GET"     | ["X-Roles": "cwaas:creator"]                            | ["200"]                    | 1 
        "/widgets/1234/gizmos"      | "GET"     | ["X-Roles": "cwaas:admin"]                              | ["200"]                    | 1 
        "/widgets/1234/gizmos"      | "GET"     | ["X-Roles": ""]                                         | ["200"]                    | 1 
        "/widgets/1234/gizmos"      | "POST"    | ["X-Roles": "cwaas:observer,cwaas:creator,cwaas:admin"] | ["200"]                    | 1
        "/widgets/1234/gizmos"      | "POST"    | ["X-Roles": "cwaas:observer,cwaas:creator"]             | ["200"]                    | 1
        "/widgets/1234/gizmos"      | "POST"    | ["X-Roles": "cwaas:observer,cwaas:admin"]               | ["200"]                    | 1
        "/widgets/1234/gizmos"      | "POST"    | ["X-Roles": "cwaas:observer"]                           | ["403","404","405"]        | 0
        "/widgets/1234/gizmos"      | "POST"    | ["X-Roles": "cwaas:creator,cwaas:admin"]                | ["200"]                    | 1
        "/widgets/1234/gizmos"      | "POST"    | ["X-Roles": "cwaas:creator"]                            | ["200"]                    | 1
        "/widgets/1234/gizmos"      | "POST"    | ["X-Roles": "cwaas:admin"]                              | ["200"]                    | 1
        "/widgets/1234/gizmos"      | "POST"    | ["X-Roles": ""]                                         | ["403","404","405"]        | 0
        "/widgets/1234/gizmos"      | "PUT"     | ["X-Roles": "cwaas:observer,cwaas:creator,cwaas:admin"] | ["405"]                    | 0
        "/widgets/1234/gizmos"      | "PUT"     | ["X-Roles": "cwaas:observer,cwaas:creator"]             | ["405"]                    | 0
        "/widgets/1234/gizmos"      | "PUT"     | ["X-Roles": "cwaas:observer,cwaas:admin"]               | ["405"]                    | 0
        "/widgets/1234/gizmos"      | "PUT"     | ["X-Roles": "cwaas:observer"]                           | ["405"]                    | 0
        "/widgets/1234/gizmos"      | "PUT"     | ["X-Roles": "cwaas:creator,cwaas:admin"]                | ["405"]                    | 0
        "/widgets/1234/gizmos"      | "PUT"     | ["X-Roles": "cwaas:creator"]                            | ["405"]                    | 0
        "/widgets/1234/gizmos"      | "PUT"     | ["X-Roles": "cwaas:admin"]                              | ["405"]                    | 0
        "/widgets/1234/gizmos"      | "PUT"     | ["X-Roles": ""]                                         | ["405"]                    | 0
        "/widgets/1234/gizmos"      | "DELETE"  | ["X-Roles": "cwaas:observer,cwaas:creator,cwaas:admin"] | ["405"]                    | 0 
        "/widgets/1234/gizmos"      | "DELETE"  | ["X-Roles": "cwaas:observer,cwaas:creator"]             | ["405"]                    | 0
        "/widgets/1234/gizmos"      | "DELETE"  | ["X-Roles": "cwaas:observer,cwaas:admin"]               | ["405"]                    | 0 
        "/widgets/1234/gizmos"      | "DELETE"  | ["X-Roles": "cwaas:observer"]                           | ["405"]                    | 0
        "/widgets/1234/gizmos"      | "DELETE"  | ["X-Roles": "cwaas:creator,cwaas:admin"]                | ["405"]                    | 0 
        "/widgets/1234/gizmos"      | "DELETE"  | ["X-Roles": "cwaas:creator"]                            | ["405"]                    | 0
        "/widgets/1234/gizmos"      | "DELETE"  | ["X-Roles": "cwaas:admin"]                              | ["405"]                    | 0 
        "/widgets/1234/gizmos"      | "DELETE"  | ["X-Roles": ""]                                         | ["405"]                    | 0
        "/widgets/1234/gizmos/5678" | "GET"     | ["X-Roles": "cwaas:observer,cwaas:creator,cwaas:admin"] | ["200"]                    | 1
        "/widgets/1234/gizmos/5678" | "GET"     | ["X-Roles": "cwaas:observer,cwaas:creator"]             | ["200"]                    | 1
        "/widgets/1234/gizmos/5678" | "GET"     | ["X-Roles": "cwaas:observer,cwaas:admin"]               | ["200"]                    | 1
        "/widgets/1234/gizmos/5678" | "GET"     | ["X-Roles": "cwaas:observer"]                           | ["200"]                    | 1
        "/widgets/1234/gizmos/5678" | "GET"     | ["X-Roles": "cwaas:creator,cwaas:admin"]                | ["200"]                    | 1
        "/widgets/1234/gizmos/5678" | "GET"     | ["X-Roles": "cwaas:creator"]                            | ["200"]                    | 1
        "/widgets/1234/gizmos/5678" | "GET"     | ["X-Roles": "cwaas:admin"]                              | ["200"]                    | 1
        "/widgets/1234/gizmos/5678" | "GET"     | ["X-Roles": ""]                                         | ["200"]                    | 1
        "/widgets/1234/gizmos/5678" | "POST"    | ["X-Roles": "cwaas:observer,cwaas:creator,cwaas:admin"] | ["405"]                    | 0
        "/widgets/1234/gizmos/5678" | "POST"    | ["X-Roles": "cwaas:observer,cwaas:creator"]             | ["405"]                    | 0
        "/widgets/1234/gizmos/5678" | "POST"    | ["X-Roles": "cwaas:observer,cwaas:admin"]               | ["405"]                    | 0
        "/widgets/1234/gizmos/5678" | "POST"    | ["X-Roles": "cwaas:observer"]                           | ["405"]                    | 0
        "/widgets/1234/gizmos/5678" | "POST"    | ["X-Roles": "cwaas:creator,cwaas:admin"]                | ["405"]                    | 0
        "/widgets/1234/gizmos/5678" | "POST"    | ["X-Roles": "cwaas:creator"]                            | ["405"]                    | 0
        "/widgets/1234/gizmos/5678" | "POST"    | ["X-Roles": "cwaas:admin"]                              | ["405"]                    | 0
        "/widgets/1234/gizmos/5678" | "POST"    | ["X-Roles": ""]                                         | ["405"]                    | 0
        "/widgets/1234/gizmos/5678" | "PUT"     | ["X-Roles": "cwaas:observer,cwaas:creator,cwaas:admin"] | ["405"]                    | 0
        "/widgets/1234/gizmos/5678" | "PUT"     | ["X-Roles": "cwaas:observer,cwaas:creator"]             | ["405"]                    | 0
        "/widgets/1234/gizmos/5678" | "PUT"     | ["X-Roles": "cwaas:observer,cwaas:admin"]               | ["405"]                    | 0
        "/widgets/1234/gizmos/5678" | "PUT"     | ["X-Roles": "cwaas:observer"]                           | ["405"]                    | 0
        "/widgets/1234/gizmos/5678" | "PUT"     | ["X-Roles": "cwaas:creator,cwaas:admin"]                | ["405"]                    | 0
        "/widgets/1234/gizmos/5678" | "PUT"     | ["X-Roles": "cwaas:creator"]                            | ["405"]                    | 0
        "/widgets/1234/gizmos/5678" | "PUT"     | ["X-Roles": "cwaas:admin"]                              | ["405"]                    | 0
        "/widgets/1234/gizmos/5678" | "PUT"     | ["X-Roles": ""]                                         | ["405"]                    | 0
        "/widgets/1234/gizmos/5678" | "DELETE"  | ["X-Roles": "cwaas:observer,cwaas:creator,cwaas:admin"] | ["200"]                    | 1
        "/widgets/1234/gizmos/5678" | "DELETE"  | ["X-Roles": "cwaas:observer,cwaas:creator"]             | ["403","404","405"]        | 0
        "/widgets/1234/gizmos/5678" | "DELETE"  | ["X-Roles": "cwaas:observer,cwaas:admin"]               | ["200"]                    | 1
        "/widgets/1234/gizmos/5678" | "DELETE"  | ["X-Roles": "cwaas:observer"]                           | ["403","404","405"]        | 0
        "/widgets/1234/gizmos/5678" | "DELETE"  | ["X-Roles": "cwaas:creator,cwaas:admin"]                | ["200"]                    | 1
        "/widgets/1234/gizmos/5678" | "DELETE"  | ["X-Roles": "cwaas:creator"]                            | ["403","404","405"]        | 0
        "/widgets/1234/gizmos/5678" | "DELETE"  | ["X-Roles": "cwaas:admin"]                              | ["200"]                    | 1
        "/widgets/1234/gizmos/5678" | "DELETE"  | ["X-Roles": ""]                                         | ["403","404","405"]        | 0
    }
}
