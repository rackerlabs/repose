package framework

import deproxy.GDeproxy
import framework.client.jmx.JmxClient
import org.linkedin.util.clock.SystemClock

import static org.linkedin.groovy.util.concurrent.GroovyConcurrentUtils.waitForCondition

class ReposeValveLauncher implements ReposeLauncher {
    def JmxClient jmx
    def String jmxUrl

    def String reposeJar = "/Users/lisa/workspace/repose/test/spock-functional-test/target/repose_home/repose-valve.jar"
    def String configDir = "/Users/lisa/workspace/repose/test/spock-functional-test/target/repose_home/configs"

    def GDeproxy reposeClient
    def clock = new SystemClock()

    def int shutdownPort = 9999
    def int jmxPort = 9001
    def jmxprops = ""

    def ReposeConfigurationProvider configurationProvider

    ReposeValveLauncher(ReposeConfigurationProvider configurationProvider) {
        this.configurationProvider = configurationProvider
    }

    def setJmxUrl(String jmxUrl) {
        this.jmxUrl = jmxUrl
        jmx = new JmxClient(jmxUrl)
    }

    void enableJmx(boolean isEnabled) {
        if (isEnabled) {
            jmxprops = "-Dcom.sun.management.jmxremote.port=${jmxPort} -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.ssl=false -Dcom.sun.management.jmxremote.local.only=true"
        } else {
            jmxprops = ""
        }
    }

    void applyConfigs(String[] configLocations) {
        configurationProvider.applyConfigs(configLocations)
    }

    void updateConfigs(String[] configLocations) {
        configurationProvider.updateConfigs(configLocations)
    }

    @Override
    void start() {
        def cmd = "java ${jmxprops} -jar ${reposeJar} -s ${shutdownPort} -c ${configDir} start"
        println("Starting repose: ${cmd}")

        def th = new Thread({cmd.execute()});

        th.run()
        th.join()

        print("Waiting for repose to start")
        waitForCondition(clock, '30s', '1s', {
            isRunning()
        })
    }

    private boolean isRunning() {

        if (reposeClient == null) {
            reposeClient = new GDeproxy("http://localhost:8888")
        }

        try {
            def response = reposeClient.doGet("/")
            return response.getHeader("Via").contains("Repose")
        } catch (Exception e) {
        }
        print('.')
        return false
    }



    @Override
    void stop() {
        def cmd = "java -jar ${reposeJar} -s ${shutdownPort} stop"
        println("Stopping repose: ${cmd}")

        cmd.execute();
        waitForCondition(clock, '5s', '1s', {
            !isRunning()
        })
    }
}
