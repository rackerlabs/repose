package framework.client.jmx

import org.linkedin.util.clock.SystemClock

import javax.management.ObjectName
import javax.management.remote.JMXConnectorFactory
import javax.management.remote.JMXServiceURL

import static org.linkedin.groovy.util.concurrent.GroovyConcurrentUtils.waitForCondition

/**
 * Simple JMX client
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
    def findMBeanByName(String beanName) {

        def server = JMXConnectorFactory.connect(new JMXServiceURL(jmxUrl)).MBeanServerConnection
        server.queryMBeans(new ObjectName(beanName), null)
    }

    /**
     * Connects via JMX to a Java Application and queries all MBeans matching the provided beanName
     *
     * Conditional wait allows for some latency between time of request and MBeans being available in JMX
     *
     * @param beanName
     * @return
     */
    def getMBeans(domain, expectedClassName, expectedCount) {

        def mbeans

        def clock = new SystemClock()
        try {
            waitForCondition(clock, '30s', '1s', {
                def beansInDomain = findMBeanByName(domain)
                mbeans = beansInDomain.findAll { it.className == expectedClassName }
                mbeans.size() == expectedCount
            })
        } catch (TimeoutException) {
            // ignore this and simply return the total mbeans found
        }
        mbeans
    }

    /**
     * Accounting for some test flakiness caused by some latency in MBeans being available to JMX clients.
     *
     * Either this is due to async processing with the JMX mbean registration, or there is some latency
     * with an MBean being visible to a client.
     *
     */
    def getMBeanCount(domain, expectedClassName, expectedCount) {
        def Set mbeans = getMBeans(domain, expectedClassName, expectedCount)
        mbeans.size()
    }



}
