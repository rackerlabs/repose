package framework
import framework.client.jmx.JmxClient
import org.linkedin.util.clock.SystemClock

import static org.linkedin.groovy.util.concurrent.GroovyConcurrentUtils.waitForCondition

class ReposeValveLauncher implements ReposeLauncher {

    def boolean debugEnabled
    def String reposeJar
    def String configDir

    def clock = new SystemClock()

    def reposeEndpoint
    def int shutdownPort

    def JmxClient jmx
    def static int jmxPort = 9001
    def int debugPort = 8005

    def ReposeConfigurationProvider configurationProvider

    ReposeValveLauncher(ReposeConfigurationProvider configurationProvider, String reposeJar, String reposeEndpoint, String configDir, int shutdownPort) {
        this.configurationProvider = configurationProvider
        this.reposeJar = reposeJar
        this.reposeEndpoint = reposeEndpoint
        this.shutdownPort = shutdownPort
        this.configDir = configDir
    }

    @Override
    void applyConfigs(String[] configLocations) {
        configurationProvider.applyConfigs(configLocations)
    }

    @Override
    void updateConfigs(String[] configLocations) {
        configurationProvider.updateConfigs(configLocations)
    }

    @Override
    void start() {
        killIfUp()

        def jmxprops = ""
        def debugProps = ""

        if (debugEnabled) {
            debugProps = "-Xdebug -Xrunjdwp:transport=dt_socket,address=${debugPort},server=y,suspend=n"
        }

        jmxPort++
        jmxprops = "-Dcom.sun.management.jmxremote.port=${jmxPort} -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.ssl=false -Dcom.sun.management.jmxremote.local.only=true"

        def cmd = "java ${debugProps} ${jmxprops} -jar ${reposeJar} -s ${shutdownPort} -c ${configDir} start"
        println("Starting repose: ${cmd}")

        def th = new Thread({cmd.execute()});

        th.run()
        th.join()

        def jmxUrl = "service:jmx:rmi:///jndi/rmi://localhost:${jmxPort}/jmxrmi"

        waitForCondition(clock, '15s', '1s') {
            connectViaJmxRemote(jmxUrl)
        }

        print("Waiting for repose to start")
        waitForCondition(clock, '60s', '1s', {
            isFilterChainInitialized()
        })
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
    void stop() {
        def cmd = "java -jar ${reposeJar} -s ${shutdownPort} stop"
        println("Stopping repose: ${cmd}")

        cmd.execute();
        waitForCondition(clock, '15s', '1s', {
            !isFilterChainInitialized()
        })
    }

    @Override
    void enableDebug() {
        this.debugEnabled = true
    }

    private boolean isFilterChainInitialized() {
        print('.')

        // First query for the mbean.  The name of the mbean is partially configurable, so search for a match.
        def HashSet cfgBean = jmx.getMBeans("*com.rackspace.papi.jmx:type=ConfigurationInformation")
        if (cfgBean == null || cfgBean.isEmpty()) {
            return false
        }

        def ArrayList filterchain = jmx.getMBeanAttribute(cfgBean.iterator().next().name.toString(), "FilterChain")

        if (filterchain == null || filterchain.isEmpty()) {
            return false
        }

        def initialized = true

        filterchain.each { data ->
            if (data."successfully initialized" == false) {
                initialized=false
            }
        }

        return initialized
    }

    private String getJvmProcesses() {
        def runningJvms = "jps".execute()
        runningJvms.waitFor()

        return runningJvms.in.text
    }

    private boolean isUp() {
        return getJvmProcesses().contains("repose-valve.jar")
    }

    private void killIfUp() {
        String processes = getJvmProcesses()
        def regex = /(\d*) repose-valve.jar/
        def matcher = ( processes =~ regex )
        if (matcher.size() > 0) {
            String pid = matcher[0][1]

            if (!pid.isEmpty()) {
                println("Killing running repose-valve process: " + pid)
                Runtime rt = Runtime.getRuntime();
                if (System.getProperty("os.name").toLowerCase().indexOf("windows") > -1)
                    rt.exec("taskkill " + pid.toInteger());
                else
                    rt.exec("kill -9 " + pid.toInteger());
            }
        }
    }


}
