package features.core.powerfilter

import framework.ReposeValveTest
import org.json.JSONArray
import org.json.JSONTokener
import org.json.JSONObject
import org.rackspace.gdeproxy.Deproxy
import org.rackspace.gdeproxy.MessageChain

/**
 * Created with IntelliJ IDEA.
 * User: dimi5963
 */
class GraphiteTest extends ReposeValveTest {
    String GRAPHITE_SERVER = "http://graphite.staging.ord1.us.ci.rackspace.net/render?target=test.1.metrics.repose-node1-com.rackspace.papi.ResponseCode.Repose.2XX.count&format=json&from=-1min"
    String PREFIX = "\"repose-node1-com.rackspace.papi\":type=\"ResponseCode\",scope=\""

    String NAME_2XX = "\",name=\"2XX\""
    String ALL_2XX = PREFIX + "All Endpoints" + NAME_2XX
    String REPOSE_2XX = PREFIX + "Repose" + NAME_2XX

    def setup() {
        repose.applyConfigs("features/core/powerfilter/graphite")
        repose.start()

        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.getProperty("target.port").toInteger())
    }

    def cleanup() {
        if (deproxy)
            deproxy.shutdown()
        repose.stop()
    }

    def "when sending requests, data should be logged to graphite"() {
        given:
        def responses = []

        when:
        responses.add(deproxy.makeRequest(reposeEndpoint + "/endpoint"))
        responses.add(deproxy.makeRequest(reposeEndpoint + "/endpoint"))
        responses.add(deproxy.makeRequest(reposeEndpoint + "/cluster"))

        then:
        boolean isFound = false
        InputStream is = new URI(GRAPHITE_SERVER).toURL().openStream()
        try {
            BufferedReader rd = new BufferedReader(new InputStreamReader(is))
            String jsonText = rd.readLine()
            JSONArray results = new JSONArray(new JSONTokener(jsonText))
            results != null
            for(int i = 0; i < results.length(); i ++){
                JSONArray datapoints = results.get(i).getJSONArray("datapoints")
                datapoints != null
                for(JSONArray datapoint : datapoints){
                    if(datapoint.getString(0) != "null"){
                        datapoint.getString(0).equals("3.0")
                        isFound = true
                        break
                    }
                }
            }
        } finally {
            is.close()
        }

        isFound == true

        responses.each { MessageChain mc ->
            assert(mc.receivedResponse.code == "200")
        }
    }

}
