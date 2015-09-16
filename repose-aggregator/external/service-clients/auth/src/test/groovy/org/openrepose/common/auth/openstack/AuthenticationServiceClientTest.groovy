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
package org.openrepose.common.auth.openstack

import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.core.LoggerContext
import org.apache.logging.log4j.test.appender.ListAppender
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.TypeSafeMatcher
import org.mockito.invocation.InvocationOnMock
import org.mockito.stubbing.Answer
import org.openrepose.common.auth.AuthServiceException
import org.openrepose.common.auth.AuthServiceOverLimitException
import org.openrepose.common.auth.ResponseUnmarshaller
import org.openrepose.commons.utils.http.CommonHttpHeader
import org.openrepose.commons.utils.http.ServiceClientResponse
import org.openrepose.commons.utils.transform.jaxb.JaxbEntityToXml
import org.openrepose.core.filter.logic.FilterDirector
import org.openrepose.core.services.serviceclient.akka.AkkaServiceClient
import org.openstack.docs.identity.api.v2.AuthenticationRequest
import org.openstack.docs.identity.api.v2.ObjectFactory
import org.openstack.docs.identity.api.v2.PasswordCredentialsRequiredUsername
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll

import javax.servlet.http.HttpServletResponse
import javax.ws.rs.core.MediaType
import javax.xml.bind.JAXBContext
import javax.xml.datatype.DatatypeFactory

import static org.mockito.Matchers.*
import static org.mockito.Mockito.*

class AuthenticationServiceClientTest extends Specification {
    @Shared
    def objectFactory = new ObjectFactory()
    @Shared
    def coreJaxbContext = JAXBContext.newInstance(
            org.openstack.docs.identity.api.v2.ObjectFactory.class,
            com.rackspace.docs.identity.api.ext.rax_auth.v1.ObjectFactory.class)
    @Shared
    def groupJaxbContext = JAXBContext.newInstance(com.rackspace.docs.identity.api.ext.rax_ksgrp.v1.ObjectFactory.class)
    @Shared
    def jaxbEntityToXml = new JaxbEntityToXml(coreJaxbContext)

    ListAppender app;

    def setup() {
        LoggerContext ctx = (LoggerContext) LogManager.getContext(false)
        app = ((ListAppender) (ctx.getConfiguration().getAppender("List0"))).clear();
    }

    def 'can make a call to an auth service to validate a token'() {
        given:
        def admin = [user: "adminUser", password: "adminPass", tenant: "adminTenant", token: "adminToken"]
        def userToValidate = [user: "normalUser", tenant: "normalTenant", token: "normalToken"]

        def akkaServiceClient = mock(AkkaServiceClient)
        mockAdminTokenRequest(akkaServiceClient, admin)
        mockUserAuthenticationRequest(akkaServiceClient, admin.token, userToValidate)

        def client = createAuthenticationServiceClient(admin.user, admin.password, admin.tenant, akkaServiceClient)

        when:
        def response = client.validateToken(userToValidate.tenant, userToValidate.token, "")

        then:
        response.token.id == userToValidate.token
        response.user.name == userToValidate.user
    }

    def 'throws an AuthServiceException when the admin token can not be retrieved'() {
        given:
        def admin = [user: "adminUser", password: "adminPass", tenant: "adminTenant", token: "adminToken"]

        def akkaServiceClient = mock(AkkaServiceClient)
        mockAdminTokenRequest(akkaServiceClient, admin, 401)
        def client = createAuthenticationServiceClient(admin.user, admin.password, admin.tenant, akkaServiceClient)

        when:
        client.validateToken("123456", "someToken", "")

        then:
        def e = thrown(AuthServiceException)
        e.getMessage() =~ "Unable to retrieve admin token"
        app.getEvents().find {
            it.getMessage().getFormattedMessage() == "Unable to get admin token.  Verify admin credentials. 401"
        }
    }

    @Unroll
    def 'throws an AuthServiceOverLimitException when the admin token request is over limit with $statusCode'() {
        given:
        def admin = [user: "adminUser", password: "adminPass", tenant: "adminTenant", token: "adminToken"]

        def akkaServiceClient = mock(AkkaServiceClient)
        mockAdminTokenRequest(akkaServiceClient, admin, statusCode)
        def client = createAuthenticationServiceClient(admin.user, admin.password, admin.tenant, akkaServiceClient)

        when:
        client.validateToken("123456", "someToken", "")

        then:
        def e = thrown(AuthServiceOverLimitException)
        e.getMessage() =~ "Rate limited by identity service"
        app.getEvents().find {
            it.getMessage().getFormattedMessage() == "Unable to get admin token. Status code: $statusCode"
        }

        where:
        statusCode << [HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE, FilterDirector.SC_TOO_MANY_REQUESTS] // [413, 429]
    }

    def 'reuses the admin token if it is still valid'() {
        given:
        def admin = [user: "adminUser", password: "adminPass", tenant: "adminTenant", token: "adminToken"]
        def userToValidate = [user: "normalUser", tenant: "normalTenant", token: "normalToken"]

        def akkaServiceClient = mock(AkkaServiceClient)
        mockAdminTokenRequest(akkaServiceClient, admin, 200)
        mockUserAuthenticationRequest(akkaServiceClient, admin.token, userToValidate, 200)

        def client = createAuthenticationServiceClient(admin.user, admin.password, admin.tenant, akkaServiceClient)

        when:
        client.validateToken(userToValidate.tenant, userToValidate.token, "")
        def response = client.validateToken(userToValidate.tenant, userToValidate.token, "")

        then:
        verifyAdminTokenRequest(akkaServiceClient, 1)
        verifyUserAuthenticationRequest(akkaServiceClient, admin.token, userToValidate, 2)
        response.token.id == userToValidate.token
        response.user.name == userToValidate.user
    }

    def 'gets a new admin token if the current one is expired and validates the user'() {
        given:
        def admin = [user: "adminUser", password: "adminPass", tenant: "adminTenant", token: "adminToken"]
        def userToValidate = [user: "normalUser", tenant: "normalTenant", token: "normalToken"]

        def akkaServiceClient = mock(AkkaServiceClient)
        def adminAuthRequest = createAuthenticationRequest(admin.user, admin.password, admin.tenant)
        when(akkaServiceClient.post(eq("ADMIN_TOKEN"), eq("http://some/uri/tokens"), any(Map), eq(adminAuthRequest), eq(MediaType.APPLICATION_XML_TYPE)))
                .thenReturn(new ServiceClientResponse(200, new ByteArrayInputStream(createAuthenticateResponse(admin.user, admin.token).getBytes())),
                new ServiceClientResponse(200, new ByteArrayInputStream(createAuthenticateResponse(admin.user, "newAdminToken").getBytes())))
        mockUserAuthenticationRequest(akkaServiceClient, admin.token, userToValidate, 401)
        mockUserAuthenticationRequest(akkaServiceClient, "newAdminToken", userToValidate, 200)

        def client = createAuthenticationServiceClient(admin.user, admin.password, admin.tenant, akkaServiceClient)

        when:
        def response = client.validateToken(userToValidate.tenant, userToValidate.token, "")

        then:
        app.getEvents().find {
            it.getMessage().getFormattedMessage() == "Unable to validate token: normalToken due to status code: 401 :admin token expired. Retrieving new admin token and retrying token validation..."
        }
        response.token.id == userToValidate.token
    }

    @Unroll
    def 'logs a message when the users token is not found when validating a user #desc'() {
        given:
        def admin = [user: "adminUser", password: "adminPass", tenant: "adminTenant", token: "adminToken"]
        def userToValidate = [user: "normalUser", tenant: "normalTenant", token: "normalToken"]
        def codes = userAuthCalls

        def akkaServiceClient = mock(AkkaServiceClient)
        mockAdminTokenRequest(akkaServiceClient, admin)
        def authHeaders = ["Accept": MediaType.APPLICATION_XML, "X-Auth-Token": admin.token, (CommonHttpHeader.TRACE_GUID.toString()): ""]
        when(akkaServiceClient.get("TOKEN:${userToValidate.token}", "http://some/uri/tokens/${userToValidate.token}", authHeaders))
                .thenAnswer(new Answer() {
            def increment = 0

            @Override
            Object answer(InvocationOnMock invocation) throws Throwable {
                return new ServiceClientResponse(codes[increment++], new ByteArrayInputStream(createAuthenticateResponse(userToValidate.user, userToValidate.token).getBytes()))
            }
        })
        def client = createAuthenticationServiceClient(admin.user, admin.password, admin.tenant, akkaServiceClient)

        when:
        client.validateToken(userToValidate.tenant, userToValidate.token, "")

        then:
        app.getEvents().find {
            it.getMessage().getFormattedMessage() == "Unable to validate token.  Invalid token. ${userAuthCalls.last()}"
        }

        where:
        desc                                | adminTokenCalls | userAuthCalls
        "with an admin token"               | 1               | [404]
        "after replacing a bad admin token" | 2               | [401, 404]
    }

    @Unroll
    def 'logs a message and throws an exception for a #statusMap.statusCode status code when validating a user'() {
        given:
        def admin = [user: "adminUser", password: "adminPass", tenant: "adminTenant", token: "adminToken"]
        def userToValidate = [user: "normalUser", tenant: "normalTenant", token: "normalToken"]
        def codes = statusMap.userAuthCalls

        def akkaServiceClient = mock(AkkaServiceClient)
        mockAdminTokenRequest(akkaServiceClient, admin, 200)
        def authHeaders = ["Accept": MediaType.APPLICATION_XML, "X-Auth-Token": admin.token, (CommonHttpHeader.TRACE_GUID.toString()): ""]
        when(akkaServiceClient.get("TOKEN:${userToValidate.token}", "http://some/uri/tokens/${userToValidate.token}", authHeaders))
                .thenAnswer(new Answer() {
            def increment = 0

            @Override
            Object answer(InvocationOnMock invocation) throws Throwable {
                return new ServiceClientResponse(codes[increment++], new ByteArrayInputStream(createAuthenticateResponse(userToValidate.user, userToValidate.token).getBytes()))
            }
        })

        def client = createAuthenticationServiceClient(admin.user, admin.password, admin.tenant, akkaServiceClient)

        when:
        client.validateToken(userToValidate.tenant, userToValidate.token, "")

        then:
        def e = thrown(AuthServiceException)
        e.getMessage() =~ statusMap.errorMessage
        app.getEvents().find { it.getMessage().getFormattedMessage() == statusMap.logMessage }

        where:
        statusMap << getStatusList().collect { code ->
            [statusCode   : code,
             userAuthCalls: [code],
             errorMessage : "Unable to validate token. Response from http://some/uri: $code",
             logMessage   : "Authentication Service returned an unexpected response status code: $code",]
        } + getStatusList().collect { code ->
            [statusCode   : code,
             userAuthCalls: [401, code],
             errorMessage : "Unable to authenticate user with configured Admin credentials",
             logMessage   : "Still unable to validate token: $code",]
        }
    }

    def getStatusList() {
        ((200..599) - 200 - 401 - 404 - 413 - 429)
    }

    def "can get endpoints for a given user token"() {
        given:
        def admin = [user: "adminUser", password: "adminPass", tenant: "adminTenant", token: "adminToken"]
        def userToValidate = [user: "normalUser", tenant: "normalTenant", token: "normalToken"]

        def akkaServiceClient = mock(AkkaServiceClient)
        mockAdminTokenRequest(akkaServiceClient, admin)
        mockUserEndpointRequest(akkaServiceClient, admin.token, userToValidate)

        def client = createAuthenticationServiceClient(admin.user, admin.password, admin.tenant, akkaServiceClient)

        when:
        def response = client.getEndpointsForToken(userToValidate.token, "")

        then:
        response[0].id == 12345
        response[0].publicURL == "http://foo.com"
    }

    def 'gets a new admin token if the current one is expired and gets the endpoint for the user'() {
        given:
        def admin = [user: "adminUser", password: "adminPass", tenant: "adminTenant", token: "adminToken"]
        def userToValidate = [user: "normalUser", tenant: "normalTenant", token: "normalToken"]

        def akkaServiceClient = mock(AkkaServiceClient)
        def adminAuthRequest = createAuthenticationRequest(admin.user, admin.password, admin.tenant)
        when(akkaServiceClient.post(eq("ADMIN_TOKEN"), eq("http://some/uri/tokens"), any(Map), eq(adminAuthRequest), eq(MediaType.APPLICATION_XML_TYPE)))
                .thenReturn(new ServiceClientResponse(200, new ByteArrayInputStream(createAuthenticateResponse(admin.user, admin.token).getBytes())),
                new ServiceClientResponse(200, new ByteArrayInputStream(createAuthenticateResponse(admin.user, "newAdminToken").getBytes())))
        mockUserEndpointRequest(akkaServiceClient, admin.token, userToValidate, 401)
        mockUserEndpointRequest(akkaServiceClient, "newAdminToken", userToValidate, 200)

        def client = createAuthenticationServiceClient(admin.user, admin.password, admin.tenant, akkaServiceClient)

        when:
        def response = client.getEndpointsForToken(userToValidate.token, "")

        then:
        app.getEvents().find {
            it.getMessage().getFormattedMessage() == "Unable to get endpoints for user: 401 :admin token expired. Retrieving new admin token and retrying endpoints retrieval..."
        }
        response[0].id == 12345
        response[0].publicURL == "http://foo.com"
    }

    @Unroll
    def 'throws an AuthServiceOverLimitException when the endpoints request is over limit with $statusCode'() {
        given:
        def admin = [user: "adminUser", password: "adminPass", tenant: "adminTenant", token: "adminToken"]
        def userToValidate = [user: "normalUser", tenant: "normalTenant", token: "normalToken"]

        def akkaServiceClient = mock(AkkaServiceClient)
        mockAdminTokenRequest(akkaServiceClient, admin)
        mockUserEndpointRequest(akkaServiceClient, admin.token, userToValidate, statusCode)

        def client = createAuthenticationServiceClient(admin.user, admin.password, admin.tenant, akkaServiceClient)

        when:
        client.getEndpointsForToken(userToValidate.token, "")

        then:
        def e = thrown(AuthServiceOverLimitException)
        e.getMessage() =~ "Rate limited by identity service"
        app.getEvents().find {
            it.getMessage().getFormattedMessage() == "Unable to get endpoints for token: ${userToValidate.token}. Status code: $statusCode"
        }

        where:
        statusCode << [HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE, FilterDirector.SC_TOO_MANY_REQUESTS] // [413, 429]
    }

    def "when converting a stream, it should return a base 64 encoded string"() {
        given:
        def asc = createAuthenticationServiceClient(null, null, null, Mock(AkkaServiceClient))
        def inputStream = new ByteArrayInputStream("test".getBytes())

        when:
        def convertedStream = asc.convertStreamToBase64String(inputStream)

        then:
        convertedStream == "dGVzdA=="
    }

    private void mockUserEndpointRequest(AkkaServiceClient akkaServiceClient, String adminToken, LinkedHashMap<String, String> userToValidate, int responseCode = 200, int timesCalled = 1) {
        def authHeaders = ["Accept": MediaType.APPLICATION_XML, "X-Auth-Token": adminToken, (CommonHttpHeader.TRACE_GUID.toString()): ""]
        when(akkaServiceClient.get("ENDPOINTS${userToValidate.token}", "http://some/uri/tokens/${userToValidate.token}/endpoints", authHeaders))
                .thenReturn(new ServiceClientResponse(responseCode, new ByteArrayInputStream(createEndpointResponse().getBytes())))
    }

    private void mockUserAuthenticationRequest(AkkaServiceClient akkaServiceClient, String adminToken, LinkedHashMap<String, String> userToValidate, int responseCode = 200) {
        when(akkaServiceClient.get("TOKEN:${userToValidate.token}", "http://some/uri/tokens/${userToValidate.token}", headersForUserAuthentication(adminToken)))
                .thenAnswer(new Answer() {
            @Override
            Object answer(InvocationOnMock invocation) throws Throwable {
                return new ServiceClientResponse(responseCode, new ByteArrayInputStream(createAuthenticateResponse(userToValidate.user, userToValidate.token).getBytes()))
            }
        })
    }

    void mockAdminTokenRequest(AkkaServiceClient akkaServiceClient, LinkedHashMap<String, String> admin, int responseCode = 200) {
        def adminAuthRequest = createAuthenticationRequest(admin.user, admin.password, admin.tenant)
        when(akkaServiceClient.post(eq("ADMIN_TOKEN"), eq("http://some/uri/tokens"), any(Map), eq(adminAuthRequest), eq(MediaType.APPLICATION_XML_TYPE)))
                .thenAnswer(new Answer() {
            @Override
            Object answer(InvocationOnMock invocation) throws Throwable {
                return new ServiceClientResponse(responseCode, new ByteArrayInputStream(createAuthenticateResponse(admin.user, admin.token).getBytes()))
            }
        })
    }

    void verifyAdminTokenRequest(AkkaServiceClient akkaServiceClient, int timesCalled) {
        verify(akkaServiceClient, times(timesCalled)).post(eq("ADMIN_TOKEN"), eq("http://some/uri/tokens"), any(Map), anyString(), eq(MediaType.APPLICATION_XML_TYPE))
    }

    void verifyUserAuthenticationRequest(AkkaServiceClient akkaServiceClient, String adminToken, Map userToValidate, int timesCalled) {
        verify(akkaServiceClient, times(timesCalled)).get(eq("TOKEN:${userToValidate.token}".toString()), eq("http://some/uri/tokens/${userToValidate.token}".toString()), argThat(hasMapValues(headersForUserAuthentication(adminToken))))
    }

    Matcher hasMapValues(Map<String, String> expectedValues) {
        return new TypeSafeMatcher<Map<String, String>>() {
            @Override
            protected boolean matchesSafely(Map<String, String> passedValues) {
                boolean success = true
                Set expectedEntries = expectedValues.entrySet()
                for (Map.Entry<String, String> entry : expectedEntries) {
                    if (!passedValues.get(entry.key).equals(entry.value)) {
                        success = false
                    }
                }
                return success
            }

            @Override
            void describeTo(Description description) {
                description.appendText("The map should contain ").appendValue(expectedValues.toString())
            }
        }
    }

    LinkedHashMap<String, String> headersForUserAuthentication(String adminToken) {
        ["Accept": MediaType.APPLICATION_XML,
         "X-Auth-Token": adminToken,
         (CommonHttpHeader.TRACE_GUID.toString()): ""]
    }

    def createAuthenticationServiceClient(def adminUser, def adminPassword, def adminTenant, def akkaServiceClient) {
        new AuthenticationServiceClient("http://some/uri", adminUser, adminPassword, adminTenant,
                new ResponseUnmarshaller(coreJaxbContext), new ResponseUnmarshaller(groupJaxbContext),
                jaxbEntityToXml, akkaServiceClient)
    }

    private String createEndpointResponse() {
        def endpointList = objectFactory.createEndpointList()
        endpointList.getEndpoint().add(objectFactory.createEndpoint().with {
            id = 12345
            publicURL = new URL("http://foo.com")
            it
        })
        jaxbEntityToXml.transform(objectFactory.createEndpoints(endpointList))
    }

    private String createAuthenticationRequest(def adminUser, def adminPassword, def adminTenant) {
        PasswordCredentialsRequiredUsername credentials = objectFactory.createPasswordCredentialsRequiredUsername().with {
            username = adminUser
            password = adminPassword
            it
        }
        AuthenticationRequest request = objectFactory.createAuthenticationRequest().with {
            tenantId = adminTenant
            credential = objectFactory.createPasswordCredentials(credentials)
            it
        }
        jaxbEntityToXml.transform(objectFactory.createAuth(request))
    }

    private String createAuthenticateResponse(def username, def token) {
        def authenticateResponse = objectFactory.createAuthenticateResponse().with {
            it.token = objectFactory.createToken().with {
                it.id = token
                def time = new GregorianCalendar()
                time.setTime(new Date() + 1)
                it.expires = DatatypeFactory.newInstance().newXMLGregorianCalendar(time)
                it
            }
            it.user = objectFactory.createUserForAuthenticateResponse().with {
                it.name = username
                it
            }
            it
        }
        jaxbEntityToXml.transform(objectFactory.createAccess(authenticateResponse))
    }
}
