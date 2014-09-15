package features.filters.openstackidentitybasicauth

import framework.ReposeValveTest
import framework.mocks.MockIdentityService
import org.joda.time.DateTime
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.Request
import org.rackspace.deproxy.Response

import javax.servlet.http.HttpServletResponse
import javax.ws.rs.core.HttpHeaders

class OpenStackIdentityBasicAuthTest extends ReposeValveTest {

    def static originEndpoint
    def static identityEndpoint

    def static MockIdentityService fakeIdentityService

    def setupSpec() {
        int deproxyPort = properties.targetPort
        int reposePort = properties.reposePort
        deproxy = new Deproxy()
        deproxy.addEndpoint(deproxyPort)

        def params = properties.getDefaultTemplateParams()
        repose.configurationProvider.cleanConfigDirectory()
        repose.configurationProvider.applyConfigs("common", params);
        repose.configurationProvider.applyConfigs("features/filters/openstackidentitybasicauth", params);

        repose.start()

        //TODO: The port finding logic is not working!
        originEndpoint = deproxy.addEndpoint(properties.targetPort + 10, 'origin service')
        fakeIdentityService = new MockIdentityService(properties.identityPort, properties.targetPort)
        identityEndpoint = deproxy.addEndpoint(properties.identityPort, 'identity service', null, fakeIdentityService.handler)

        fakeIdentityService.with {
            generateTokenHandler = { Request request, xml -> new Response(HttpServletResponse.SC_NO_CONTENT)
                // TODO: Flesh out the generateTokenHandler.
                // IF the body is a userName/apiKey request,
                // THEN return the Client token response;
                // ELSE IF the body is userName/passWord request,
                // THEN return the Admin token response.
            }
            validateTokenHandler = { tokenId, request, xml -> new Response(HttpServletResponse.SC_NO_CONTENT)
                // TODO: Flesh out the validateTokenHandler.
                // IF the tokenID matches the token from the Client token response,
                // THEN return the Token Validated response.
            }
        }
    }

    def cleanupSpec() {
        if (deproxy) {
            deproxy.shutdown()
        }

        if (repose) {
            repose.stop()
        }
    }

    def setup() {
        sleep 500
        fakeIdentityService.resetHandlers()
    }

    def "When the request does not have an HTTP Basic authentication header, then simply pass it on down the filter chain and this configuration will respond with a 401 and add an HTTP Basic authentication header"() {
        when: "the request does not have an HTTP Basic authentication header"
        def messageChain = deproxy.makeRequest([url: reposeEndpoint])

        then: "simply pass it on down the filter chain and this configuration will respond with a 401 and add an HTTP Basic authentication header"
        messageChain.receivedResponse.code == "401"
        messageChain.receivedResponse.getHeaders().findAll(HttpHeaders.WWW_AUTHENTICATE).contains("Basic realm=\"RAX-KEY\"")
        messageChain.getOrphanedHandlings().empty
    }

    def "When the request does have an HTTP Basic authentication header, then get a token and validate it"() {
        given:
        fakeIdentityService.with {
            //client_userid = reqTenant
            //client_tenant = reqTenant
            client_token = UUID.randomUUID().toString()
            tokenExpiresAt = DateTime.now().plusDays(1)
            generateTokenHandler = {
                request, xml ->
                    new Response(HttpServletResponse.SC_NO_CONTENT, null, null, null)
            }
        }

        when: "the request does have an HTTP Basic authentication header"
        def messageChain = deproxy.makeRequest([url: reposeEndpoint])

        then: "then get a token and validate it"
        messageChain.receivedResponse.code == "200"
        messageChain.receivedResponse.getBody().toString().contains(":-)")
        messageChain.getOrphanedHandlings().empty
    }

    def "When the request does have an HTTP Basic authentication header that is cached, then use the cached token"() {
        when: "the request does have an HTTP Basic authentication header that is cached"
        def messageChain = deproxy.makeRequest([url: reposeEndpoint])

        then: "use the cached token"
        messageChain.receivedResponse.code == "200"
        messageChain.receivedResponse.getBody().toString().contains(":-)")
        messageChain.getOrphanedHandlings().size() == 1
    }
}
