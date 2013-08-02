package features.filters.contentidentity

import framework.ReposeValveTest
import org.rackspace.gdeproxy.Deproxy
import org.rackspace.gdeproxy.Handling
import org.rackspace.gdeproxy.MessageChain
import spock.lang.Unroll

class ContentIdentityTest extends ReposeValveTest {

    def static Map contentJson = ["content-type": "application/json"]
    def static Map contentXml = ["content-type": "application/xml"]

    def static String jsonPasswordCred = "{ \"passwordCredentials\": { \"username\": \"test-user\", \"password\": \"testpwd\"}}"
    def static String jsonKeyCred = "{ \"credentials\": { \"username\": \"test-user\", \"key\": \"testpwd\"}}"
    def static String jsonKeyCredEmptyKey = "{ \"credentials\": { \"username\": \"test-user\", \"key\": \"\"}}"
    def static String jsonMossoIdCred = "{ \"mossoCredentials\": { \"mossoId\": 12345, \"key\": \"testpwd\"}}"
    def static String jsonMossoNastCred = "{ \"mossoCredentials\": { \"mossoId\": 12345, \"key\": \"testpwd\"}}"
    def static String jsonMossoNastCredEmptyKey = "{ \"nastCredentials\": { \"nastId\": \"12345\", \"key\": \"\"}}"
    def static String xmlPasswordCred = "<passwordCredentials xmlns=\"http://docs.rackspacecloud.com/auth/api/v1.1\" username=\"test-user\" password=\"testpwd\" />"
    def static String xmlPasswordCredEmptyKey = "<passwordCredentials xmlns=\"http://docs.rackspacecloud.com/auth/api/v1.1\" username=\"test-user\" password=\"\" />"
    def static String xmlKeyCred = "<credentials xmlns=\"http://docs.rackspacecloud.com/auth/api/v1.1\" username=\"test-user\" key=\"testpwd\" />"
    def static String xmlKeyCredEmptyKey = "<credentials xmlns=\"http://docs.rackspacecloud.com/auth/api/v1.1\" username=\"test-user\" key=\"\" />"
    def static String xmlMossoIdCred = "<mossoCredentials xmlns=\"http://docs.rackspacecloud.com/auth/api/v1.1\" mossoId=\"12345\" key=\"testpwd\" />"
    def static String xmlMossoNastCred = "<mossoCredentials xmlns=\"http://docs.rackspacecloud.com/auth/api/v1.1\" mossoId=\"12345\" key=\"testpwd\" />"

    def static String jsonPasswordCredEmptyUser = "{ \"passwordCredentials\": { \"username\": \"\", \"password\": \"testpwd\"}}"
    def static String jsonPasswordCredEmptyUserField = "{ \"passwordCredentials\": { \"password\": \"testpwd\"}}"
    def static String jsonPasswordCredEmptyPasswordField = "{ \"passwordCredentials\": { \"username\": \"test-user\" }}"
    def static String jsonKeyCredEmptyUser = "{ \"credentials\": { \"username\": \"\", \"key\": \"testpwd\"}}"
    def static String jsonKeyCredEmptyUserField = "{ \"credentials\": { \"key\": \"testpwd\"}}"
    def static String jsonKeyCredNoKey = "{ \"credentials\": { \"username\": \"test-user\" }}"
    def static String jsonMossoCredNoId = "{ \"mossoCredentials\": { \"key\": \"testpwd\"}}"
    def static String jsonMossoCredNoKey = "{ \"mossoCredentials\": { \"mossoId\": 12345 }}"
    def static String jsonMossoNastEmptyNast = "{ \"nastCredentials\": { \"nastId\": \"\", \"key\": \"testpwd\"}}"
    def static String jsonMossoNastNoNastId = "{ \"nastCredentials\": { \"key\": \"testpwd\"}}"
    def static String jsonMossoNastNoKey = "{ \"nastCredentials\": { \"nastId\": \"12345\" }}"
    def static String xmlPasswordCredEmptyUser = "<passwordCredentials xmlns=\"http://docs.rackspacecloud.com/auth/api/v1.1\" username=\"\" password=\"testpwd\" />"
    def static String xmlPasswordCredEmptyUserField = "<passwordCredentials xmlns=\"http://docs.rackspacecloud.com/auth/api/v1.1\" password=\"testpwd\" />"
    def static String xmlKeyCredEmptyUser = "<credentials xmlns=\"http://docs.rackspacecloud.com/auth/api/v1.1\" username=\"\" key=\"testpwd\" />"
    def static String xmlKeyCredEmptyUserField = "<credentials xmlns=\"http://docs.rackspacecloud.com/auth/api/v1.1\" key=\"testpwd\" />"
    def static String xmlMossoCredInvalidId = "<mossoCredentials xmlns=\"http://docs.rackspacecloud.com/auth/api/v1.1\" mossoId=\"a\" key=\"testpwd\" />"
    def static String xmlMossoNastCredEmptyNastId = "<nastCredentials xmlns=\"http://docs.rackspacecloud.com/auth/api/v1.1\" nastId=\"\" key=\"testpwd\" />"
    def static String xmlMossoNastCredNoEmptyNastField = "<nastCredentials xmlns=\"http://docs.rackspacecloud.com/auth/api/v1.1\" key=\"testpwd\" />"

    def static String invalidData = "Invalid data"

    def static String jsonOverLimit = "{ \"passwordCredentials\": { \"username\": \"012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789\", \"password\": \"testpwd\"}}"
    def static String xmlOverLimit = "<passwordCredentials xmlns=\"http://docs.rackspacecloud.com/auth/api/v1.1\" username=\"012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789\" password=\"testpwd\" />"

    def setupSpec() {
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.getProperty("target.port").toInteger())

        repose.applyConfigs("features/filters/contentidentity")
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

    @Unroll("When request contains identity in content #requestBody\n Expected user is #expectedUser")
    def "when identifying requests by header"() {

        when: "Request body contains user credentials"
        def messageChain = deproxy.makeRequest([url: reposeEndpoint, requestBody: requestBody, headers: contentType])
        def sentRequest = ((MessageChain) messageChain).getHandlings()[0]

        then: "Repose will send x-pp-user with a single value"
        ((Handling) sentRequest).request.getHeaders().findAll("x-pp-user").size() == 1

        and: "Repose will send user from Request body"
        ((Handling) sentRequest).request.getHeaders().getFirstValue("x-pp-user").equals(expectedUser + ";q=0.75")

        where:
        requestBody               | contentType | expectedUser
        jsonPasswordCred          | contentJson | "test-user"
        jsonKeyCred               | contentJson | "test-user"
        jsonKeyCredEmptyKey       | contentJson | "test-user"
        jsonMossoIdCred           | contentJson | "12345"
        jsonMossoNastCred         | contentJson | "12345"
        jsonMossoNastCredEmptyKey | contentJson | "12345"
        xmlPasswordCred           | contentXml  | "test-user"
        xmlPasswordCredEmptyKey   | contentXml  | "test-user"
        xmlKeyCred                | contentXml  | "test-user"
        xmlKeyCredEmptyKey        | contentXml  | "test-user"
        xmlMossoIdCred            | contentXml  | "12345"
        xmlMossoNastCred          | contentXml  | "12345"
    }

    @Unroll("When bad requests pass through repose #requestBody\n")
    def "when attempting to identity user by content and passed bad content"() {

        when: "Request body contains user credentials"
        def messageChain = deproxy.makeRequest([url: reposeEndpoint, requestBody: requestBody, headers: contentType])
        def sentRequest = ((MessageChain) messageChain).getHandlings()[0]

        then: "Repose will not send x-pp-user"
        ((Handling) sentRequest).request.getHeaders().findAll("x-pp-user").size() == 0


        where:
        requestBody                        | contentType
        jsonPasswordCredEmptyUser          | contentJson
        jsonPasswordCredEmptyUserField     | contentJson
        jsonPasswordCredEmptyPasswordField | contentJson
        jsonKeyCredEmptyUser               | contentJson
        jsonKeyCredEmptyUserField          | contentJson
        jsonKeyCredNoKey                   | contentJson
        jsonMossoCredNoId                  | contentJson
        jsonMossoCredNoKey                 | contentJson
        jsonMossoNastEmptyNast             | contentJson
        jsonMossoNastNoNastId              | contentJson
        jsonMossoNastNoKey                 | contentJson

        xmlPasswordCredEmptyUser           | contentXml
        xmlPasswordCredEmptyUserField      | contentXml
        xmlKeyCredEmptyUser                | contentXml
        xmlKeyCredEmptyUserField           | contentXml
        xmlMossoCredInvalidId              | contentXml
        xmlMossoNastCredEmptyNastId        | contentXml
        xmlMossoNastCredNoEmptyNastField   | contentXml

        invalidData                        | contentJson
        invalidData                        | contentXml
        jsonOverLimit                      | contentJson
        xmlOverLimit                       | contentXml


    }
}
