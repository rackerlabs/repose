package features.services.datastore

import com.rackspace.papi.commons.util.io.ObjectSerializer
import com.rackspace.papi.components.datastore.Patchable
import com.rackspace.papi.components.datastore.distributed.SerializablePatch
import framework.ReposeValveTest
import framework.category.Slow
import org.junit.experimental.categories.Category
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain

@Category(Slow.class)
class DistDatastoreServiceTest extends ReposeValveTest {

    static def params

    def setupSpec() {
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort)

        params = properties.getDefaultTemplateParams()
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/services/datastore", params)
        repose.start()
        waitUntilReadyToServiceRequests()
    }

    def cleanupSpec() {
        if (deproxy)
            deproxy.shutdown()
        repose.stop()
    }

    def "when configured with DD service, repose should start and successfully execute calls" () {
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


    def "PATCH a new cache object should return 200 response" () {
        given:
        def headers = ['X-PP-Host-Key':'temp', 'X-TTL':'5']
        def objectkey = '8e969a44-990b-de49-d894-cf200b7d4c11'
        def body = ObjectSerializer.instance().writeObject(new TestValue.Patch("test data"))

        when:
        MessageChain mc =
            deproxy.makeRequest(
                    [
                            method: 'PATCH',
                            url:distDatastoreEndpoint + "/powerapi/dist-datastore/objects/" + objectkey,
                            headers:headers,
                            body: body
                    ])

        then:
        mc.receivedResponse.code == '200'
    }

    def "PATCH a cache object to an existing key should patch the cached value"() {
        given:
        def headers = ['X-PP-Host-Key':'temp', 'X-TTL':'5']
        def objectkey = '8e969a44-990b-de49-d894-cf200b7d4c11'
        def body = ObjectSerializer.instance().writeObject(new TestValue.Patch("original value"))
        def newBody = ObjectSerializer.instance().writeObject(new TestValue.Patch(" patched on value"))

        when: "I make 2 PATCH calls for 2 different values for the same key"
        MessageChain mc1 = deproxy.makeRequest(
                [
                        method: 'PATCH',
                        url:distDatastoreEndpoint + "/powerapi/dist-datastore/objects/" + objectkey,
                        headers:headers,
                        body: body
                ])
        MessageChain mc2 = deproxy.makeRequest(
                [
                        method: 'PATCH',
                        url:distDatastoreEndpoint + "/powerapi/dist-datastore/objects/" + objectkey,
                        headers:headers,
                        body: newBody
                ])

        and: "I get the value for the key"
        MessageChain mc3 = deproxy.makeRequest(
                [
                        method: 'GET',
                        url:distDatastoreEndpoint + "/powerapi/dist-datastore/objects/" + objectkey,
                        headers:headers
                ])

        then: "The body of the get response should be my second request body"
        mc1.receivedResponse.code == "200"
        mc2.receivedResponse.code == "200"
        ObjectSerializer.instance().readObject(mc2.receivedResponse.body as byte[]).value == "original value patched on value"
        ObjectSerializer.instance().readObject(mc3.receivedResponse.body as byte[]).value == "original value patched on value"
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
        waitUntilReadyToServiceRequests()
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
        waitUntilReadyToServiceRequests()
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

    public static class TestValue implements Patchable<TestValue, TestValue.Patch>, Serializable {
        private String value;

        public TestValue(String value) {
            this.value = value;
        }

        @Override
        public TestValue applyPatch(Patch patch) {
            String originalValue = value;
            value = value + patch.newFromPatch().getValue();
            return new TestValue(originalValue + patch.newFromPatch().getValue());
        }

        public String getValue() {
            return value;
        }

        public static class Patch implements SerializablePatch<TestValue> {
            private String value;

            public Patch(String value) {
                this.value = value;
            }

            @Override
            public TestValue newFromPatch() {
                return new TestValue(value);
            }
        }
    }

}
