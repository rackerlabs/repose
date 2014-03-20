package framework

import org.linkedin.util.clock.SystemClock
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain

import static org.linkedin.groovy.util.concurrent.GroovyConcurrentUtils.waitForCondition

class TestUtils {

    def static String getJvmProcesses() {
        def runningJvms = "jps -v".execute()
        runningJvms.waitFor()

        return runningJvms.in.text
    }
}
