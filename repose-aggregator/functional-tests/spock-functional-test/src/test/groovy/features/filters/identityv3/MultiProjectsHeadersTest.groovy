package features.filters.identityv3
import framework.ReposeValveTest
import framework.mocks.MockIdentityV3Service
import org.joda.time.DateTime
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain
import spock.lang.Unroll

/**
 * Created by jennyvo on 9/24/14.
 * test option returning multi projects in the headers
 */
class MultiProjectIdsHeadersTest extends ReposeValveTest{
    def static originEndpoint
    def static identityEndpoint
    def static MockIdentityV3Service fakeIdentityV3Service

    def setupSpec() {

        deproxy = new Deproxy()

        def params = properties.defaultTemplateParams
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/filters/identityv3", params)
        repose.configurationProvider.applyConfigs("features/filters/identityv3/multiprojectids", params)
        repose.start()
        waitUntilReadyToServiceRequests('401')

        originEndpoint = deproxy.addEndpoint(properties.targetPort, 'origin service')
        fakeIdentityV3Service = new MockIdentityV3Service(properties.identityPort, properties.targetPort)
        identityEndpoint = deproxy.addEndpoint(properties.identityPort,
                'identity service', null, fakeIdentityV3Service.handler)


    }

    def cleanupSpec() {
        deproxy.shutdown()

        repose.stop()
    }

    def setup(){
        fakeIdentityV3Service.resetHandlers()
    }

    @Unroll
    def "When user have multi-projects will retrieve all projects to headers" () {
        given:
        fakeIdentityV3Service.with {
            client_token = clientToken
            tokenExpiresAt = (new DateTime()).plusDays(1)
            client_projectid = defaultProject
            client_projectid2 = secondProject
        }

        when: "User passes a request through repose with $reqProject"
        MessageChain mc = deproxy.makeRequest(
                url: "$reposeEndpoint/servers/$reqProject",
                method: 'GET',
                headers: ['content-type': 'application/json', 'X-Subject-Token': fakeIdentityV3Service.client_token])

        then: "Everything gets passed as is to the origin service (no matter the user)"
        mc.receivedResponse.code == serviceRespCode

        if (serviceRespCode != "200")
            assert mc.handlings.size() == 0
        else {
            assert mc.handlings.size() == 1
            assert mc.handlings[0].request.headers.findAll("x-project-id").size() == numberProjects
            assert mc.handlings[0].request.headers.findAll("x-project-id").contains(defaultProject)
            assert mc.handlings[0].request.headers.findAll("x-project-id").contains(secondProject)
        }

        where:
        defaultProject  | secondProject   | reqProject      | clientToken       | serviceRespCode   | numberProjects
        "123456"        | "test-project"  | "123456"        |UUID.randomUUID()  | "200"             | 2
        "123456"        | "test-project"  | "test-project"  |UUID.randomUUID()  | "200"             | 2
        "123456"        | "123456"        | "123456"        |UUID.randomUUID()  | "200"             | 1
        "123456"        | "test-project"  | "openstack"     |UUID.randomUUID()  | "401"             | 2
    }
}
