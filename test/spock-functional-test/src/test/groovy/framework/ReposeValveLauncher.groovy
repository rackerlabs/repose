package framework

import deproxy.GDeproxy
import framework.client.jmx.JmxClient
import org.linkedin.util.clock.SystemClock

import static org.linkedin.groovy.util.concurrent.GroovyConcurrentUtils.waitForCondition

class ReposeValveLauncher implements ReposeLauncher {

    def boolean debugEnabled
    def String reposeJar
    def String configDir

    def GDeproxy reposeClient
    def clock = new SystemClock()

    def reposeEndpoint
    def int shutdownPort

    def JmxClient jmx
    def boolean jmxEnabled = false
    def String jmxUrl
    def int jmxPort = 9001
    def int debugPort = 8005

    def ReposeConfigurationProvider configurationProvider

    ReposeValveLauncher(ReposeConfigurationProvider configurationProvider, String reposeJar, String reposeEndpoint, String configDir, int shutdownPort, String jmxUrl, int jmxPort) {
        this.configurationProvider = configurationProvider
        this.reposeJar = reposeJar
        this.reposeEndpoint = reposeEndpoint
        this.shutdownPort = shutdownPort
        this.configDir = configDir
        this.jmxUrl = jmxUrl
        this.jmxPort = jmxPort
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

        if (jmxEnabled) {
            jmxprops = "-Dcom.sun.management.jmxremote.port=${jmxPort} -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.ssl=false -Dcom.sun.management.jmxremote.local.only=true"
        }

        def cmd = "java ${debugProps} ${jmxprops} -jar ${reposeJar} -s ${shutdownPort} -c ${configDir} start"
        println("Starting repose: ${cmd}")

        def th = new Thread({cmd.execute()});

        th.run()
        th.join()

        print("Waiting for repose to start")
        waitForCondition(clock, '30s', '1s', {
            isAvailable()
        })

        if (jmxEnabled) {
            jmx = new JmxClient(jmxUrl)
        }
    }

    @Override
    void stop() {
        def cmd = "java -jar ${reposeJar} -s ${shutdownPort} stop"
        println("Stopping repose: ${cmd}")

        cmd.execute();
        waitForCondition(clock, '15s', '1s', {
            !isAvailable()
        })
    }

    @Override
    void enableJmx() {
        this.jmxEnabled = true
    }

    @Override
    void enableDebug() {
        this.debugEnabled = true
    }

    private boolean isAvailable() {

        if (reposeClient == null) {
            reposeClient = new GDeproxy(reposeEndpoint)
        }

        try {
            def response = reposeClient.doGet("/")
            return response.getHeader("Via").contains("Repose")
        } catch (Exception e) {
        }
        print('.')
        return false
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
