package features.filters.rackspaceauthuser

import framework.ReposeValveTest
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.Handling
import org.rackspace.deproxy.MessageChain

import static features.filters.rackspaceauthuser.RackspaceAuthPayloads.*

class RackspaceAuthUserRateLimitTest extends ReposeValveTest {

    def setupSpec() {
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort)

        def params = properties.defaultTemplateParams
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/filters/rackspaceauthuser/rate-limiting", params)
        //For this test, we need to wait for JMX to tell us it's ready to go, and then we can do stuff
        repose.start([waitOnJmxAfterStarting: true])
    }

    def cleanupSpec() {
        if (deproxy) {
            deproxy.shutdown()
        }

        if (repose) {
            repose.stop()
        }
    }


    def "Rate limits specific post requests"() {
        when: "first request"
        def messageChain = deproxy.makeRequest([
                url: reposeEndpoint + "/my-post", requestBody: requestBody, headers: contentType, method: "POST"
        ])
        def sentRequest = ((MessageChain) messageChain).getHandlings()[0]

        then: "Repose will send x-pp-user with two values"
        ((Handling) sentRequest).request.getHeaders().findAll("x-pp-user").size() == 1

        and: "Repose will send user from Request body"
        ((Handling) sentRequest).request.getHeaders().findAll("x-pp-user").contains(expectedUser + ";q=0.8")

        and: "Repose will send two values for x-pp-groups"
        ((Handling) sentRequest).request.getHeaders().findAll("x-pp-groups").size() == 1

        and: "Repose will send 'My Group' for x-pp-groups"
        ((Handling) sentRequest).request.getHeaders().findAll("x-pp-groups").contains("2_0-Group;q=0.8")

        and: "The result will be passed through"
        messageChain.receivedResponse.code == "200"

        when: "second request"
        messageChain = deproxy.makeRequest([
                url: reposeEndpoint + "/my-post", requestBody: requestBody, headers: contentType, method: "POST"
        ])
        sentRequest = ((MessageChain) messageChain).getHandlings()[0]

        then: "The result will be rate limited"
        messageChain.receivedResponse.code == "413"

        where:
        requestBody            | contentType | expectedUser | testName
        xmlPasswordCred        | contentXml  | "demoauthor" | "xmlPasswordCred"
        jsonPasswordCredAuthr2 | contentJSON | "demoauthr2" | "jsonPasswordKey"
    }
}
