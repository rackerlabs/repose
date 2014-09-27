package features.filters.contentidentity

import framework.ReposeValveTest
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.Handling
import org.rackspace.deproxy.MessageChain
import org.rackspace.deproxy.Response
import spock.lang.Unroll

/**
 * Created by dimi5963 on 9/26/14.
 */
class ContentIdentityV2Test  extends ReposeValveTest {

    def static Map contentJson = ["content-type": "application/json"]
    def static Map contentXml = ["content-type": "application/xml"]

    def static String xmlPasswordCred = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<auth xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
            "xmlns=\"http://docs.openstack.org/identity/api/v2.0\">\n" +
            "<passwordCredentials username=\"demoauthor\" password=\"theUsersPassword\" tenantId=\"1100111\"/>\n" +
            "</auth>"

    def setupSpec() {
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort)

        def params = properties.defaultTemplateParams
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/filters/contentidentityv2", params)
        repose.start([waitOnJmxAfterStarting: false])
        waitUntilReadyToServiceRequests()
    }

    def cleanupSpec() {
        if (deproxy) {
            deproxy.shutdown()
        }

        if (repose) {
            repose.stop()
        }
    }


    def "when identifying requests by header"() {
        when: "Request body contains user credentials"
        def messageChain = deproxy.makeRequest(
                [
                        method: 'POST',
                        url: reposeEndpoint + "/randomness",
                        requestBody: xmlPasswordCred,
                        headers: contentXml
                ]
        )
        def sentRequest = ((MessageChain) messageChain).getHandlings()[0]

        then: "Repose will send x-pp-user with a single value"
        ((Handling) sentRequest).request.getHeaders().findAll("x-pp-user").size() == 2

        and: "Repose will send user from Request body"
        ((Handling) sentRequest).request.getHeaders().findAll("x-pp-user").contains("127.0.0.1;q=0.4")
        ((Handling) sentRequest).request.getHeaders().findAll("x-pp-user").contains("demoauthor;q=0.75")

        and: "Repose will send a single value for x-pp-groups"
        ((Handling) sentRequest).request.getHeaders().findAll("x-pp-groups").size() == 2

        and: "Repose will send 'My Group' for x-pp-groups"
        ((Handling) sentRequest).request.getHeaders().findAll("x-pp-groups").contains("test group;q=0.75")
        ((Handling) sentRequest).request.getHeaders().findAll("x-pp-groups").contains("IP_Standard;q=0.4")
    }


    def "when rate limiting by content identity"() {
        MessageChain messageChain = null
        when: "Request body contains user credentials"
        (0..2).each {
            messageChain = deproxy.makeRequest(
                    [
                            method: 'POST',
                            url: reposeEndpoint + "/my/post",
                            requestBody: xmlPasswordCred,
                            headers: contentXml
                    ]
            )
        }
        then: "Repose will rate limit"
        messageChain.receivedResponse.code == "413"

    }
}
