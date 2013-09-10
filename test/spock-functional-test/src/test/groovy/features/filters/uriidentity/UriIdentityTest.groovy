package features.filters.uriidentity

import framework.ReposeValveTest
import framework.category.Slow
import org.junit.experimental.categories.Category
import org.rackspace.gdeproxy.Deproxy
import org.rackspace.gdeproxy.Handling
import org.rackspace.gdeproxy.MessageChain

@Category(Slow.class)
class UriIdentityTest extends ReposeValveTest {

    def setupSpec() {
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.getProperty("target.port").toInteger())

        repose.applyConfigs("features/filters/uriidentity")
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

    def "when identifying requests by uri"() {

        when: "Request contains the user within the uri"
        def messageChain = deproxy.makeRequest([url: reposeEndpoint+"/service/reposeuser1/something"])
        def sentRequest = ((MessageChain) messageChain).getHandlings()[0]

        then: "Repose will send x-pp-user with a single value"
        ((Handling) sentRequest).request.getHeaders().findAll("x-pp-user").contains("reposeuser1;q=0.5")

        and: "Repose will send x-pp-groups with value of User_Standard"
        ((Handling) sentRequest).request.getHeaders().findAll("x-pp-groups").contains("User_Standard;q=0.5")


    }

    def "when identifying requests on uri without user"(){
        when: "Request does not contain the user within the uri"
        def messageChain = deproxy.makeRequest([url: reposeEndpoint+"/resource/reposeuser1/something"])
        def sentRequest = ((MessageChain) messageChain).getHandlings()[0]

        then: "Repose will not send a x-pp-user header"
        ((Handling) sentRequest).request.getHeaders().findAll("x-pp-user").size() == 0

        and: "Repose will not send a value for x-pp-groups"
        ((Handling) sentRequest).request.getHeaders().findAll("x-pp-groups").size() == 0


    }
 }
