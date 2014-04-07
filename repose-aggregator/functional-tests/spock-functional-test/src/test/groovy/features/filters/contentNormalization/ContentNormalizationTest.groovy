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

    @Unroll("Send req with Accept Headers #sendAcceptHeaders when content normalizing will be #acceptHeaders")
    def "When content normalizing with Accept Headers contains #sendAcceptHeaders then Accept Headers #acceptHeaders" () {
        given:
        def headers = []
        def acceptHeaderList = acceptHeaders.split(',')
        if(sendAcceptHeaders != null){
            sendAcceptHeaders.split(',').each {
                headers << ['accept': it]
            }
            //headers = ['accept':sendAcceptHeaders]
        }


        when:
        MessageChain mc = null
        if(headers.size() == 0 )
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
        mc.receivedResponse.code == '200'
        mc.handlings[0].request.headers.findAll("accept") == acceptHeaderList

        where:
        sendAcceptHeaders                               |acceptHeaders
        'application/xml'                               |'application/xml'
        'application/xml,application/json'              |'application/json'
        'application/other'                             |'application/other'
        'application/other,application/xml'             |'application/xml'
        'html/text,application/xml'                     |'application/xml'
        'application/xml,html/text'                     |'application/xml'
        'application/xml,html/text,application/json'    |'application/json'
        '*/*,application/json'                          |'application/json'
        '*/*'                                           |'application/json'
        null                                            |'application/json'
        'application/json;q=1,application/xml;q=0.5'    |'application/json'
        'application/xml;q=1,application/json;q=0.5'    |'application/json'
        'application/xml;q=1'                           |'application/xml'
        '*/json'                                        |'application/json'
        '*/other'                                       |'application/json'

    }
}
