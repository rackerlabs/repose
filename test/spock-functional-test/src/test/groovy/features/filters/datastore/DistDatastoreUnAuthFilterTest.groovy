package features.filters.datastore

import framework.ReposeValveTest
import org.rackspace.gdeproxy.Deproxy
import org.rackspace.gdeproxy.MessageChain

/**
 * User: dimi5963
 * Date: 9/9/13
 * Time: 10:55 AM
 */
class DistDatastoreUnAuthFilterTest extends ReposeValveTest {

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

    def setupSpec() {
        repose.applyConfigs(
                "features/filters/datastore/authed/"
        )
        repose.start()
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.getProperty("target.port").toInteger())

    }

    def cleanupSpec() {
        repose.stop()
        deproxy.shutdown()
    }

    def "when putting cache objects" () {
        given:
        def headers = ['X-PP-Host-Key':'temp', 'x-ttl':'5000']
        def objectkey = '8e969a44-990b-de49-d894-cf200b7d4c11'
        def body = "test data"

        when:

        def url = tempEndpoint + "/powerapi/dist-datastore/objects/" + objectkey
        MessageChain mc =
            deproxy.makeRequest(
                    [
                            method: 'PUT',
                            url: url,
                            headers:headers,
                            requestBody: body
                    ])

        then:
        mc.receivedResponse.code == '403'
    }

    def "when checking cache object time to live"(){
        given:
        def headers = ['X-PP-Host-Key':'temp', 'x-ttl':'2']
        def objectkey = '8e969a44-990b-de49-d894-cf200b7d4c11'
        def body = "test data"
        MessageChain mc =
            deproxy.makeRequest(
                    [
                            method: 'PUT',
                            url:tempEndpoint + "/powerapi/dist-datastore/objects/" + objectkey,
                            headers:headers,
                            requestBody: body
                    ])

        when:
        Thread.sleep(2500)
        mc =
            deproxy.makeRequest(
                    [
                            method: 'GET',
                            url:tempEndpoint + "/powerapi/dist-datastore/objects/" + objectkey,
                            headers:headers
                    ])

        then:
        mc.receivedResponse.code == '403'

    }

    def "when deleting cache objects"(){
        given:
        def headers = ['X-PP-Host-Key':'temp', 'x-ttl':'50']
        def objectkey = '8e969a44-990b-de49-d894-cf200b7d4c11'
        def body = "test data"

        when:
        MessageChain mc =
            deproxy.makeRequest(
                    [
                            method: "DELETE",
                            url:tempEndpoint + "/powerapi/dist-datastore/objects/" + objectkey,
                            headers:headers
                    ])

        then:
        mc.receivedResponse.code == '403'
    }
}