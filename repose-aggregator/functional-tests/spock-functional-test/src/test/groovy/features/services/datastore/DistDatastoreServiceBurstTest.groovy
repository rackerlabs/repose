package features.services.datastore

import com.rackspace.papi.commons.util.io.ObjectSerializer
import framework.ReposeValveTest
import framework.category.Slow
import org.junit.experimental.categories.Category
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain
import org.rackspace.deproxy.PortFinder

@Category(Slow.class)
class DistDatastoreServiceBurstTest extends ReposeValveTest {
    static def datastoreEndpoint1

    def setup() {
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort)
        int dataStorePort1 = PortFinder.Singleton.getNextOpenPort()

        datastoreEndpoint1 = "http://localhost:${dataStorePort1}"

        def params = properties.getDefaultTemplateParams()
        params += [
                'datastorePort1' : dataStorePort1
        ]
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/services/datastore/burst/", params)
        repose.start()
        waitUntilReadyToServiceRequests("401")
    }

    def cleanup() {
        if (deproxy)
            deproxy.shutdown()

        if (repose)
            repose.stop()

    }

    def "under heavy load should not go over specified rate limit"() {

        given:
        List<Thread> clientThreads = new ArrayList<Thread>()
        def totalSuccessfulCount = 0
        def totalFailedCount = 0
        List<String> requests = new ArrayList()
        def headers = ['X-PP-USER': '1']
        int rate_limiting_count = 10

        for (x in 1..numClients) {

            def thread = Thread.start {
                def threadNum = x

                for (i in 1..callsPerClient) {
                    requests.add('spock-thread-' + threadNum + '-request-' + i)
                    def messageChain = deproxy.makeRequest(url: (String) reposeEndpoint, method: "GET", headers: headers)
                    if(messageChain.receivedResponse.code.equals("200"))
                        totalSuccessfulCount = totalSuccessfulCount + 1
                    else
                        totalFailedCount = totalFailedCount + 1

                }
            }
            clientThreads.add(thread)
        }

        when:
        clientThreads*.join()

        then:
        println totalSuccessfulCount
        println totalFailedCount
        println requests.size()
        inRange(totalSuccessfulCount, rate_limiting_count)

        where:
        numClients | callsPerClient
        30         | 20
        10         | 50
        50         | 10

    }


    def "under heavy load and small ttl - PATCH a new cache object should return 200 response and no errors" () {
        given:
        def headers = ['X-PP-Host-Key':'temp', 'X-TTL':'1']
        def objectkey1 = UUID.randomUUID().toString();
        def objectkey2 = UUID.randomUUID().toString();
        def body = ObjectSerializer.instance().writeObject(new com.rackspace.papi.components.datastore.StringValue.Patch("test data"))
        List<Thread> clientThreads = new ArrayList<Thread>()
        List<String> requests = new ArrayList()
        MessageChain mc =
            deproxy.makeRequest(
                    [
                            method: 'PATCH',
                            url:datastoreEndpoint1 + "/powerapi/dist-datastore/objects/" + objectkey1,
                            headers:headers,
                            requestBody: body
                    ])
        mc =
            deproxy.makeRequest(
                    [
                            method: 'PATCH',
                            url:datastoreEndpoint1 + "/powerapi/dist-datastore/objects/" + objectkey2,
                            headers:headers,
                            requestBody: body
                    ])
        Thread.sleep(945)
        for (x in 1..numClients) {

            def thread = Thread.start {
                def threadNum = x

                for (i in 1..callsPerClient) {
                    requests.add('spock-thread-' + threadNum + '-request-' + i)
                    mc =
                        deproxy.makeRequest(
                                [
                                        method: 'PATCH',
                                        url:datastoreEndpoint1 + "/powerapi/dist-datastore/objects/" + objectkey1,
                                        headers:headers,
                                        requestBody: body
                                ])
                    mc.receivedResponse.code == '200'
                    mc =
                        deproxy.makeRequest(
                                [
                                        method: 'PATCH',
                                        url:datastoreEndpoint1 + "/powerapi/dist-datastore/objects/" + objectkey2,
                                        headers:headers,
                                        requestBody: body
                                ])
                    mc.receivedResponse.code == '200'
                }
            }
            clientThreads.add(thread)
        }

        when:
        clientThreads*.join()

        then:
        true

        where:
        numClients | callsPerClient
        30         | 20
    }
    private void inRange(int totalSuccessfulCount, int rateLimitingCount){
        int minRange = Math.floor(rateLimitingCount - (rateLimitingCount * 0.03))
        int maxRange = Math.ceil(rateLimitingCount + (rateLimitingCount * 0.03))
        assert totalSuccessfulCount >= minRange
        assert totalSuccessfulCount <= maxRange
    }
}
