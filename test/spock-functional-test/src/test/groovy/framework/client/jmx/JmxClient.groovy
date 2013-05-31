package framework.client.jmx

import org.linkedin.util.clock.SystemClock

import javax.management.ObjectName
import javax.management.remote.JMXConnectorFactory
import javax.management.remote.JMXServiceURL

import static org.linkedin.groovy.util.concurrent.GroovyConcurrentUtils.waitForCondition

/**
 * TODO: Add comments here
 */
class JmxClient {

    def String jmxUrl

    JmxClient(String jmxUrl) {
        this.jmxUrl = jmxUrl
    }

    /**
     * Connects via JMX to a Java Application and queries all MBeans matching the provided beanName
     *
     * Conditional wait allows for some latency between time of request and MBeans being available in JMX
     *
     * @param beanName
     * @return
     */
    def getMBean(String beanName) {

        def server = JMXConnectorFactory.connect(new JMXServiceURL(jmxUrl)).MBeanServerConnection
        server.queryMBeans(new ObjectName(beanName), null)
    }

    /**
     * Accounting for some test flakiness caused by some latency in MBeans being available to JMX clients.
     *
     * Either this is due to async processing with the JMX mbean registration, or there is some latency
     * with an MBean being visible to a client.
     *
     */
    def verifyMBeanCount(domain, expectedClassName, expectedCount) {

        def totalFound

        def clock = new SystemClock()
        try {
            waitForCondition(clock, '15s', '1s', {
                def mbeans = getMBean(domain)
                totalFound = 0
                mbeans.each {
                    if (it.className == expectedClassName)
                        totalFound++
                }
                totalFound == expectedCount
            })
        } catch (TimeoutException) {
            // ignore this and simply return the total mbeans found
        }
        totalFound
    }

}
