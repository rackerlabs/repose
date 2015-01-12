package framework.client.jmx

import org.linkedin.util.clock.SystemClock
import org.spockframework.runtime.SpockAssertionError
import org.spockframework.runtime.SpockTimeoutError
import spock.util.concurrent.PollingConditions

import javax.management.MBeanServerConnection
import javax.management.ObjectName
import javax.management.remote.JMXConnectorFactory
import javax.management.remote.JMXServiceURL

/**
 * Deceptively Simple JMX client
 */
class JmxClient {

    def String jmxUrl
    def clock = new SystemClock()
    MBeanServerConnection server

    JmxClient(String jmxUrl) {
        this.jmxUrl = jmxUrl
        server = JMXConnectorFactory.connect(new JMXServiceURL(jmxUrl)).MBeanServerConnection

    }

    /**
     * Takes a closure of code to evaluate, and wraps it with a bit more context around why JMX might timeout
     * TODO: this should go into a test library
     * @param tehCode TEH CODE
     * @return
     */
    static def eventually(Closure tehCode) {
        def conditions = new PollingConditions(timeout: 25, initialDelay: 0, delay: 0.1)
        Exception whyFail = null

        try {
            conditions.eventually {
                try {
                    tehCode.call()
                } catch (Exception e) {
                    //Don't actually care, because eventually a thingy
                    whyFail = e
                    assert false
                }
            }
        } catch (SpockTimeoutError ste) {
            throw new SpockAssertionError(ste.getMessage(), whyFail)
        }

    }

    /**
     * Looks for a particular mbean & attribute by JMX name and returns it.
     *
     * @param name - complete MBean name, to be passed into ObjectName
     * @return
     */
    def getMBeanAttribute(name, attr) {
        def obj = null
        eventually {
            obj = server.getAttribute(new ObjectName(name), attr)
            assert obj != null
        }
        return obj
    }


    /**
     * Connects via JMX to a Java Application and queries all MBeans matching the provided beanName
     *
     * @param beanName
     * @return
     */
    def getMBeans(domain, expectedClassName, expectedCount) {
        def mbeans = null

        eventually {
            def beansInDomain = server.queryMBeans(new ObjectName(domain), null)
            mbeans = beansInDomain.findAll { it.className == expectedClassName }
            assert mbeans.size() == expectedCount
        }

        return mbeans
    }
    /**
     * Connects via JMX to a Java Application and queries all MBeans matching the provided beanName
     *
     * @param beanName
     * @return
     */
    def getMBeans(domain) {
        def mbeans = null
        eventually {
            mbeans = server.queryMBeans(new ObjectName(domain), null)
            assert mbeans != null && mbeans.size() >= 1
        }
        return mbeans
    }

    /**
     * Connects via JMX to a Java Application and queries all MBeans matching the provided beanName
     *
     * @param beanName
     * @return
     */
    def getMBeanNames(domain) {
        def mbeans

        eventually {
            mbeans = server.queryNames(new ObjectName(domain), null)
            assert mbeans != null && mbeans.size() >= 1
        }

        return mbeans
    }
}
