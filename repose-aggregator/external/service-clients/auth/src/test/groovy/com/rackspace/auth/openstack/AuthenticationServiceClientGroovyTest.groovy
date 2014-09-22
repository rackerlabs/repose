package com.rackspace.auth.openstack

import com.rackspace.auth.AuthServiceException
import com.rackspace.auth.ResponseUnmarshaller
import com.rackspace.papi.commons.util.http.ServiceClientResponse
import com.rackspace.papi.commons.util.transform.jaxb.JaxbEntityToXml
import com.rackspace.papi.service.serviceclient.akka.AkkaServiceClient
import org.openstack.docs.identity.api.v2.AuthenticationRequest
import org.openstack.docs.identity.api.v2.ObjectFactory
import org.openstack.docs.identity.api.v2.PasswordCredentialsRequiredUsername
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll

import javax.ws.rs.core.MediaType
import javax.xml.bind.JAXBContext
import javax.xml.datatype.DatatypeFactory

class AuthenticationServiceClientGroovyTest extends Specification {
    @Shared def objectFactory = new ObjectFactory()
    @Shared def coreJaxbContext = JAXBContext.newInstance(
                org.openstack.docs.identity.api.v2.ObjectFactory.class,
                com.rackspace.docs.identity.api.ext.rax_auth.v1.ObjectFactory.class)
    @Shared def groupJaxbContext = JAXBContext.newInstance(com.rackspace.docs.identity.api.ext.rax_ksgrp.v1.ObjectFactory.class)
    @Shared def jaxbEntityToXml = new JaxbEntityToXml(coreJaxbContext)

    def setup() {
        AppenderForTesting.clear()
    }

    def 'can make a call to an auth service to validate a token'() {
        given:
        def admin = [user: "adminUser", password: "adminPass", tenant: "adminTenant", token: "adminToken"]
        def userToValidate = [user: "normalUser", tenant: "normalTenant", token: "normalToken"]

        def akkaServiceClient = Mock(AkkaServiceClient)
        mockAdminTokenRequest(admin, akkaServiceClient)
        mockUserAuthenticationRequest(akkaServiceClient, admin.token, userToValidate)

        def client = createAuthenticationServiceClient(admin.user, admin.password, admin.tenant, akkaServiceClient)

        when:
        def response = client.validateToken(userToValidate.tenant, userToValidate.token)

        then:
        response.token.id == userToValidate.token
        response.user.name == userToValidate.user
    }

    def 'throws an AuthServiceException when the admin token can not be retrieved'() {
        given:
        def admin = [user: "adminUser", password: "adminPass", tenant: "adminTenant", token: "adminToken"]

        def akkaServiceClient = Mock(AkkaServiceClient)
        mockAdminTokenRequest(admin, akkaServiceClient, 401)
        def client = createAuthenticationServiceClient(admin.user, admin.password, admin.tenant, akkaServiceClient)

        when:
        client.validateToken("123456", "someToken")

        then:
        def e = thrown(AuthServiceException)
        e.getMessage() =~ "Unable to retrieve admin token"
        AppenderForTesting.getMessages().find { it =~ "Unable to get admin token.  Verify admin credentials. 401" }
    }

    def 'reuses the admin token if it is still valid'() {
        given: "We have no admin token on the first pass"
        def admin = [user: "adminUser", password: "adminPass", tenant: "adminTenant", token: "adminToken"]
        def userToValidate = [user: "normalUser", tenant: "normalTenant", token: "normalToken"]
        def newTokenToValidate = "newToken"

        def akkaServiceClient = Mock(AkkaServiceClient)
        mockAdminTokenRequest(admin, akkaServiceClient)
        mockUserAuthenticationRequest(akkaServiceClient, admin.token, userToValidate)

        def client = createAuthenticationServiceClient(admin.user, admin.password, admin.tenant, akkaServiceClient)

        when: "We ask to validate a token"
        def response = client.validateToken(userToValidate.tenant, userToValidate.token)

        then: "We get a response and an admin token is retrieved"
        response.token.id == userToValidate.token

        when: "The admin token call would provide a new token"
        akkaServiceClient.post("ADMIN_TOKEN", _, _, _, _) >>
                new ServiceClientResponse(200, new ByteArrayInputStream(createAuthenticateResponse(admin.user, "BrandNewToken").getBytes()))
        def authHeaders = ["Accept": MediaType.APPLICATION_XML, "X-Auth-Token": admin.token]
        akkaServiceClient.get("TOKEN:$newTokenToValidate", "http://some/uri/tokens/$newTokenToValidate", authHeaders) >>
                new ServiceClientResponse(200, new ByteArrayInputStream(createAuthenticateResponse(userToValidate.user, newTokenToValidate).getBytes()))
        response = client.validateToken(userToValidate.tenant, newTokenToValidate)

        then: "We get a response using the old admin token when validating a new user token"
        response.token.id == newTokenToValidate
    }

    def 'gets a new admin token if the current one is expired and validates the user'() {
        given:
        def admin = [user: "adminUser", password: "adminPass", tenant: "adminTenant", token: "adminToken"]
        def userToValidate = [user: "normalUser", tenant: "normalTenant", token: "normalToken"]

        def akkaServiceClient = Mock(AkkaServiceClient)
        def adminAuthRequest = createAuthenticationRequest(admin.user, admin.password, admin.tenant)
        akkaServiceClient.post("ADMIN_TOKEN", "http://some/uri/tokens", _, adminAuthRequest, MediaType.APPLICATION_XML_TYPE) >>>
                [new ServiceClientResponse(200, new ByteArrayInputStream(createAuthenticateResponse(admin.user, admin.token).getBytes())),
                 new ServiceClientResponse(200, new ByteArrayInputStream(createAuthenticateResponse(admin.user, "newAdminToken").getBytes())),]
        mockUserAuthenticationRequest(akkaServiceClient, admin.token, userToValidate, 401)
        mockUserAuthenticationRequest(akkaServiceClient, "newAdminToken", userToValidate, 200)

        def client = createAuthenticationServiceClient(admin.user, admin.password, admin.tenant, akkaServiceClient)

        when:
        def response = client.validateToken(userToValidate.tenant, userToValidate.token)

        then:
        AppenderForTesting.getMessages().find {
            it =~ "Unable to validate token: 401 :admin token expired. Retrieving new admin token and retrying token validation..."
        }
        response.token.id == userToValidate.token
    }

    def 'logs a message when the users token is not found when validating a user'() {
        given:
        def statusCode = 404
        def admin = [user: "adminUser", password: "adminPass", tenant: "adminTenant", token: "adminToken"]
        def userToValidate = [user: "normalUser", tenant: "normalTenant", token: "normalToken"]

        def akkaServiceClient = Mock(AkkaServiceClient)
        mockAdminTokenRequest(admin, akkaServiceClient)
        mockUserAuthenticationRequest(akkaServiceClient, admin.token, userToValidate, statusCode)

        def client = createAuthenticationServiceClient(admin.user, admin.password, admin.tenant, akkaServiceClient)

        when:
        client.validateToken(userToValidate.tenant, userToValidate.token)

        then:
        AppenderForTesting.getMessages().find { it =~ "Unable to validate token.  Invalid token. $statusCode" }
    }

    @Unroll
    def 'logs a message and throws an exception for a #statusCode status code when validating a user'() {
        given:
        def admin = [user: "adminUser", password: "adminPass", tenant: "adminTenant", token: "adminToken"]
        def userToValidate = [user: "normalUser", tenant: "normalTenant", token: "normalToken"]

        def akkaServiceClient = Mock(AkkaServiceClient)
        mockAdminTokenRequest(admin, akkaServiceClient)
        mockUserAuthenticationRequest(akkaServiceClient, admin.token, userToValidate, statusCode)

        def client = createAuthenticationServiceClient(admin.user, admin.password, admin.tenant, akkaServiceClient)

        when:
        client.validateToken(userToValidate.tenant, userToValidate.token)

        then:
        def e = thrown(AuthServiceException)
        e.getMessage() =~ "Unable to validate token. Response from http://some/uri: $statusCode"
        AppenderForTesting.getMessages().find { it =~ "Authentication Service returned an unexpected response status code: $statusCode" }

        where:
        statusCode << (200..599) - 200 - 401 - 404
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


    private void mockUserAuthenticationRequest(AkkaServiceClient akkaServiceClient, String adminToken, LinkedHashMap<String, String> userToValidate, int responseCode = 200) {
        def authHeaders = ["Accept": MediaType.APPLICATION_XML, "X-Auth-Token": adminToken]
        1 * akkaServiceClient.get("TOKEN:${userToValidate.token}", "http://some/uri/tokens/${userToValidate.token}", authHeaders) >>
                new ServiceClientResponse(responseCode, new ByteArrayInputStream(createAuthenticateResponse(userToValidate.user, userToValidate.token).getBytes()))
    }

    private void mockAdminTokenRequest(LinkedHashMap<String, String> admin, AkkaServiceClient akkaServiceClient, int responseCode = 200) {
        def adminAuthRequest = createAuthenticationRequest(admin.user, admin.password, admin.tenant)
        1 * akkaServiceClient.post("ADMIN_TOKEN", "http://some/uri/tokens", _, adminAuthRequest, MediaType.APPLICATION_XML_TYPE) >>
                new ServiceClientResponse(responseCode, new ByteArrayInputStream(createAuthenticateResponse(admin.user, admin.token).getBytes()))
    }

    def createAuthenticationServiceClient(def adminUser, def adminPassword, def adminTenant, def akkaServiceClient) {
        new AuthenticationServiceClient("http://some/uri", adminUser, adminPassword, adminTenant,
                new ResponseUnmarshaller(coreJaxbContext), new ResponseUnmarshaller(groupJaxbContext),
                jaxbEntityToXml, akkaServiceClient)
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
