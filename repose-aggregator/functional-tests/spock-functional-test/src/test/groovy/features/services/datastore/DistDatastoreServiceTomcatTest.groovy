package features.services.datastore
import com.rackspace.papi.commons.util.io.ObjectSerializer
import com.rackspace.papi.components.datastore.StringValue
import framework.*
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain
import org.rackspace.deproxy.PortFinder
import org.rackspace.deproxy.Response
import org.spockframework.runtime.SpockAssertionError
import spock.lang.Ignore
import spock.lang.Specification
/**
 * Test the Distributed Datastore Service in 2 multinode containers
 */

class DistDatastoreServiceTomcatTest extends Specification {

    static def reposeTomcatEndpoint1
    static def reposeTomcatEndpoint2
    static def datastoreTomcatEndpoint1
    static def datastoreTomcatEndpoint2

    static Deproxy deproxy

    static ReposeLauncher repose1
    static ReposeLauncher repose2

    static def params

    def setupSpec() {

        def TestProperties properties = new TestProperties()
        def logFile = properties.logFile

        int originServicePort = properties.targetPort

        println("Deproxy: " + originServicePort)
        // start deproxy
        deproxy = new Deproxy()
        deproxy.addEndpoint(originServicePort)


        int reposePort1 = properties.reposePort
        int reposePort2 = PortFinder.Singleton.getNextOpenPort()
        int dataStorePort1 = PortFinder.Singleton.getNextOpenPort()
        int dataStorePort2 = PortFinder.Singleton.getNextOpenPort()
        int shutdownPort1 = properties.reposeShutdownPort
        int shutdownPort2 = PortFinder.Singleton.getNextOpenPort()

        println("repose1: " + reposePort1 + "\nrepose2: " + reposePort2 + "\ndatastore1: " + dataStorePort1 + "\n" +
                "datastore2: " + dataStorePort2)

        // configure and start repose

        reposeTomcatEndpoint1 = "http://localhost:${reposePort1}"
        reposeTomcatEndpoint2 = "http://localhost:${reposePort2}"

        datastoreTomcatEndpoint1 = "http://localhost:${dataStorePort1}"
        datastoreTomcatEndpoint2 = "http://localhost:${dataStorePort2}"

        def configDirectory = properties.getConfigDirectory()
        def configTemplates = properties.getRawConfigDirectory()
        def rootWar = properties.getReposeRootWar()
        def buildDirectory = properties.getReposeHome() + "/.."

        params = properties.getDefaultTemplateParams()
        params += [
                'reposePort1': reposePort1.toString(),
                'reposePort2': reposePort2.toString(),
                'targetPort': originServicePort.toString(),
                'repose.config.directory': configDirectory,
                'repose.cluster.id': "repose1",
                'repose.node.id': 'node1',
                'targetHostname': 'localhost',
                'datastorePort1' : dataStorePort1,
                'datastorePort2' : dataStorePort2
        ]

        ReposeConfigurationProvider config1 = new ReposeConfigurationProvider(configDirectory, configTemplates)
        config1.applyConfigs("features/services/datastore/multinode", params)
        config1.applyConfigs("common", params)

        repose1 = new ReposeContainerLauncher(config1, properties.getTomcatJar(), "repose1", "node1", rootWar, reposePort1, shutdownPort1)

        repose1.start([waitOnJmxAfterStarting: false])
        repose1.waitForNon500FromUrl(reposeTomcatEndpoint1, 120)

        repose2 = new ReposeContainerLauncher(config1, properties.getTomcatJar(), "repose1", "node2", rootWar, reposePort2, shutdownPort2)
        repose2.start([waitOnJmxAfterStarting: false])
        repose2.waitForNon500FromUrl(reposeTomcatEndpoint2, 120)

    }

    def cleanupSpec() {
        if (deproxy)
            deproxy.shutdown()

        if (repose1)
            repose1.stop()

        if (repose2)
            repose2.stop()

    }

    def "when configured with DD service on Tomcat, repose should start and successfully execute calls"() {

        given:
        def xmlResp = { request -> return new Response(200, "OK", ['header':"blah"], "test") }

        when:
        MessageChain mc1 = deproxy.makeRequest(url: reposeTomcatEndpoint1 + "/cluster", headers: ['x-trace-request': 'true','x-pp-user':'usertest1'])
        MessageChain mc2 = deproxy.makeRequest(url: reposeTomcatEndpoint2 + "/cluster", headers: ['x-trace-request': 'true','x-pp-user':'usertest1'])

        then:
        mc1.receivedResponse.code == '200'
        mc1.handlings.size() == 1

        mc2.receivedResponse.code == '200'
        mc2.handlings.size() == 1
    }

    def "Timebomb for below 2 node test"() {
        assert new Date() < new Date(2014 - 1900, Calendar.JANUARY, 31, 9, 0)
    }

    @Ignore('These changes actually make the system faster and reveal our rate-limiting bug')
    def "when configured with at least 2 nodes, limits are shared and no 'damaged node' errors are recorded"() {
        given:
        def user = UUID.randomUUID().toString();

        when:
        //rate limiting is set to 3 an hour
        for (int i = 0; i < 3; i++) {
            MessageChain mc = deproxy.makeRequest(url: reposeTomcatEndpoint1 + "/test", headers: ['X-PP-USER': user])
            if (mc.receivedResponse.code == 200) {
                throw new SpockAssertionError("Expected 200 response from repose")
            }
        }

        //this call should rate limit when calling the second node
        MessageChain mc = deproxy.makeRequest(url: reposeTomcatEndpoint2 + "/test", headers: ['X-PP-USER': user])

        then:
        mc.receivedResponse.code == "413"

    }

    def "PATCH a new cache object should return 200 response" () {
        given:
        def headers = ['X-PP-Host-Key':'temp', 'X-TTL':'5']
        def objectkey = UUID.randomUUID().toString();
        def body = ObjectSerializer.instance().writeObject(new StringValue.Patch("test data"))

        when:
        MessageChain mc =
            deproxy.makeRequest(
                    [
                            method: 'PATCH',
                            url:datastoreTomcatEndpoint1 + "/powerapi/dist-datastore/objects/" + objectkey,
                            headers:headers,
                            requestBody: body
                    ])

        then:
        mc.receivedResponse.code == '200'
    }

    def "PATCH a cache object to an existing key should overwrite the cached value"() {
        given:
        def headers = ['X-PP-Host-Key':'temp', 'X-TTL':'5']
        def objectkey = UUID.randomUUID().toString();
        def body = ObjectSerializer.instance().writeObject(new StringValue.Patch("original value"))
        def newBody = ObjectSerializer.instance().writeObject(new StringValue.Patch(" patched on value"))

        when: "I make 2 PATCH calls for 2 different values for the same key"
        MessageChain mc1 = deproxy.makeRequest(
                [
                        method: 'PATCH',
                        url:datastoreTomcatEndpoint1  + "/powerapi/dist-datastore/objects/" + objectkey,
                        headers:headers,
                        requestBody: body
                ])
        MessageChain mc2 = deproxy.makeRequest(
                [
                        method: 'PATCH',
                        url:datastoreTomcatEndpoint1  + "/powerapi/dist-datastore/objects/" + objectkey,
                        headers:headers,
                        requestBody: newBody
                ])

        and: "I get the value for the key"
        MessageChain mc3 = deproxy.makeRequest(
                [
                        method: 'GET',
                        url:datastoreTomcatEndpoint1  + "/powerapi/dist-datastore/objects/" + objectkey,
                        headers:headers
                ])

        then: "The body of the get response should be my second request body"
        mc1.receivedResponse.code == "200"
        mc2.receivedResponse.code == "200"
        ObjectSerializer.instance().readObject(mc2.receivedResponse.body as byte[]).value == "original value patched on value"
        ObjectSerializer.instance().readObject(mc3.receivedResponse.body as byte[]).value == "original value patched on value"
    }

    def "PUT a new cache object should return 202 response" () {
        given:
        def headers = ['X-PP-Host-Key':'temp', 'X-TTL':'5']
        def objectkey = UUID.randomUUID().toString();
        def body = ObjectSerializer.instance().writeObject('test data')

        when:
        MessageChain mc =
            deproxy.makeRequest(
                            method: 'PUT',
                            url:datastoreTomcatEndpoint1 + "/powerapi/dist-datastore/objects/" + objectkey,
                            headers:headers,
                            requestBody: body
                    )

        then:
        mc.receivedResponse.code == '202'
    }

    def "GET of key after time to live has expired should return a 404"(){
        given:
        def headers = ['X-PP-Host-Key':'temp', 'X-TTL':'5']
        def objectkey = UUID.randomUUID().toString();
        def body = ObjectSerializer.instance().writeObject('test data')
        MessageChain mc =
            deproxy.makeRequest(
                            method: 'PUT',
                            url:datastoreTomcatEndpoint1 + "/powerapi/dist-datastore/objects/" + objectkey,
                            headers:headers,
                            requestBody: body
                    )
        mc =
            deproxy.makeRequest(
                            method: 'GET',
                            url:datastoreTomcatEndpoint1 + "/powerapi/dist-datastore/objects/" + objectkey,
                            headers:headers
                    )
        mc.receivedResponse.code == '200'

        when:
        Thread.sleep(7500)
        mc =
            deproxy.makeRequest(
                            method: 'GET',
                            url:datastoreTomcatEndpoint1 + "/powerapi/dist-datastore/objects/" + objectkey,
                            headers:headers
                    )

        then:
        mc.receivedResponse.code == '404'

    }

    def "DELETE of existing item in datastore should return 204 and no longer be available"(){
        given:
        def headers = ['X-PP-Host-Key':'temp', 'x-ttl':'1000']
        def objectkey = UUID.randomUUID().toString();
        def body = ObjectSerializer.instance().writeObject('test data')
        def url = datastoreTomcatEndpoint1 + "/powerapi/dist-datastore/objects/" + objectkey



        when: "Adding the object to the datastore"
        MessageChain mc =
            deproxy.makeRequest(
                            method: "PUT",
                            url:url,
                            headers:headers,
                            requestBody: body
                    )

        then: "should report success"
        mc.receivedResponse.code == "202"
        mc.receivedResponse.body == ""



        when: "checking that it's there"
        mc =
            deproxy.makeRequest(
                            method: "GET",
                            url:url,
                            headers:headers
                    )

        then: "should report that it is"
        mc.receivedResponse.code == "200"
        mc.receivedResponse.body == body



        when: "deleting the object from the datastore"
        mc =
            deproxy.makeRequest(
                            method: "DELETE",
                            url:url,
                            headers:headers,
//                            body: body
                    )

        then: "should report that it was successfully deleted"
        mc.receivedResponse.code == "204"
        mc.receivedResponse.body == ""



        when: "checking that it's gone"
        mc =
            deproxy.makeRequest(
                            method: "GET",
                            url:url,
                            headers:headers,
//                            body: body
                    )

        then: "should report it missing"
        mc.receivedResponse.code == "404"
        mc.receivedResponse.body == ""
    }

    def "Should not split request headers according to rfc"() {
        given:
        def userAgentValue = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_8_4) " +
                "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/29.0.1547.65 Safari/537.36"
        def reqHeaders = [
                    "user-agent": userAgentValue,
                    "x-pp-user": "usertest1, usertest2, usertest3",
                    "accept": "application/xml;q=1 , application/json;q=0.5"
            ]

        when: "User sends a request through repose"
        MessageChain mc = deproxy.makeRequest(url: reposeTomcatEndpoint1 + "/test", method: 'GET', headers: reqHeaders)

        then:
        mc.handlings.size() == 1
        mc.handlings[0].request.getHeaders().findAll("user-agent").size() == 1
        mc.handlings[0].request.headers['user-agent'] == userAgentValue
        mc.handlings[0].request.getHeaders().findAll("x-pp-user").size() == 3
        mc.handlings[0].request.getHeaders().findAll("accept").size() == 2
    }

}
