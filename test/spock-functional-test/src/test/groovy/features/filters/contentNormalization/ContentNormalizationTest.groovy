package features.filters.contentNormalization

import framework.ReposeValveTest
import org.rackspace.gdeproxy.Deproxy
import org.rackspace.gdeproxy.MessageChain

/**
 * User: dimi5963
 * Date: 9/9/13
 * Time: 10:55 AM
 */
class ContentNormalizationTest extends ReposeValveTest {

    def headers = [
            'user1':'usertest1',
            'X-Auth-Token':'358484212:99493',
            'X-First-Filter':'firstValue',
            'X-SeCoND-Filter':'secondValue',
            'X-third-filter':'thirdValue',
            'X-last-Filter':'lastValue',
            'X-User-Token':'something'
    ]

    def setupSpec() {
        repose.applyConfigs(
                "features/filters/headerNormalization/"
        )
        repose.start()
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.getProperty("target.port").toInteger())

    }

    def cleanupSpec() {
        repose.stop()
        deproxy.shutdown()
    }

    def "When Filtering Based on URI and Method" () {
        when:
        MessageChain mc =
            deproxy.makeRequest(
                    [
                            method: 'GET',
                            url:reposeEndpoint + "/v1/usertest1/servers/something",
                            headers:headers
                    ])

        then:
        mc.handlings.size() == 0
        mc.orphanedHandlings.size() == 1
        mc.orphanedHandlings[0].request.headers.getFirstValue("x-auth-token") == '358484212:99493'
        mc.orphanedHandlings[0].request.headers.getFirstValue("x-first-filter") == 'firstValue'
        mc.orphanedHandlings[0].request.headers.findAll("x-second-filter") == []
        mc.orphanedHandlings[0].request.headers.findAll("x-third-filter") == []
        mc.orphanedHandlings[0].request.headers.findAll("x-last-filter") == []
        mc.orphanedHandlings[0].request.headers.getFirstValue("via").contains('1.1 localhost:8888 (Repose/')
        mc.receivedResponse.code == '200'
    }

    def "When Filtering Based on URI"(){
        when:
        MessageChain mc =
            deproxy.makeRequest(
                    [
                            method: 'POST',
                            url:reposeEndpoint + "/v1/usertest1/servers/something",
                            headers:headers
                    ])

        then:
        mc.handlings.size() == 0
        mc.orphanedHandlings.size() == 1
        mc.orphanedHandlings[0].request.headers.getFirstValue("x-auth-token") == '358484212:99493'
        mc.orphanedHandlings[0].request.headers.getFirstValue("x-second-filter") == 'secondValue'
        mc.orphanedHandlings[0].request.headers.findAll("x-first-filter") == []
        mc.orphanedHandlings[0].request.headers.findAll("x-third-filter") == []
        mc.orphanedHandlings[0].request.headers.findAll("x-last-filter") == []
        mc.orphanedHandlings[0].request.headers.getFirstValue("via").contains('1.1 localhost:8888 (Repose/')
        mc.receivedResponse.code == '200'

    }

    def "When Filtering Based on Method"(){
        when:
        MessageChain mc =
            deproxy.makeRequest(
                    [
                            method: 'POST',
                            url:reposeEndpoint + "/v1/usertest1/resources/something",
                            headers:headers
                    ])
        then:
        mc.handlings.size() == 0
        mc.orphanedHandlings.size() == 1
        mc.orphanedHandlings[0].request.headers.getFirstValue("x-auth-token") == '358484212:99493'
        mc.orphanedHandlings[0].request.headers.getFirstValue("x-third-filter") == 'thirdValue'
        mc.orphanedHandlings[0].request.headers.findAll("x-second-filter") == []
        mc.orphanedHandlings[0].request.headers.findAll("x-first-filter") == []
        mc.orphanedHandlings[0].request.headers.findAll("x-last-filter") == []
        mc.orphanedHandlings[0].request.headers.getFirstValue("via").contains('1.1 localhost:8888 (Repose/')
        mc.receivedResponse.code == '200'
    }

    def "When Filtering using catch all"(){
        when:
        MessageChain mc =
            deproxy.makeRequest(
                    [
                            method: 'GET',
                            url:reposeEndpoint + "/v1/usertest1/resources/something",
                            headers:headers
                    ])
        then:
        mc.handlings.size() == 1
        mc.orphanedHandlings.size() == 0
        mc.handlings[0].request.headers.getFirstValue("x-auth-token") == '358484212:99493'
        mc.handlings[0].request.headers.getFirstValue("x-user-token") == 'something'
        mc.handlings[0].request.headers.getFirstValue("user1") == 'usertest1'
        mc.handlings[0].request.headers.getFirstValue("x-last-filter") == 'lastValue'
        mc.orphanedHandlings[0].request.headers.findAll("x-second-filter") == 'secondValue'
        mc.orphanedHandlings[0].request.headers.findAll("x-third-filter") == 'thirdValue'
        mc.orphanedHandlings[0].request.headers.findAll("x-first-filter") == 'firstValue'
        mc.handlings[0].request.headers.getFirstValue("via").contains('1.1 localhost:8888 (Repose/')
        mc.receivedResponse.code == '200'
    }
}
