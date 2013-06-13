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
    def clock = new SystemClock()
    def server

    JmxClient(String jmxUrl) {
        this.jmxUrl = jmxUrl
        server = JMXConnectorFactory.connect(new JMXServiceURL(jmxUrl)).MBeanServerConnection

    }

    /**
     * Looks for a particular mbean & attribute by JMX name and returns it.
     *
     *
     * @param name - complete MBean name, to be passed into ObjectName
     * @return
     */
    def getMBeanAttribute( name, attr ) {

        def obj
        println( "looking up mbean attribute" )

        try {
            waitForCondition( clock, '25s', '1s', {
                obj = server.getAttribute( new ObjectName( name ), attr )
                obj != null
            })
        } catch (TimeoutException) {
            // ignore this and simply return the total mbeans found
            println( "failed to find expected mbean attribute" )
        }

        println( "found mbean attribute" )

        obj
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
        println("looking up mbeans")

        try {
            waitForCondition(clock, '25s', '1s', {
                def beansInDomain = server.queryMBeans(new ObjectName(domain), null)
                mbeans = beansInDomain.findAll { it.className == expectedClassName }
                mbeans.size() == expectedCount
            })
        } catch (TimeoutException) {
            // ignore this and simply return the total mbeans found
            println("failed to find total expected mbeans")
        }
        println("found " + mbeans.size() + " mbeans")

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
