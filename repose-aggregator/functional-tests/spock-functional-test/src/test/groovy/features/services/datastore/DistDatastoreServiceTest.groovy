package features.services.datastore
import framework.ReposeValveTest
import framework.category.Slow
import org.junit.experimental.categories.Category
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain
import org.rackspace.deproxy.Response

@Category(Slow.class)
class DistDatastoreServiceTest extends ReposeValveTest {
    boolean isFailedStart = false

    static def distDatastoreEndpoint = "http://localhost:4999"

    def setup(){
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.getProperty("target.port").toInteger())
    }

    def cleanup(){
        if (deproxy)
            deproxy.shutdown()

        if(!getIsFailedStart())
            repose.stop()
        setIsFailedStart(false)
    }

    def "when configured with DD service, repose should start and successfully execute calls" () {
        given:
        repose.applyConfigs("features/services/datastore")
        repose.start()
        waitUntilReadyToServiceRequests()

        when:
        MessageChain mc = deproxy.makeRequest([url:reposeEndpoint + "/cluster",headers:['x-trace-request': 'true']])

        then:
        mc.receivedResponse.code == '200'
        mc.handlings.size() == 1
    }

    def "when configured with DD service and filter, repose should not start" () {
        given:
        repose.applyConfigs("features/services/datastore/badconfig")
        setIsFailedStart(true)

        def MessageChain mc


        when:
        try{
            repose.start()
            mc = deproxy.makeRequest([url:reposeEndpoint + "/cluster",headers:['x-trace-request': 'true']])
        } catch(Exception e){

        }

        then:
        mc == null
    }


    def "when configured with DD filter, repose should start and log a warning" () {
        given:
        cleanLogDirectory()
        repose.applyConfigs("features/filters/datastore")
        repose.start()
        def user= UUID.randomUUID().toString();

        when:
        MessageChain mc = deproxy.makeRequest([url:reposeEndpoint,headers:['X-PP-USER': user, 'X-PP-Groups' : "BETA_Group"]])

        then:
        mc.receivedResponse.code == '200'
        mc.handlings.size() == 1
        def List<String> logMatches = reposeLogSearch.searchByString(
                "Use of the dist-datastore filter is deprecated. Please use the distributed datastore service.");
        logMatches.size() == 1
    }

    def "when configured with DD filter and adding a service, repose should log a warning and continue running with previous config" () {
        given:
        def List<String> logMatchesTrue
        def List<String> logMatchesFalse
        cleanLogDirectory()
        repose.applyConfigs("features/filters/datastore")
        repose.start()
        logMatchesFalse = reposeLogSearch.searchByString(
                "The distributed datastore filter and service can not be used at the same time, within the same cluster. Please check your configuration.");
        repose.updateConfigs("features/services/datastore/badconfig")
        logMatchesTrue = reposeLogSearch.searchByString(
                "The distributed datastore filter and service can not be used at the same time, within the same cluster. Please check your configuration.");
        def user= UUID.randomUUID().toString();

        when:
        MessageChain mc = deproxy.makeRequest([url:reposeEndpoint,headers:['X-PP-USER': user, 'X-PP-Groups' : "BETA_Group"]])

        then:
        mc.receivedResponse.code == '200'
        mc.handlings.size() == 1
        logMatchesTrue.size() > logMatchesFalse.size()
    }

    def "when configured with DD service and adding a filter, repose should log a warning and continue running with previous config" () {
        given:
        def List<String> logMatchesTrue
        def List<String> logMatchesFalse
        cleanLogDirectory()
        repose.applyConfigs("features/services/datastore")
        repose.start()
        waitUntilReadyToServiceRequests()
        logMatchesFalse = reposeLogSearch.searchByString(
                "The distributed datastore filter and service can not be used at the same time, within the same cluster. Please check your configuration.");
        repose.updateConfigs("features/services/datastore/badconfig")
        logMatchesTrue = reposeLogSearch.searchByString(
                "The distributed datastore filter and service can not be used at the same time, within the same cluster. Please check your configuration.");
        def user= UUID.randomUUID().toString();

        when:
        MessageChain mc = deproxy.makeRequest([url:reposeEndpoint + "/cluster",headers:['X-PP-USER': user, 'X-PP-Groups' : "BETA_Group"]])

        then:
        mc.receivedResponse.code == '200'
        mc.handlings.size() == 1
        logMatchesTrue.size() > logMatchesFalse.size()
    }

    def "when putting cache objects" () {
        given:
        repose.applyConfigs("features/services/datastore")
        repose.start()
        waitUntilReadyToServiceRequests()
        def headers = ['X-PP-Host-Key':'temp', 'X-TTL':'5']
        def objectkey = '8e969a44-990b-de49-d894-cf200b7d4c11'
        def body = "test data"


        when:
        MessageChain mc =
            deproxy.makeRequest(
                    [
                            method: 'PUT',
                            url:distDatastoreEndpoint + "/powerapi/dist-datastore/objects/" + objectkey,
                            headers:headers,
                            body: body
                    ])

        then:
        mc.receivedResponse.code == '202'
    }

    def "when checking cache object time to live"(){
        given:
        repose.applyConfigs("features/services/datastore")
        repose.start()
        def headers = ['X-PP-Host-Key':'temp', 'X-TTL':'5']
        def objectkey = '8e969a44-990b-de49-d894-cf200b7d4c11'
        def body = "test data"
        MessageChain mc =
            deproxy.makeRequest(
                    [
                            method: 'PUT',
                            url:distDatastoreEndpoint + "/powerapi/dist-datastore/objects/" + objectkey,
                            headers:headers,
                            requestBody: body
                    ])
        mc =
            deproxy.makeRequest(
                    [
                            method: 'GET',
                            url:distDatastoreEndpoint + "/powerapi/dist-datastore/objects/" + objectkey,
                            headers:headers
                    ])
        mc.receivedResponse.code == '200'

        when:
        Thread.sleep(7500)
        mc =
            deproxy.makeRequest(
                    [
                            method: 'GET',
                            url:distDatastoreEndpoint + "/powerapi/dist-datastore/objects/" + objectkey,
                            headers:headers
                    ])

        then:
        mc.receivedResponse.code == '404'

    }

    def "when deleting cache objects"(){
        given:
        repose.applyConfigs("features/services/datastore")
        repose.start()
        def headers = ['X-PP-Host-Key':'temp', 'x-ttl':'1000']
        def objectkey = '8e969a44-990b-de49-d894-cf200b7d4c11'
        def body = "test data"
        def url = distDatastoreEndpoint + "/powerapi/dist-datastore/objects/" + objectkey


        when: "Adding the object to the datastore"
        MessageChain mc =
            deproxy.makeRequest(
                    [
                            method: "PUT",
                            url:url,
                            headers:headers,
                            requestBody: body
                    ])

        then: "should report success"
        mc.receivedResponse.code == "202"
        mc.receivedResponse.body == ""



        when: "checking that it's there"
        mc =
            deproxy.makeRequest(
                    [
                            method: "GET",
                            url:url,
                            headers:headers
                    ])

        then: "should report that it is"
        mc.receivedResponse.code == "200"
        mc.receivedResponse.body == body



        when: "deleting the object from the datastore"
        mc =
            deproxy.makeRequest(
                    [
                            method: "DELETE",
                            url:url,
                            headers:headers,
                    ])

        then: "should report that it was successfully deleted"
        mc.receivedResponse.code == "202"
        mc.receivedResponse.body == ""



        when: "checking that it's gone"
        mc =
            deproxy.makeRequest(
                    [
                            method: "GET",
                            url:url,
                            headers:headers,
                    ])

        then: "should report it missing"
        mc.receivedResponse.code == "404"
        mc.receivedResponse.body == ""
    }

    def "Should not split request headers according to rfc"() {
        given:
        repose.applyConfigs("features/services/datastore")
        repose.start()
        waitUntilReadyToServiceRequests()
        def userAgentValue = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_8_4) " +
                "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/29.0.1547.65 Safari/537.36"
        def reqHeaders =
            [
                    "user-agent": userAgentValue,
                    "x-pp-user": "usertest1, usertest2, usertest3",
                    "accept": "application/xml;q=1 , application/json;q=0.5"
            ]

        when: "User sends a request through repose"
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint + "/test", method: 'GET', headers: reqHeaders)

        then:
        mc.handlings.size() == 1
        mc.handlings[0].request.getHeaders().findAll("user-agent").size() == 1
        mc.handlings[0].request.headers['user-agent'] == userAgentValue
        mc.handlings[0].request.getHeaders().findAll("x-pp-user").size() == 3
        mc.handlings[0].request.getHeaders().findAll("accept").size() == 2
    }
}
