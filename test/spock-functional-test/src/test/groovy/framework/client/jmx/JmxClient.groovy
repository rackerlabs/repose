package framework.client.jmx

import javax.management.ObjectName
import javax.management.remote.JMXConnectorFactory
import javax.management.remote.JMXServiceURL

/**
 * TODO: Add comments here
 */
class JmxClient {

    def String jmxUrl

    JmxClient(String jmxUrl) {
        this.jmxUrl = jmxUrl
    }

    def GroovyMBean getMBean(String beanName) {
        def server = JMXConnectorFactory.connect(new JMXServiceURL(jmxUrl)).MBeanServerConnection

        def query = new ObjectName("repose*")


        server.queryMBeans(query)

        def foundBean = new GroovyMBean(server, beanName)
        foundBean
    }

}
