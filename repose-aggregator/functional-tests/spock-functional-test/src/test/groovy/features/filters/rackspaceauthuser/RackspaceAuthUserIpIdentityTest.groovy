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
import org.rackspace.deproxy.Handling
import org.rackspace.deproxy.MessageChain
import spock.lang.Unroll

import static features.filters.rackspaceauthuser.RackspaceAuthPayloads.*

class RackspaceAuthUserIpIdentityTest extends ReposeValveTest {

    def setupSpec() {
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort)

        def params = properties.defaultTemplateParams
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/filters/rackspaceauthuser/ip-identity", params)
        repose.start([waitOnJmxAfterStarting: false])
        waitUntilReadyToServiceRequests()
    }

    @Unroll("When request contains Identity 2.0 in User content preceded by ip-identity #testName Expected user is #expectedUser")
    def "Verifying that x-pp-user and x-pp-groups are added for a User auth2.0 payload, not replacing existing headers"() {

        when: "Request body contains user credentials"
        def messageChain = deproxy.makeRequest([url: reposeEndpoint, requestBody: requestBody, headers: contentType, method: "POST"])
        def sentRequest = ((MessageChain) messageChain).getHandlings()[0]

        then: "Repose will send x-pp-user with two values"
        ((Handling) sentRequest).request.getHeaders().findAll("x-pp-user").size() == 2

        and: "Repose will send user from Request body"
        ((Handling) sentRequest).request.getHeaders().findAll("x-pp-user").contains(expectedUser + ";q=0.8")

        and: "Repose will send two values for x-pp-groups"
        ((Handling) sentRequest).request.getHeaders().findAll("x-pp-groups").size() == 2

        and: "Repose will send 'My Group' for x-pp-groups"
        ((Handling) sentRequest).request.getHeaders().findAll("x-pp-groups").contains("2_0-Group;q=0.8")

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

    @Unroll("When request contains Identity 2.0 in Domain'd content preceded by ip-identity #testName Expected user is #expectedUser")
    def "Verifying that x-pp-user and x-pp-groups are added for a domain'd auth2.0 payload, not replacing existing headers"() {

        when: "Request body contains user credentials"
        def messageChain = deproxy.makeRequest([url: reposeEndpoint, requestBody: requestBody, headers: contentType, method: "POST"])
        def sentRequest = ((MessageChain) messageChain).getHandlings()[0]

        then: "Repose will send x-pp-user with two values"
        ((Handling) sentRequest).request.getHeaders().findAll("x-pp-user").size() == 2

        and: "Repose will send user from Request body"
        ((Handling) sentRequest).request.getHeaders().findAll("x-pp-user").contains(expectedUser + ";q=0.8")

        and: "Repose will send two values for x-domain"
        ((Handling) sentRequest).request.getHeaders().findAll("x-domain").size() == 1

        and: "Repose will send #expectedDomain x-domain"
        ((Handling) sentRequest).request.getHeaders().findAll("x-domain").contains(expectedDomain)

        and: "Repose will send two values for x-pp-groups"
        ((Handling) sentRequest).request.getHeaders().findAll("x-pp-groups").size() == 2

        and: "Repose will send 'My Group' for x-pp-groups"
        ((Handling) sentRequest).request.getHeaders().findAll("x-pp-groups").contains("2_0-Group;q=0.8")

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
    def "Verifying that x-pp-user and x-pp-groups are untouched if the filter does nothing"() {

        when: "Request body contains user credentials"
        def messageChain = deproxy.makeRequest([url: reposeEndpoint, requestBody: requestBody, headers: contentType, method: "POST"])
        def sentRequest = ((MessageChain) messageChain).getHandlings()[0]

        then: "Repose will send x-pp-user with a single value"
        ((Handling) sentRequest).request.getHeaders().findAll("x-pp-user").size() == 1

        and: "Repose will send user from Request body"
        ((Handling) sentRequest).request.getHeaders().findAll("x-pp-user").contains("127.0.0.1;q=0.4")

        and: "Repose will send a single value for x-pp-groups"
        ((Handling) sentRequest).request.getHeaders().findAll("x-pp-groups").size() == 1

        and: "Repose will send 'My Group' for x-pp-groups"
        ((Handling) sentRequest).request.getHeaders().findAll("x-pp-groups").contains("IP_Standard;q=0.4")

        where:
        requestBody            | contentType | testName
        invalidData            | contentXml  | "invalidData XML"
        invalidData            | contentJson | "invalidData JSON"
        userPasswordXmlOverV20 | contentXml  | "userPasswordXmlOverV20"
        // TODO: We evidently don't care about the length in JSON
        //userPasswordJsonOverV20 | contentJson | "userPasswordJsonOverV20"
    }

    // Joe Savak - we don't need to do other v1.1 internal contracts
    @Unroll("When request contains identity 1.1 in content, with preceding ip-identity, #testName Expected user is #expectedUser")
    def "Verifying that x-pp-user and x-pp-groups are added for an auth1.1 payload, not replacing existing headers"() {

        when: "Request body contains user credentials"
        def messageChain = deproxy.makeRequest([url: reposeEndpoint, requestBody: requestBody, headers: contentType, method: "POST"])
        def sentRequest = ((MessageChain) messageChain).getHandlings()[0]

        then: "Repose will send x-pp-user with two values"
        ((Handling) sentRequest).request.getHeaders().findAll("x-pp-user").size() == 2

        and: "Repose will send user from Request body"
        ((Handling) sentRequest).request.getHeaders().findAll("x-pp-user").contains(expectedUser + ";q=0.75")

        and: "Repose will send two values for x-pp-groups"
        ((Handling) sentRequest).request.getHeaders().findAll("x-pp-groups").size() == 2

        and: "Repose will send 'My Group' for x-pp-groups"
        ((Handling) sentRequest).request.getHeaders().findAll("x-pp-groups").contains("1_1-Group;q=0.75")

        where:
        requestBody         | contentType | expectedUser | testName
        userKeyXmlV11       | contentXml  | "test-user"  | "userKeyXmlV11"
        userKeyJsonV11      | contentJson | "test-user"  | "userKeyJsonV11"
        userKeyXmlEmptyV11  | contentXml  | "test-user"  | "userKeyXmlEmptyV11"
        userKeyJsonEmptyV11 | contentJson | "test-user"  | "userKeyJsonEmptyV11"
    }

    @Unroll("Does not affect #method requests")
    def "Does not affect non-post requests, when preceded by an ip-identity filter"() {
        when: "Request is a #method"
        def messageChain = deproxy.makeRequest([url: reposeEndpoint, method: method])
        def sentRequest = ((MessageChain) messageChain).getHandlings()[0]

        then: "Repose will send x-pp-user with a single value"
        ((Handling) sentRequest).request.getHeaders().findAll("x-pp-user").size() == 1

        and: "Repose will send user from Request body"
        ((Handling) sentRequest).request.getHeaders().findAll("x-pp-user").contains("127.0.0.1;q=0.4")

        and: "Repose will send a single value for x-pp-groups"
        ((Handling) sentRequest).request.getHeaders().findAll("x-pp-groups").size() == 1

        and: "Repose will send 'My Group' for x-pp-groups"
        ((Handling) sentRequest).request.getHeaders().findAll("x-pp-groups").contains("IP_Standard;q=0.4")

        and: "The result will be passed through"
        messageChain.receivedResponse.code == "200"

        where:
        method   | _
        "GET"    | _
        "DELETE" | _
        "PUT"    | _
    }

    def "Does not affect random post requests"() {
        when:
        def messageChain = deproxy.makeRequest([url: reposeEndpoint, method: 'POST'])
        def sentRequest = ((MessageChain) messageChain).getHandlings()[0]

        then: "Repose will send x-pp-user with a single value"
        ((Handling) sentRequest).request.getHeaders().findAll("x-pp-user").size() == 1

        and: "Repose will send user from Request body"
        ((Handling) sentRequest).request.getHeaders().findAll("x-pp-user").contains("127.0.0.1;q=0.4")

        and: "Repose will send a single value for x-pp-groups"
        ((Handling) sentRequest).request.getHeaders().findAll("x-pp-groups").size() == 1

        and: "Repose will send 'My Group' for x-pp-groups"
        ((Handling) sentRequest).request.getHeaders().findAll("x-pp-groups").contains("IP_Standard;q=0.4")

        and: "The result will be passed through"
        messageChain.receivedResponse.code == "200"
    }
}
