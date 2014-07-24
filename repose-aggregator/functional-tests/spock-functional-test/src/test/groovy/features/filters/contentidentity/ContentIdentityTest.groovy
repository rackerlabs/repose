package features.filters.contentidentity

import framework.ReposeValveTest
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.Handling
import org.rackspace.deproxy.MessageChain
import org.rackspace.deproxy.Response
import spock.lang.Unroll

class ContentIdentityTest extends ReposeValveTest {

    // NOTE: JSON is not supported by the content identity 2.0 filter

    def static String xmlPasswordCred = "<auth xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://docs.openstack.org/identity/api/v2.0\"><credential xsi:type=\"PasswordCredentialsRequiredUsername\" username=\"test-user\" password=\"testpwd\" /></auth>"

    def static Map contentXml = ["content-type": "application/xml"]
    def static String xmlPasswordCredEmptyKey = "<auth xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://docs.openstack.org/identity/api/v2.0\"><credential xsi:type=\"PasswordCredentialsRequiredUsername\" username=\"test-user\" password=\"\" /></auth>"

    def static String invalidData = "Invalid data"

    def static String xmlOverLimit = "<auth xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://docs.openstack.org/identity/api/v2.0\"><credential xsi:type=\"PasswordCredentialsRequiredUsername\" username=\"012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789\" password=\"testpwd\" /></auth>"

    def setupSpec() {
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort)

        def params = properties.defaultTemplateParams
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/filters/contentidentity", params)
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


    @Unroll("When request contains identity in content #testName\n Expected user is #expectedUser")
    def "when identifying requests by header"() {

        when: "Request body contains user credentials"
        def messageChain = deproxy.makeRequest([url: reposeEndpoint, requestBody: requestBody, headers: contentType])
        def sentRequest = ((MessageChain) messageChain).getHandlings()[0]

        then: "Repose will send x-pp-user with a single value"
        ((Handling) sentRequest).request.getHeaders().findAll("x-pp-user").size() == 1

        and: "Repose will send user from Request body"
        ((Handling) sentRequest).request.getHeaders().getFirstValue("x-pp-user").equals(expectedUser + ";q=0.75")

        and: "Repose will send a single value for x-pp-groups"
        ((Handling) sentRequest).request.getHeaders().findAll("x-pp-groups").size() == 1

        and: "Repose will send 'My Group' for x-pp-groups"
        ((Handling) sentRequest).request.getHeaders().getFirstValue("x-pp-groups").equals("My Group;q=0.75")

        where:
        requestBody               | contentType | expectedUser  | testName
        xmlPasswordCred           | contentXml  | "test-user"   | "xmlPasswordCred"
        xmlPasswordCredEmptyKey   | contentXml  | "test-user"   | "xmlPasswordCredEmptyKey"
    }


    @Unroll("When bad requests pass through repose #testName\n")
    def "when attempting to identity user by content and passed bad content"() {

        when: "Request body contains user credentials"
        def messageChain = deproxy.makeRequest([url: reposeEndpoint, requestBody: requestBody, headers: contentType])
        def sentRequest = ((MessageChain) messageChain).getHandlings()[0]

        then: "Repose will not send x-pp-user"
        ((Handling) sentRequest).request.getHeaders().findAll("x-pp-user").size() == 0

        and: "Repose will not send x-pp-groups"
        ((Handling) sentRequest).request.getHeaders().findAll("x-pp-groups").size() == 0

        where:
        requestBody                        | contentType | testName
        invalidData                        | contentXml  | "invalidData"
        xmlOverLimit                       | contentXml  | "xmlOverLimit"
    }

    def "Should not split request headers according to rfc"() {
        given:
        def userAgentValue = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_8_4) " +
                "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/29.0.1547.65 Safari/537.36"
        def reqHeaders =
            [
                    "user-agent": userAgentValue,
                    "x-pp-user": "usertest1, usertest2, usertest3",
                    "accept": "application/xml;q=1 , application/json;q=0.5"
            ]

        when: "User sends a request through repose"
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint, method: 'GET', headers: reqHeaders)

        then:
        mc.handlings.size() == 1
        mc.handlings[0].request.getHeaders().findAll("user-agent").size() == 1
        mc.handlings[0].request.headers['user-agent'] == userAgentValue
        mc.handlings[0].request.getHeaders().findAll("x-pp-user").size() == 3
        mc.handlings[0].request.getHeaders().findAll("accept").size() == 2
    }

    def "Should not split response headers according to rfc"() {
        given: "Origin service returns headers "
        def respHeaders = ["location": "http://somehost.com/blah?a=b,c,d", "via": "application/xml;q=0.3, application/json;q=1"]
        def handler = { request -> return new Response(201, "Created", respHeaders, "") }

        when: "User sends a request through repose"
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint, method: 'GET', defaultHandler: handler)
        def handling = mc.getHandlings()[0]

        then:
        mc.receivedResponse.code == "201"
        mc.handlings.size() == 1
        mc.receivedResponse.headers.findAll("location").size() == 1
        mc.receivedResponse.headers['location'] == "http://somehost.com/blah?a=b,c,d"
        mc.receivedResponse.headers.findAll("via").size() == 1
    }

    @Unroll("With headers: #xppuser[#xppuservalue],#accept[#acceptvalue],#roles[#rolevalue]")
    def "Should not toLowerCase headers"() {
        given:
        def userAgentValue = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_8_4) " +
                "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/29.0.1547.65 Safari/537.36"
        def reqHeaders =
                [
                        "user-agent": userAgentValue,
                ]
        reqHeaders[xppuser.toString()] = xppuservalue.toString()
        reqHeaders[accept.toString()] = acceptvalue.toString()
        reqHeaders[roles.toString()] = rolevalue.toString()

        when: "When Requesting resource with headers:#xppuser[#xppuservalue],#accept[#acceptvalue],#roles[#rolevalue]"
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint, method: 'GET', headers: reqHeaders)
        def handling = mc.getHandlings()[0]

        then:
        handling.request.getHeaders().findAll("user-agent").size() == 1
        handling.request.headers['user-agent'] == userAgentValue
        handling.request.getHeaders().findAll(xppuser).size() == xppuservalue.split(',').size()
        handling.request.getHeaders().findAll(accept).size() == acceptvalue.split(',').size()
        handling.request.headers.contains(xppuser)
        handling.request.headers.findAll(xppuser) == xppuservalue.split(',')
        handling.request.headers.contains(accept)
        handling.request.headers.findAll(accept) == acceptvalue.split(',')
        handling.request.headers.contains(roles)
        handling.request.headers.findAll(roles) == rolevalue.split(',')

        where:
        xppuser     |xppuservalue           |accept     |acceptvalue                        |roles      |rolevalue
        "x-pp-user" |"usertest1,usertest2"  |"accept"   |"application/xml,application/json" |"x-roles"  |"group1"
        "X-pp-user" |"User1,user2"          |"Accept"   |"Application/xml,application/JSON" |"X-roles"  |"group1,Group2"
        "X-PP-User" |"USER1,user2,User2"    |"ACCEPT"   |"APPLICATION/XML"                  |"X-Roles"  |"group1,role1"
        "X-PP-USER" |"USERTEST"             |"accEPT"   |"application/XML,text/plain"       |"X-ROLES"  |"ROLE1,group1,ROLE30"
    }

    @Unroll("Requests - headers: #headerName with \"#headerValue\" keep its case")
    def "Requests - headers should keep its case in requests"() {

        when: "make a request with the given header and value"
        def headers = [
                'Content-Length': '0'
        ]
        headers[headerName.toString()] = headerValue.toString()

        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint, headers: headers)

        then: "the request should keep headerName and headerValue case"
        mc.handlings.size() == 1
        mc.handlings[0].request.headers.contains(headerName)
        mc.handlings[0].request.headers.getFirstValue(headerName) == headerValue


        where:
        headerName | headerValue
        "Accept"           | "text/plain"
        "ACCEPT"           | "text/PLAIN"
        "accept"           | "TEXT/plain;q=0.2"
        "aCCept"           | "text/plain"
        "CONTENT-Encoding" | "identity"
        "Content-ENCODING" | "identity"
        //"content-encoding" | "idENtItY"
        //"Content-Encoding" | "IDENTITY"
    }

    @Unroll("Responses - headers: #headerName with \"#headerValue\" keep its case")
    def "Responses - header keep its case in responses"() {

        when: "make a request with the given header and value"
        def headers = [
                'Content-Length': '0'
        ]
        headers[headerName.toString()] = headerValue.toString()

        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint, defaultHandler: { new Response(200, null, headers) })

        then: "the response should keep headerName and headerValue case"
        mc.handlings.size() == 1
        mc.receivedResponse.headers.contains(headerName)
        mc.receivedResponse.headers.getFirstValue(headerName) == headerValue


        where:
        headerName | headerValue
        "x-auth-token" | "123445"
        "X-AUTH-TOKEN" | "239853"
        "x-AUTH-token" | "slDSFslk&D"
        "x-auth-TOKEN" | "sl4hsdlg"
        "CONTENT-Type" | "application/json"
        "Content-TYPE" | "application/json"
        //"content-type" | "application/xMl"
        //"Content-Type" | "APPLICATION/xml"
    }
}
