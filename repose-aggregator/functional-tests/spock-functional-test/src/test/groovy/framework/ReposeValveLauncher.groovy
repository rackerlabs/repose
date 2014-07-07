package framework

import framework.client.jmx.JmxClient
import org.linkedin.util.clock.SystemClock
import org.rackspace.deproxy.PortFinder

import java.util.concurrent.TimeoutException

import static org.linkedin.groovy.util.concurrent.GroovyConcurrentUtils.waitForCondition

class ReposeValveLauncher extends ReposeLauncher {

    def boolean debugEnabled
    def String reposeJar
    def String configDir

    def clock = new SystemClock()

    def reposeEndpoint
    def int reposePort

    def JmxClient jmx
    def jmxPort = null
    def debugPort = null
    def classPaths = []

    Process process

    def ReposeConfigurationProvider configurationProvider

    ReposeValveLauncher(ReposeConfigurationProvider configurationProvider,
                        TestProperties properties) {
        this(configurationProvider,
                properties.reposeJar,
                properties.reposeEndpoint,
                properties.configDirectory,
                properties.reposePort
        )
    }

    ReposeValveLauncher(ReposeConfigurationProvider configurationProvider,
                        String reposeJar,
                        String reposeEndpoint,
                        String configDir,
                        int reposePort) {
        this.configurationProvider = configurationProvider
        this.reposeJar = reposeJar
        this.reposeEndpoint = reposeEndpoint
        this.reposePort = reposePort
        this.configDir = configDir
    }

    @Override
    void start() {
        this.start([:])
    }

    void start(Map params) {

        boolean killOthersBeforeStarting = true
        if (params.containsKey("killOthersBeforeStarting")) {
            killOthersBeforeStarting = params.killOthersBeforeStarting
        }
        boolean waitOnJmxAfterStarting = true
        if (params.containsKey("waitOnJmxAfterStarting")) {
            waitOnJmxAfterStarting = params.waitOnJmxAfterStarting
        }

        start(killOthersBeforeStarting, waitOnJmxAfterStarting)
    }

    void start(boolean killOthersBeforeStarting, boolean waitOnJmxAfterStarting) {

        File jarFile = new File(reposeJar)
        if (!jarFile.exists() || !jarFile.isFile()) {
            throw new FileNotFoundException("Missing or invalid Repose Valve Jar file.")
        }

        File configFolder = new File(configDir)
        if (!configFolder.exists() || !configFolder.isDirectory()) {
            throw new FileNotFoundException("Missing or invalid configuration folder.")
        }


        if (killOthersBeforeStarting) {
            waitForCondition(clock, '5s', '1s', {
                killIfUp()
                !isUp()
            })
        }

        def jmxprops = ""
        def debugProps = ""
        def jacocoProps = ""
        def classPath = ""

        if (debugEnabled) {

            if (!debugPort) {
                debugPort = PortFinder.Singleton.getNextOpenPort()
            }
            debugProps = "-Xdebug -Xrunjdwp:transport=dt_socket,address=${debugPort},server=y,suspend=n"
        }

        if (!jmxPort) {
            jmxPort = PortFinder.Singleton.getNextOpenPort()
        }
        jmxprops = "-Dspock=spocktest -Dcom.sun.management.jmxremote.port=${jmxPort} -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.ssl=false -Dcom.sun.management.jmxremote.local.only=true"

        if (!classPaths.isEmpty()) {
            classPath = "-cp " + (classPaths as Set).join(";")

        }

        if (System.getProperty('jacocoArguements')) {
            jacocoProps = System.getProperty('jacocoArguements')
        }

        def cmd = "java -Xmx1536M -Xms1024M -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/tmp/dump.hprof -XX:MaxPermSize=128M $classPath $debugProps $jmxprops $jacocoProps -jar $reposeJar -c $configDir"
        println("Starting repose: ${cmd}")

        def th = new Thread({ this.process = cmd.execute() });

        th.run()
        th.join()

        def jmxUrl = "service:jmx:rmi:///jndi/rmi://localhost:${jmxPort}/jmxrmi"

        waitForCondition(clock, '60s', '1s') {
            connectViaJmxRemote(jmxUrl)
        }

        if (waitOnJmxAfterStarting) {
            print("Waiting for repose to start")
            waitForCondition(clock, '60s', '1s', {
                isFilterChainInitialized()
            })
        }

        // TODO: improve on this.  embedding a sleep for now, but how can we ensure Repose is up and
        // ready to receive requests without actually sending a request through (skews the metrics if we do)
        //sleep(10000)
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
        this.stop([:])
    }

    void stop(Map params) {
        def timeout = params?.timeout ?: 45000
        def throwExceptionOnKill = true

        if (params.containsKey("throwExceptionOnKill")) {
            throwExceptionOnKill = params.throwExceptionOnKill
        }

        stop(timeout, throwExceptionOnKill)
    }

    void stop(int timeout, boolean throwExceptionOnKill) {
        try {
            println("Stopping Repose");
            this.process.destroy()

            print("Waiting for Repose to shutdown")
            waitForCondition(clock, "${timeout}", '1s', {
                print(".")
                !isUp()
            })

            println()
        } catch (IOException ioex) {
            this.process.waitForOrKill(5000)
            killIfUp()
            if (throwExceptionOnKill) {
                throw new TimeoutException("An error occurred while attempting to stop Repose Controller. Reason: " + ioex.getMessage());
            }
        }
    }

    @Override
    void enableDebug() {
        this.debugEnabled = true
    }

    @Override
    void addToClassPath(String path) {
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

    @Override
    public boolean isUp() {
        println TestUtils.getJvmProcesses()
        return TestUtils.getJvmProcesses().contains("repose-valve.jar")
    }

    private void killIfUp() {
        String processes = TestUtils.getJvmProcesses()
        def regex = /(\d*) repose-valve.jar .*spocktest .*/
        def matcher = (processes =~ regex)
        if (matcher.size() > 0) {

            for (int i = 1; i <= matcher.size(); i++) {
                String pid = matcher[0][i]

                if (pid != null && !pid.isEmpty()) {
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
}
