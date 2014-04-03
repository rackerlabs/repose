package features.filters.contentNormalization

import framework.ReposeValveTest
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain
import spock.lang.Unroll

class ContentNormalizationTest extends ReposeValveTest {

    def setupSpec() {
        def params = properties.defaultTemplateParams
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/filters/contentnormalization", params)
        repose.start()
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort)

    }

    def cleanupSpec() {
        repose.stop()
        deproxy.shutdown()
    }

    def "When content normalizing with Accept Headers #acceptHeaders" () {
        given:
        def headers = null
        if(acceptHeaders != null){
            headers = 'accept: '+acceptHeaders
        }

        when:
        MessageChain mc = null
        if(headers != null)
            mc = deproxy.makeRequest(
                    [
                            method: 'GET',
                            url:reposeEndpoint + "/v1/usertest1/servers/something"
                    ])
        else
            mc = deproxy.makeRequest(
                    [
                            method: 'GET',
                            url:reposeEndpoint + "/v1/usertest1/servers/something",
                            headers:headers
                    ])


        then:
        mc.handlings.size() == 1
        mc.handlings[0].request.headers.findAll("accept").contains('application/json')
        mc.handlings[0].request.headers.getFirstValue("accept") == 'application/json'
        mc.receivedResponse.code == '200'

        where:
        acceptHeaders << [
                'application/json',
                'application/json, application/xml',
                'application/json, application/xml, application/other',
                'application/json;q=0, application/xml, application/other',
                'application/json;q=0, application/xml, application/doesnotexist',
                'application/json+xml, application/atom+xml, application/doesnotexist',
                'application/xml+json;useragent=0, application/atom+xml, application/doesnotexist',
                '',
                null,
                '*/*',
                '*/json',
                '*/other'
        ]
    }

    @Unroll
    def "When content normalizing with Accept Headers contains #acceptHeaders filter #requestHeaders" () {
        given:
        def headers = null
        def acceptHeaderList = requestHeaders.split(',')
        if(acceptHeaders != null){
            headers = 'accept: '+acceptHeaders
        }


        when:
        MessageChain mc = null
        if(headers == null)
            mc = deproxy.makeRequest(
                    [
                            method: 'GET',
                            url:reposeEndpoint + "/v1/usertest1/servers/something"
                    ])
        else
            mc = deproxy.makeRequest(
                    [
                            method: 'GET',
                            url:reposeEndpoint + "/v1/usertest1/servers/something",
                            headers:headers
                    ])


        then:
        mc.handlings.size() == 1
        mc.handlings[0].request.headers.findAll("accept") == acceptHeaderList
        //mc.handlings[0].request.headers.findAll("accept").contains(requestHeaders)
        mc.receivedResponse.code == '200'

        where:
        acceptHeaders                                   |requestHeaders
        'application/xml'                               |'application/xml'
        'application/xml,application/json'              |'application/xml,application/json'
        'application/other'                             |'application/other'
        'application/other,application/xml'             |'application/other,application/xml'
        'html/text,application/xml'                     |'application/xml'
        'application/doesnotexist,application/other'    |'application/other'
        '*/*'                                           |'application/json'
        null                                            |'application/json'
        'application/json;q=1,application/xml;q=0.5'    |'application/json,application/xml'
        'application/xml;q=1'                           |'application/xml'
        '*/json'                                        |'application/json'
        '*/other'                                       |'application/json'

    }
}
