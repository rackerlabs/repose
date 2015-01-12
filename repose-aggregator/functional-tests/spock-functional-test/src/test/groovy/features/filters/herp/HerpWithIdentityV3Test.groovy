package features.filters.herp

import framework.ReposeValveTest
import framework.mocks.MockIdentityV3Service
import groovy.json.JsonSlurper
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain

/**
 * Created by jennyvo on 1/12/15.
 */
class HerpWithIdentityV3Test extends ReposeValveTest{
    def static originEndpoint
    def static identityEndpoint
    def static MockIdentityV3Service fakeIdentityV3Service

    def setupSpec() {
        deproxy = new Deproxy()
        def params = properties.defaultTemplateParams
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs('features/filters/herp', params)
        repose.configurationProvider.applyConfigs('features/filters/herp/withIdentityV3', params)
        repose.start()

        originEndpoint = deproxy.addEndpoint(properties.targetPort, 'origin service')
        fakeIdentityV3Service = new MockIdentityV3Service(properties.identityPort, properties.targetPort)
        fakeIdentityV3Service.resetCounts()
        identityEndpoint = deproxy.addEndpoint(properties.identityPort,
                'identity service', null,fakeIdentityV3Service.handler)
    }

    def cleanupSpec() {
        if(deproxy)
            deproxy.shutdown()
        if(repose)
            repose.stop()
    }


    def "When using herp filter with identity V3 filter the set of headers include projectId will be added to log"() {
        given:
        List listattr = ["GUI", "ServiceCode", "Region", "DataCenter", "Timestamp", "Request", "Method", "URL", "Parameters",
                         "UserName", "ImpersonatorName", "ProjectID", "Role", "UserAgent", "Response", "Code", "Message"]
        reposeLogSearch.cleanLog()

        when: "I send a GET request to Repose with an X-Subject-Token header"
        fakeIdentityV3Service.resetCounts()
        fakeIdentityV3Service.default_region = "DFW"
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint, method: 'GET', headers: ['X-Subject-Token': fakeIdentityV3Service.client_token])

        String logLine = reposeLogSearch.searchByString("INFO  highly-efficient-record-processor")
        String jsonpart = logLine.substring(logLine.indexOf("{"))
        def slurper = new JsonSlurper()
        def result = slurper.parseText(jsonpart)

        then:
        mc.receivedResponse.code == "200"
        checkAttribute(jsonpart, listattr)
        result.ServiceCode == "repose"
        result.Region == "USA"
        result.DataCenter == "DFW"
        result.Request.Method == "GET"
        result.Response.Code == 200
        result.Response.Message == "OK"
    }

    // Check all required attributes in the log
    private boolean checkAttribute(String jsonpart, List listattr) {
        boolean check = true
        for (attr in listattr) {
            if (!jsonpart.contains(attr)) {
                check = false
                break
            }
        }
        return check
    }
}
