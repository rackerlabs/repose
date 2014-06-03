package framework

import org.linkedin.util.clock.SystemClock
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain

import java.util.concurrent.TimeUnit

import static org.linkedin.groovy.util.concurrent.GroovyConcurrentUtils.waitForCondition

class TestUtils {

    def static String getJvmProcesses() {
        def runningJvms = "jps -v".execute()
        runningJvms.waitFor()

        return runningJvms.in.text
    }

    /**
     * Takes a boolean closure that will indicate whatever it's looking for
     * If the timeout hits, it's going to fail via throwing an exception
     * This could probably be reused in many places.
     *
     * Additionally, you can pass in an optional TimeUnit, it operates in seconds right now.
     * it will wait 500ms between iterations right now.
     * @param timeout
     * @param timeUnit
     * @param block
     * @return
     */
    static def timedSearch(int timeout, TimeUnit timeUnit = TimeUnit.SECONDS, Closure block) {
        def startTime = System.currentTimeMillis()
        boolean foundIt = false

        while(System.currentTimeMillis() < startTime + timeUnit.toMillis(timeout) && !foundIt) {
            foundIt = block.call()
            Thread.sleep(500)
        }

        if(!foundIt) {
            throw new Exception("Unable to satisfy condition within ${timeout} seconds")
        }
        foundIt
    }


}
