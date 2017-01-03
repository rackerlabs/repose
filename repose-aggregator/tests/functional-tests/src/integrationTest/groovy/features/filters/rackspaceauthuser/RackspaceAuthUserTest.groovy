/*
 * _=_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_=
 * Repose
 * _-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-
 * Copyright (C) 2010 - 2015 Rackspace US, Inc.
 * _-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * =_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_=_
 */
package features.filters.rackspaceauthuser

import framework.ReposeValveTest
import org.rackspace.deproxy.Deproxy
import spock.lang.Unroll

import static features.filters.rackspaceauthuser.RackspaceAuthPayloads.*

class RackspaceAuthUserTest extends ReposeValveTest {

    def setupSpec() {
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort)

        def params = properties.defaultTemplateParams
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/filters/rackspaceauthuser", params)
        repose.start(waitOnJmxAfterStarting: false)
        waitUntilReadyToServiceRequests()
    }

    @Unroll("When request contains Identity 2.0 in User content #testName Expected user is #expectedUser")
    def "when identifying requests by header"() {

        when: "Request body contains user credentials"
        def messageChain = deproxy.makeRequest(url: reposeEndpoint, requestBody: requestBody, headers: contentType, method: "POST")
        def sentRequest = messageChain.getHandlings()[0]

        then: "Repose will send x-pp-user with a single value"
        sentRequest.request.getHeaders().findAll("x-pp-user").size() == 1

        and: "Repose will send user from Request body"
        sentRequest.request.getHeaders().getFirstValue("x-pp-user") == expectedUser + ";q=0.8"
        sentRequest.request.getHeaders().getFirstValue("x-user-name") == expectedUser

        and: "Repose will send a single value for x-pp-groups"
        sentRequest.request.getHeaders().findAll("x-pp-groups").size() == 1

        and: "Repose will send 'My Group' for x-pp-groups"
        sentRequest.request.getHeaders().getFirstValue("x-pp-groups") == "2_0 Group;q=0.8"

        where:
        requestBody              | contentType | expectedUser | testName
        userPasswordXmlV20       | contentXml  | "demoAuthor" | "userPasswordXmlV20"
        userPasswordJsonV20      | contentJson | "demoAuthor" | "userPasswordJsonV20"
        userApiKeyXmlV20         | contentXml  | "demoAuthor" | "userApiKeyXmlV20"
        userApiKeyJsonV20        | contentJson | "demoAuthor" | "userApiKeyJsonV20"
        userPasswordXmlEmptyV20  | contentXml  | "demoAuthor" | "userPasswordXmlEmptyV20"
        userPasswordJsonEmptyV20 | contentJson | "demoAuthor" | "userPasswordJsonEmptyV20"
        userMfaSetupXmlV20       | contentXml  | "demoAuthor" | "userMfaSetupXmlV20"
        userMfaSetupJsonV20      | contentJson | "demoAuthor" | "userMfaSetupJsonV20"
    }

    @Unroll("When request contains Identity 2.0 in Domain'd content #testName Expected user is #expectedUser")
    def "when identifying requests by header with domain"() {

        when: "Request body contains user credentials"
        def messageChain = deproxy.makeRequest(url: reposeEndpoint, requestBody: requestBody, headers: contentType, method: "POST")
        def sentRequest = messageChain.getHandlings()[0]

        then: "Repose will send x-pp-user with a single value"
        sentRequest.request.getHeaders().findAll("x-pp-user").size() == 1

        and: "Repose will send user from Request body"
        sentRequest.request.getHeaders().findAll("x-pp-user").contains(expectedUser + ";q=0.8")
        sentRequest.request.getHeaders().findAll("x-user-name").contains(expectedUser)

        and: "Repose will send two values for x-domain"
        sentRequest.request.getHeaders().findAll("x-domain").size() == 1

        and: "Repose will send #expectedDomain x-domain"
        sentRequest.request.getHeaders().findAll("x-domain").contains(expectedDomain)

        and: "Repose will send a single value for x-pp-groups"
        sentRequest.request.getHeaders().findAll("x-pp-groups").size() == 1

        and: "Repose will send 'My Group' for x-pp-groups"
        sentRequest.request.getHeaders().getFirstValue("x-pp-groups") == "2_0 Group;q=0.8"

        where:
        requestBody              | contentType | expectedDomain | expectedUser     | testName
        rackerPasswordXmlV20     | contentXml  | "Rackspace"    | "Racker:jqsmith" | "rackerPasswordXmlV20"
        rackerPasswordJsonV20    | contentJson | "Rackspace"    | "Racker:jqsmith" | "rackerPasswordJsonV20"
        rackerTokenKeyXmlV20     | contentXml  | "Rackspace"    | "Racker:jqsmith" | "rackerTokenKeyXmlV20"
        rackerTokenKeyJsonV20    | contentJson | "Rackspace"    | "Racker:jqsmith" | "rackerTokenKeyJsonV20"
        federatedPasswordXmlV20  | contentXml  | "Federated"    | "jqsmith"        | "federatedTokenKeyXmlV20"
        federatedPasswordJsonV20 | contentJson | "Federated"    | "jqsmith"        | "federatedTokenKeyJsonV20"
    }

    @Unroll("When bad requests pass through repose #testName")
    def "when attempting to identity user by content and passed bad content"() {

        when: "Request body contains user credentials"
        def messageChain = deproxy.makeRequest(url: reposeEndpoint, requestBody: requestBody, headers: contentType, method: "POST")
        def sentRequest = messageChain.getHandlings()[0]

        then: "Repose will not send x-pp-user"
        sentRequest.request.getHeaders().findAll("x-pp-user").isEmpty()
        sentRequest.request.getHeaders().findAll("x-user-name").isEmpty()

        and: "Repose will not send x-pp-groups"
        sentRequest.request.getHeaders().findAll("x-pp-groups").isEmpty()

        where:
        requestBody            | contentType | testName
        invalidData            | contentXml  | "invalidData XML"
        invalidData            | contentJson | "invalidData JSON"
        userPasswordXmlOverV20 | contentXml  | "userPasswordXmlOverV20"
        // TODO: We evidently don't care about the length in JSON
        //userPasswordJsonOverV20 | contentJson | "userPasswordJsonOverV20"
    }

    // Joe Savak - we don't need to do other v1.1 internal contracts
    @Unroll("When request contains identity 1.1 in content #testName Expected user is #expectedUser")
    def "when using identity1.1 identifying requests by header"() {

        when: "Request body contains user credentials"
        def messageChain = deproxy.makeRequest(url: reposeEndpoint, requestBody: requestBody, headers: contentType, method: "POST")
        def sentRequest = messageChain.getHandlings()[0]

        then: "Repose will send x-pp-user with a single value"
        sentRequest.request.getHeaders().findAll("x-pp-user").size() == 1

        and: "Repose will send user from Request body"
        sentRequest.request.getHeaders().getFirstValue("x-pp-user") == expectedUser + ";q=0.75"
        sentRequest.request.getHeaders().getFirstValue("x-user-name") == expectedUser

        and: "Repose will send a single value for x-pp-groups"
        sentRequest.request.getHeaders().findAll("x-pp-groups").size() == 1

        and: "Repose will send 'My Group' for x-pp-groups"
        sentRequest.request.getHeaders().getFirstValue("x-pp-groups") == "1_1 Group;q=0.75"

        where:
        requestBody         | contentType | expectedUser | testName
        userKeyXmlV11       | contentXml  | "test-user"  | "userKeyXmlV11"
        userKeyJsonV11      | contentJson | "test-user"  | "userKeyJsonV11"
        userKeyXmlEmptyV11  | contentXml  | "test-user"  | "userKeyXmlEmptyV11"
        userKeyJsonEmptyV11 | contentJson | "test-user"  | "userKeyJsonEmptyV11"
    }

    @Unroll("Does not affect #method requests")
    def "Does not affect non-post requests"() {
        when: "Request is a #method"
        def messageChain = deproxy.makeRequest(url: reposeEndpoint, method: method)
        def sentRequest = messageChain.getHandlings()[0]

        then: "Repose will not add any headers"
        sentRequest.request.getHeaders().findAll("x-pp-user").isEmpty()
        sentRequest.request.getHeaders().findAll("x-user-name").isEmpty()
        sentRequest.request.getHeaders().findAll("x-pp-groups").isEmpty()

        and: "The result will be passed through"
        messageChain.receivedResponse.code == "200"

        where:
        method << ["GET", "DELETE", "PUT"]
    }

    def "Does not affect random post requests"() {
        when:
        def messageChain = deproxy.makeRequest(url: reposeEndpoint, method: 'POST')
        def sentRequest = messageChain.getHandlings()[0]

        then: "Repose will not add any headers"
        sentRequest.request.getHeaders().findAll("x-pp-user").isEmpty()
        sentRequest.request.getHeaders().findAll("x-user-name").isEmpty()
        sentRequest.request.getHeaders().findAll("x-pp-groups").isEmpty()

        and: "The result will be passed through"
        messageChain.receivedResponse.code == "200"
    }

    @Unroll("Will parse Forgot Password request for username with Content-Type #contentType and URL #url")
    def "Will parse a Forgot Password request if the URL is /v2.0/users/RAX-AUTH/forgot-pwd with optional trailing slash"() {
        given: "The correct request body is sent based on the content-type the test is going to use"
        def requestBody = contentType == contentXml ? userForgotXmlV20 : userForgotJsonV20

        when: "Request body contains forgot password credentials"
        def messageChain = deproxy.makeRequest(url: reposeEndpoint + url, requestBody: requestBody, headers: contentType, method: "POST")
        def sentRequest = messageChain.getHandlings()[0]

        then: "Repose will send x-pp-user with a single value"
        sentRequest.request.getHeaders().findAll("x-pp-user").size() == 1
        sentRequest.request.getHeaders().findAll("x-user-name").size() == 1
        sentRequest.request.getHeaders().findAll("x-pp-groups").size() == 1
        sentRequest.request.getHeaders().findAll("x-domain").isEmpty()

        and: "Repose will send user from Request body"
        sentRequest.request.getHeaders().getFirstValue("x-pp-user") == "demoAuthor;q=0.8"
        sentRequest.request.getHeaders().getFirstValue("x-user-name") == "demoAuthor"

        where:
        [contentType, url] << [
                [contentXml, contentJson],
                ["/v2.0/users/RAX-AUTH/forgot-pwd", "/v2.0/users/RAX-AUTH/forgot-pwd/", "/v2.0/users/RAX-AUTH/forgot-pwd?sauce=ketchup"]
        ].combinations()
    }

    @Unroll("Will not parse Forgot Password request for username when sent to an inappropriate URL (#testName)")
    def "will not parse a Forgot Password request if the URL does not match /v2.0/users/RAX-AUTH/forgot-pwd"() {
        when: "Request body contains forgot password credentials"
        def messageChain = deproxy.makeRequest(url: reposeEndpoint, requestBody: requestBody, headers: contentType, method: "POST")
        def sentRequest = messageChain.getHandlings()[0]

        then: "Repose will not send x-pp-user nor the other related headers"
        sentRequest.request.getHeaders().findAll("x-pp-user").isEmpty()
        sentRequest.request.getHeaders().findAll("x-user-name").isEmpty()
        sentRequest.request.getHeaders().findAll("x-pp-groups").isEmpty()
        sentRequest.request.getHeaders().findAll("x-domain").isEmpty()

        where:
        requestBody       | contentType | testName
        userForgotXmlV20  | contentXml  | "userForgotXmlV20"
        userForgotJsonV20 | contentJson | "userForgotJsonV20"
    }

    @Unroll("Will not parse an Auth request if the URL matches /v2.0/users/RAX-AUTH/forgot-pwd (#testName)" )
    def "will not parse an Auth request if the URL matches /v2.0/users/RAX-AUTH/forgot-pwd" () {
        when: "Request body contains user credentials"
        def messageChain = deproxy.makeRequest(url: reposeEndpoint + "/v2.0/users/RAX-AUTH/forgot-pwd", requestBody: requestBody, headers: contentType, method: "POST")
        def sentRequest = messageChain.getHandlings()[0]

        then: "Repose will not send x-pp-user nor the other related headers"
        sentRequest.request.getHeaders().findAll("x-pp-user").isEmpty()
        sentRequest.request.getHeaders().findAll("x-user-name").isEmpty()
        sentRequest.request.getHeaders().findAll("x-pp-groups").isEmpty()
        sentRequest.request.getHeaders().findAll("x-domain").isEmpty()

        where:
        requestBody              | contentType | testName
        userKeyXmlV11            | contentXml  | "userKeyXmlV11"
        userKeyJsonV11           | contentJson | "userKeyJsonV11"
        userKeyXmlEmptyV11       | contentXml  | "userKeyXmlEmptyV11"
        userKeyJsonEmptyV11      | contentJson | "userKeyJsonEmptyV11"
        userPasswordXmlV20       | contentXml  | "userPasswordXmlV20"
        userPasswordJsonV20      | contentJson | "userPasswordJsonV20"
        userApiKeyXmlV20         | contentXml  | "userApiKeyXmlV20"
        userApiKeyJsonV20        | contentJson | "userApiKeyJsonV20"
        userPasswordXmlEmptyV20  | contentXml  | "userPasswordXmlEmptyV20"
        userPasswordJsonEmptyV20 | contentJson | "userPasswordJsonEmptyV20"
        userMfaSetupXmlV20       | contentXml  | "userMfaSetupXmlV20"
        userMfaSetupJsonV20      | contentJson | "userMfaSetupJsonV20"
    }
}
