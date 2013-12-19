package features.filters.datastore

import framework.ReposeValveTest
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain

class DistDatastoreUnAuthFilterTest extends ReposeValveTest {

    String DD_URI
    def DD_HEADERS = ['X-PP-Host-Key':'temp', 'X-TTL':'5000']
    def BODY = "test body"
    static def KEY
    def DD_PATH = "/powerapi/dist-datastore/objects/"
    def URL

    def host = { host ->
        Enumeration e=NetworkInterface.getNetworkInterfaces();
        while(e.hasMoreElements())
        {
            NetworkInterface n=(NetworkInterface) e.nextElement();
            Enumeration ee = n.getInetAddresses();
            while(ee.hasMoreElements())
            {
                InetAddress i= (InetAddress) ee.nextElement();
                if(i.getHostAddress().startsWith("10") || i.getHostAddress().startsWith("192"))
                    return i.getHostAddress();
            }
        }

        throw new Exception("cannot find valid address")
    }

    def tempEndpoint = reposeEndpoint.replaceAll("localhost",host)

    def setup() {
        DD_URI = reposeEndpoint + DD_PATH
        KEY = UUID.randomUUID().toString()
        URL = tempEndpoint + DD_PATH + KEY

    }

    def setupSpec() {
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.getProperty("target.port").toInteger())

        repose.applyConfigs("features/filters/datastore/authed/")
        repose.start()
        waitUntilReadyToServiceRequests()
    }

    def cleanupSpec() {
        repose.stop()
        deproxy.shutdown()
    }

    def "PUT with 10. or 192. should return a 403 UNAUTHORIZED when allowedHosts only accepts local calls" () {
        given: "An endpoint with 10. or 192. that will not be resolved in DD Filter as localhost"
        def url = tempEndpoint + DD_PATH + KEY

        when:
        MessageChain mc = deproxy.makeRequest([method: 'PUT', url: url, headers:DD_HEADERS, requestBody: BODY])

        then:
        mc.receivedResponse.code == '403'
    }

    def "GET with 10. or 192. should return a 403 UNAUTHORIZED when allowedHosts only accepts local calls" () {

        given: "An endpoint with 10. or 192. that will not be resolved in DD Filter as localhost"
        def endpointThatWontResolveToLocalhost = tempEndpoint + DD_PATH + KEY

        when: "I PUT a value for the key"
        MessageChain mc = deproxy.makeRequest([method: 'PUT', url: DD_URI + KEY, headers:DD_HEADERS, requestBody: BODY])

        then:
        mc.receivedResponse.code == '202'

        when:
        mc = deproxy.makeRequest([method: 'GET', url: endpointThatWontResolveToLocalhost, headers:DD_HEADERS])

        then:
        mc.receivedResponse.code == '403'

    }

    def "DELETE with 10. or 192. should return a 403 UNAUTHORIZED when allowedHosts only accepts local calls" () {

        given: "An endpoint with 10. or 192. that will not be resolved in DD Filter as localhost"
        def endpointThatWontResolveToLocalhost = tempEndpoint + DD_PATH + KEY

        when: "I PUT a value for the key"
        MessageChain mc = deproxy.makeRequest([method: 'PUT', url: DD_URI + KEY, headers:DD_HEADERS, requestBody: BODY])

        then:
        mc.receivedResponse.code == '202'

        when:
        mc = deproxy.makeRequest([method: "DELETE", url:endpointThatWontResolveToLocalhost, headers:DD_HEADERS])

        then:
        mc.receivedResponse.code == '403'
    }
}