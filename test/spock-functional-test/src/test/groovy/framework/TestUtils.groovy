package framework

import org.linkedin.util.clock.SystemClock
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain

import static org.linkedin.groovy.util.concurrent.GroovyConcurrentUtils.waitForCondition

class TestUtils {

    def static waitUntilReadyToServiceRequests(String reposeEndpoint, String responseCode="200") {
        def clock = new SystemClock()
        def innerDeproxy = new Deproxy()
        MessageChain mc
        waitForCondition(clock, '60s', '1s', {
            try {
                mc = innerDeproxy.makeRequest([url: reposeEndpoint])
            } catch (Exception e) {}
            if (mc != null) {
                if (mc.receivedResponse.code.equalsIgnoreCase(responseCode)) {
                    return true
                }
            } else {
                return false
            }
        })
    }

}
