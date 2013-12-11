package framework

import framework.client.jmx.JmxClient
import org.linkedin.util.clock.SystemClock

import static org.junit.Assert.fail
import static org.linkedin.groovy.util.concurrent.GroovyConcurrentUtils.waitForCondition
import static org.linkedin.groovy.util.concurrent.GroovyConcurrentUtils.waitForCondition
import static org.linkedin.groovy.util.concurrent.GroovyConcurrentUtils.waitForCondition
import static org.linkedin.groovy.util.concurrent.GroovyConcurrentUtils.waitForCondition


abstract class AbstractReposeLauncher implements ReposeLauncher {

    def boolean debugEnabled
    def String connFramework = "jersey"

    def reposeEndpoint
    def int reposePort

    def JmxClient jmx
    def classPaths =[]

    def ReposeConfigurationProvider configurationProvider

    @Override
    void applyConfigs(String[] configLocations) {
        configurationProvider.applyConfigs(configLocations)
    }

    @Override
    void updateConfigs(String[] configLocations) {
        configurationProvider.updateConfigs(configLocations)
    }


    def nextAvailablePort() {

        def socket
        int port
        try {
            socket = new ServerSocket(0);
            port = socket.getLocalPort()
        } catch (IOException e) {
            fail("Failed to find an open port")
        } finally {
            if (socket != null && !socket.isClosed()) {
                socket.close()
            }
        }
        return port
    }

    def connectViaJmxRemote(jmxUrl) {
        try {
            jmx = new JmxClient(jmxUrl)
            return true
        } catch (Exception ex) {
            return false
        }
    }

    @Override
    void enableDebug() {
        this.debugEnabled = true
    }

    @Override
    void addToClassPath(String path){
        classPaths.add(path)
    }

    /**
     * TODO: introspect the system model for expected filters in filter chain and validate that they
     * are all present and accounted for
     * @return
     */
    private boolean isFilterChainInitialized() {
        print('.')

        // First query for the mbean.  The name of the mbean is partially configurable, so search for a match.
        def HashSet cfgBean = jmx.getMBeans("*com.rackspace.papi.jmx:type=ConfigurationInformation")
        if (cfgBean == null || cfgBean.isEmpty()) {
            return false
        }

        def String beanName = cfgBean.iterator().next().name.toString()

        def ArrayList filterchain = jmx.getMBeanAttribute(beanName, "FilterChain")


        if (filterchain == null || filterchain.size() == 0) {
            return beanName.contains("nofilters")
        }

        def initialized = true

        filterchain.each { data ->
            if (data."successfully initialized" == false) {
                initialized = false
            }
        }

        return initialized

    }

}
