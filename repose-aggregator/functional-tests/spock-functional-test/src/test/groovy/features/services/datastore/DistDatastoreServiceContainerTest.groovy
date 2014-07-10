package features.services.datastore

import com.rackspace.papi.commons.util.io.ObjectSerializer
import com.rackspace.papi.components.datastore.StringValue
import framework.ReposeConfigurationProvider
import framework.ReposeContainerLauncher
import framework.ReposeLauncher
import framework.ReposeValveTest
import framework.TestProperties
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain
import org.rackspace.deproxy.PortFinder
import org.rackspace.deproxy.Response
import spock.lang.Specification
import spock.lang.Unroll

/**
 * Created by jennyvo on 7/10/14.
 * Test the Distributed Datastore Service in 2 multinode containers
 */
class DistDatastoreServiceContainerTest extends ReposeValveTest {
    static def datastoreEndpoint1
    static def datastoreEndpoint2
    static def reposeEndpoint1
    static def reposeEndpoint2
    static def params

    static ReposeLauncher repose1
    static ReposeLauncher repose2

    static ReposeConfigurationProvider config
    static int reposePort1
    static int reposePort2
    static int dataStorePort1
    static int dataStorePort2

    def setupSpec() {

        reposePort1 = properties.reposePort
        reposePort2 = PortFinder.Singleton.getNextOpenPort()
        dataStorePort1 = PortFinder.Singleton.getNextOpenPort()
        dataStorePort2 = PortFinder.Singleton.getNextOpenPort()

        reposeEndpoint1 = "http://localhost:${reposePort1}"
        reposeEndpoint2 = "http://localhost:${reposePort2}"

        datastoreEndpoint1 = "http://localhost:${dataStorePort1}"
        datastoreEndpoint2 = "http://localhost:${dataStorePort2}"

        def configDirectory = properties.getConfigDirectory()
        def configTemplates = properties.getRawConfigDirectory()

        params = properties.getDefaultTemplateParams()
        params += [
                'reposePort1': reposePort1,
                'reposePort2': reposePort2,
                'repose.cluster.id': "repose1",
                'repose.node.id': 'node1',
                'datastorePort1' : dataStorePort1,
                'datastorePort2' : dataStorePort2
        ]
        config = new ReposeConfigurationProvider(configDirectory, configTemplates)
        config.applyConfigs("features/services/datastore/multinode", params)
        config.applyConfigs("common", params)
    }
    @Unroll("When start repose container #containerName")
    def "Test repose container with multi-nodes"() {
        given:
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort)
        def rootWar = properties.getReposeRootWar()

        repose1 = new ReposeContainerLauncher(config, serviceContainer, "repose1", "node1", rootWar, reposePort1)
        repose1.start()
        repose1.waitForNon500FromUrl(reposeEndpoint1, 120)
        repose1.waitForNon500FromUrl(datastoreEndpoint1, 120)

        repose2 = new ReposeContainerLauncher(config, serviceContainer, "repose1", "node2", rootWar, reposePort2)
        repose2.start()
        repose2.waitForNon500FromUrl(reposeEndpoint2, 120)
        repose2.waitForNon500FromUrl(datastoreEndpoint2, 120)

        repose1.waitForNon500FromUrl(reposeEndpoint1, 120)
        repose2.waitForNon500FromUrl(reposeEndpoint2, 120)

        def headers = ['X-PP-Host-Key':'temp', 'X-TTL':'5']
        def objectkey = UUID.randomUUID().toString();
        def body = ObjectSerializer.instance().writeObject(new StringValue.Patch("test data"))
        def strurl = datastoreEndpoint1 + "/powerapi/dist-datastore/objects/" + objectkey

        when: "Send a simple request"
        def xmlResp = { request -> return new Response(200, "OK", ['header':"blah"], "test") }
        MessageChain mc1 = deproxy.makeRequest(url: reposeEndpoint1 + "/cluster", headers: ['x-trace-request': 'true','x-pp-user':'usertest1'])
        MessageChain mc2 = deproxy.makeRequest(url: reposeEndpoint2 + "/cluster", headers: ['x-trace-request': 'true','x-pp-user':'usertest1'])

        then: "Repose should successful execute"
        mc1.receivedResponse.code == '200'
        mc1.handlings.size() == 1
        mc2.receivedResponse.code == '200'
        mc2.handlings.size() == 1

        when: "Send a PATCH request"
        MessageChain mc = deproxy.makeRequest(
                        [
                                method: 'PATCH',
                                url: strurl,
                                headers:headers,
                                requestBody: body
                        ])
        then:
        mc.receivedResponse.code == '200'

        when: "PUT a new cache object should return 202 response"
        body = ObjectSerializer.instance().writeObject('test data PUT GET DELETE')
        mc = deproxy.makeRequest(
                        method: 'PUT',
                        url: strurl,
                        headers:headers,
                        requestBody: body
                )

        then:
        mc.receivedResponse.code == '202'

        when: "GET check if item available"
        mc = deproxy.makeRequest(
                method: 'GET',
                url: strurl,
                headers:headers
        )

        then: "should report that it is"
        mc.receivedResponse.code == "200"
        mc.receivedResponse.body == body

        when: "DELETE of existing item in datastore should return 202 and no longer be available"
        mc = deproxy.makeRequest(
                method: "DELETE",
                url:strurl,
                headers:headers,
                )

        then: "should report that it was successfully deleted"
        mc.receivedResponse.code == "204"
        mc.receivedResponse.body == ""

        when: "GET after DELETE check if item no longer available"
        mc = deproxy.makeRequest(
                method: 'GET',
                url: strurl,
                headers:headers
        )

        then: "should report not found"
        mc.receivedResponse.code == "404"
        mc.receivedResponse.body == ""

        when: "GET of key after time to live has expired should return a 404"
        mc = deproxy.makeRequest(
                        method: 'PUT',
                        url: strurl,
                        headers:headers,
                        requestBody: body
                )
        mc = deproxy.makeRequest(
                        method: 'GET',
                        url: strurl,
                        headers:headers
                )
        mc.receivedResponse.code == '200'
        Thread.sleep(7500)
        mc = deproxy.makeRequest(
                        method: 'GET',
                        url: strurl,
                        headers:headers
                )

        then:
        mc.receivedResponse.code == '404'

        when: "PATCH a cache object to an existing key should overwrite the cached value"
        body = ObjectSerializer.instance().writeObject(new StringValue.Patch("original value"))
        def newBody = ObjectSerializer.instance().writeObject(new StringValue.Patch(" patched on value"))
        mc1 = deproxy.makeRequest(
                [
                        method: 'PATCH',
                        url:strurl,
                        headers:headers,
                        requestBody: body
                ])

        mc2 = deproxy.makeRequest(
                [
                        method: 'PATCH',
                        url: strurl,
                        headers:headers,
                        requestBody: newBody
                ])

        and: "I get the value for the key"
        MessageChain mc3 = deproxy.makeRequest(
                [
                        method: 'GET',
                        url: strurl,
                        headers:headers
                ])

        then:"The body of the get response should be the patched value"
        mc1.receivedResponse.code == "200"
        mc2.receivedResponse.code == "200"
        ObjectSerializer.instance().readObject(mc2.receivedResponse.body as byte[]).value == "original value patched on value"
        ObjectSerializer.instance().readObject(mc3.receivedResponse.body as byte[]).value == "original value patched on value"

        //additional test
        when: "User send request to repose hould not split request headers according to rfc"
        def userAgentValue = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_8_4) " +
                "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/29.0.1547.65 Safari/537.36"
        def reqHeaders = [
                "user-agent": userAgentValue,
                "x-pp-user": "usertest1, usertest2, usertest3",
                "accept": "application/xml;q=1 , application/json;q=0.5"
        ]
        mc = deproxy.makeRequest(url: reposeEndpoint1 + "/test", method: 'GET', headers: reqHeaders)

        then:
        mc.handlings.size() == 1
        mc.handlings[0].request.getHeaders().findAll("user-agent").size() == 1
        mc.handlings[0].request.headers['user-agent'] == userAgentValue
        mc.handlings[0].request.getHeaders().findAll("x-pp-user").size() == 3
        mc.handlings[0].request.getHeaders().findAll("accept").size() == 2

        where:
        containerName       | serviceContainer
        "Tomcat"            | properties.getTomcatJar()
        "GlassFist"         | properties.getGlassfishJar()
    }

    def cleanup() {
        if (deproxy)
            deproxy.shutdown()

        if (repose1)
            repose1.stop()

        if (repose2)
            repose2.stop()
    }
}
