package features.filters.rackspaceAuthIdentity

import framework.ReposeValveTest
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.Handling
import org.rackspace.deproxy.MessageChain
import org.rackspace.deproxy.Response
import spock.lang.Unroll

class RackspaceAuthIdentity20Test extends ReposeValveTest {

    def static Map contentXml = ["content-type": "application/xml"]
    def static Map contentJSON = ["content-type": "application/json"]

    def static String xmlPasswordCred = """<?xml version="1.0" encoding="UTF-8"?>
<auth xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
 xmlns="http://docs.openstack.org/identity/api/v2.0">
  <passwordCredentials username="demoauthor" password="theUsersPassword" tenantId="1100111"/>
</auth>"""


    def static String xmlPasswordCredEmptyKey = """<?xml version="1.0" encoding="UTF-8"?>
<auth xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
 xmlns="http://docs.openstack.org/identity/api/v2.0">
  <passwordCredentials username="demoauthor" password="" tenantId="1100111"/>
</auth>"""

    def static String invalidData = "Invalid data"
    def
    static String xmlOverLimit = "<auth xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://docs.openstack.org/identity/api/v2.0\"><credential xsi:type=\"PasswordCredentialsRequiredUsername\" username=\"012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789\" password=\"testpwd\" /></auth>"

    def static String jsonPasswordCred = """
{
    "auth":{
        "passwordCredentials":{
            "username":"demoauthor",
            "password":"theUsersPassword"
        },
        "tenantId": "12345678"
    }
}
"""

    def static String jsonApiKeyCred = """{
    "auth": {
        "RAX-KSKEY:apiKeyCredentials": {
            "username": "demoauthor",
            "apiKey": "aaaaa-bbbbb-ccccc-12345678"
        },
        "tenantId": "1100111"
    }
}
"""

    def setupSpec() {
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort)

        def params = properties.defaultTemplateParams
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/filters/rackspaceAuthIdentity", params)
        repose.start()
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


    @Unroll("When request contains identity in content #testName Expected user is #expectedUser")
    def "when identifying requests by header"() {

        when: "Request body contains user credentials"
        def messageChain = deproxy.makeRequest([url: reposeEndpoint, requestBody: requestBody, headers: contentType])
        def sentRequest = ((MessageChain) messageChain).getHandlings()[0]

        then: "Repose will send x-pp-user with a single value"
        ((Handling) sentRequest).request.getHeaders().findAll("x-pp-user").size() == 1

        and: "Repose will send user from Request body"
        ((Handling) sentRequest).request.getHeaders().getFirstValue("x-pp-user").equals(expectedUser + ";q=0.8")

        and: "Repose will send a single value for x-pp-groups"
        ((Handling) sentRequest).request.getHeaders().findAll("x-pp-groups").size() == 1

        and: "Repose will send 'My Group' for x-pp-groups"
        ((Handling) sentRequest).request.getHeaders().getFirstValue("x-pp-groups").equals("2_0 Group;q=0.8")

        where:
        requestBody             | contentType | expectedUser | testName
        xmlPasswordCred         | contentXml  | "demoauthor" | "xmlPasswordCred"
        xmlPasswordCredEmptyKey | contentXml  | "demoauthor" | "xmlPasswordCredEmptyKey"
        jsonPasswordCred        | contentJSON | "demoauthor" | "jsonPasswordKey"
        jsonApiKeyCred          | contentJSON | "demoauthor" | "jsonApiKey"
    }


    @Unroll("When bad requests pass through repose #testName")
    def "when attempting to identity user by content and passed bad content"() {

        when: "Request body contains user credentials"
        def messageChain = deproxy.makeRequest([url: reposeEndpoint, requestBody: requestBody, headers: contentType])
        def sentRequest = ((MessageChain) messageChain).getHandlings()[0]

        then: "Repose will not send x-pp-user"
        ((Handling) sentRequest).request.getHeaders().findAll("x-pp-user").size() == 0

        and: "Repose will not send x-pp-groups"
        ((Handling) sentRequest).request.getHeaders().findAll("x-pp-groups").size() == 0

        where:
        requestBody  | contentType | testName
        invalidData  | contentXml  | "invalidData"
        xmlOverLimit | contentXml  | "xmlOverLimit"
    }

}
