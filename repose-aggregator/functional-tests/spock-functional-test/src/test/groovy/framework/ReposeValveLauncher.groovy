package framework

import framework.client.jmx.JmxClient
import org.linkedin.util.clock.SystemClock
import org.rackspace.deproxy.PortFinder

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.StandardOpenOption
import java.util.concurrent.TimeoutException

import static org.linkedin.groovy.util.concurrent.GroovyConcurrentUtils.waitForCondition

class ReposeValveLauncher extends ReposeLauncher {

    def boolean debugEnabled
    def String servoJar
    def String jettyJar
    def String reposeWar
    def String configDir

    def clock = new SystemClock()

    def reposeEndpoint
    def int reposePort

    def JmxClient jmx
    def jmxPort = null
    def debugPort = null
    def classPaths = []

    def Process process
    def StringBuffer sout
    def StringBuffer serr

    def ReposeConfigurationProvider configurationProvider

    ReposeValveLauncher(ReposeConfigurationProvider configurationProvider,
                        TestProperties properties) {
        this(configurationProvider,
                properties.servoJar,
                properties.jettyJar,
                properties.reposeWar,
                properties.reposeEndpoint,
                properties.configDirectory,
                properties.reposePort
        )
    }

    ReposeValveLauncher(ReposeConfigurationProvider configurationProvider,
                        String servoJar,
                        String jettyJar,
                        String reposeWar,
                        String reposeEndpoint,
                        String configDir,
                        int reposePort) {
        this.configurationProvider = configurationProvider
        this.servoJar = servoJar
        this.jettyJar = jettyJar
        this.reposeWar = reposeWar
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

        File servoFile = new File(servoJar)
        if (!servoFile.exists() || !servoFile.canRead() || !servoFile.isFile()) {
            throw new FileNotFoundException("Missing or invalid Repose Servo Jar file.")
        }

        File jettyFile = new File(jettyJar)
        if (!jettyFile.exists() || !jettyFile.canRead() || !jettyFile.isFile()) {
            throw new FileNotFoundException("Missing or invalid Repose Jetty Jar file.")
        }

        File reposeFile = new File(reposeWar)
        if (!reposeFile.exists() || !reposeFile.canRead() || !reposeFile.isFile()) {
            throw new FileNotFoundException("Missing or invalid Repose Application War file.")
        }

        File configFolder = new File(configDir)
        if (!configFolder.exists() || !configFolder.canRead() || !configFolder.isDirectory()) {
            throw new FileNotFoundException("Missing or invalid configuration folder.")
        }


        if (killOthersBeforeStarting) {
            waitForCondition(clock, '5s', '1s', {
                killIfUp()
                !isUp()
            })
        }

        def jmxprops
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

        def overrideFile = File.createTempFile("overrideFile", ".conf")
        overrideFile.deleteOnExit()

        //NOTE: this command is the one that is going to be repose itself
        //NOTE: I don't know if the classPath stuff is going to work at all....
        def baseCommand = "java -Xmx1536M -Xms1024M -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/tmp/dump-${debugPort}.hprof -XX:MaxPermSize=128M $classPath $debugProps $jmxprops $jacocoProps"

        //Override a few things in the servo config file to do testing with debug and heap dump and JMX
        def overrideContent = """
launcherPath = ${jettyJar}
reposeWarLocation = ${reposeWar}
baseCommand = [ ${baseCommand} ]
"""

        Files.write(overrideFile.toPath(),
                overrideContent.getBytes(StandardCharsets.UTF_8),
                StandardOpenOption.CREATE)

        //NOTE: this command is the one that fires up servo itself
        def servoCommand = "java -jar $servoJar -c $configDir --XX_CONFIGURATION_OVERRIDE_FILE_XX "+overrideFile.absolutePath

        println("Repose launcher command: ${baseCommand}")
        println("Starting servo: ${servoCommand}")

        def th = new Thread({ process = servoCommand.execute() });
        sout = new StringBuffer()
        serr = new StringBuffer()

        th.run()
        th.join()
        process.consumeProcessOutput(sout, serr)

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
    }

    def connectViaJmxRemote(String jmxUrl) {
        def rtn = true;
        try {
            jmx = new JmxClient(jmxUrl)
        } catch (Exception ex) {
            print("Caught the following unexpected exception: "+ex)
            rtn = false
        }
        return rtn
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
            def SIGHUP = 1
            sendServoSignalIfUp(SIGHUP)

            print("Waiting for Repose to shutdown")
            waitForCondition(clock, "${timeout}", '1s', {
                print(".")
                !isUp()
            })
            println()
            println("STD_OUT:\n${sout}\n")
            println("STD_ERR:\n${serr}\n")
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
        def HashSet cfgBean = (HashSet)jmx.getMBeans("*com.rackspace.papi.jmx:type=ConfigurationInformation")
        if (cfgBean == null || cfgBean.isEmpty()) {
            return false
        }

        def String beanName = cfgBean.iterator().next().name.toString()

        def ArrayList filterChain = (ArrayList)jmx.getMBeanAttribute(beanName, "FilterChain")

        if (filterChain == null || filterChain.size() == 0) {
            return beanName.contains("nofilters")
        }

        def initialized = true

        filterChain.each { data ->
            if (data."successfully initialized" == false) {
                initialized = false
            }
        }
        return initialized
    }

    @Override
    public boolean isUp() {
        def processes = TestUtils.getJvmProcesses()
        //println processes
        return processes.contains("servo.jar")
    }

    private static void killIfUp() {
        def SIGKILL = 9
        sendServoSignalIfUp(SIGKILL)
    }

    private static void sendServoSignalIfUp(signal) {
        String processes = TestUtils.getJvmProcesses()
        def regex = /(\d*) servo.jar .*spocktest .*/
        def matcher = (processes =~ regex)
        if (matcher.size() > 0) {

            for (int i = 1; i <= matcher.size(); i++) {
                String pid = matcher[0][i]

                if (pid != null && !pid.isEmpty()) {
                    println("Killing running repose-valve process: " + pid)
                    Runtime rt = Runtime.getRuntime();
                    if (System.getProperty("os.name").toLowerCase().indexOf("windows") > -1) {
                        rt.exec("taskkill " + pid.toInteger());
                    } else {
                        rt.exec("kill -${signal} " + pid.toInteger());
                    }
                }
            }
        }
    }
}
