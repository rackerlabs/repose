package framework

import org.linkedin.util.clock.SystemClock
import org.rackspace.deproxy.PortFinder

import java.util.concurrent.TimeoutException

import static org.linkedin.groovy.util.concurrent.GroovyConcurrentUtils.waitForCondition

class ReposeContainerLauncher extends AbstractReposeLauncher {

    int reposePort

    String clusterId
    String nodeId

    String containerJar
    String rootWarLocation
    String[] appWars
    String debugPort

    def boolean debugEnabled = true

    def clock = new SystemClock()
    def Process process

    ReposeContainerLauncher(ReposeConfigurationProvider configurationProvider, String containerJar,
                            String clusterId, String nodeId,
                            String rootWarLocation, int reposePort, String... appWars) {
        this.configurationProvider = configurationProvider
        this.containerJar = containerJar
        this.clusterId = clusterId
        this.nodeId = nodeId
        this.reposePort = reposePort
        this.rootWarLocation = rootWarLocation

        this.appWars = appWars
    }

    @Override
    void start() {
        String configDirectory = configurationProvider.getReposeConfigDir()

        String webXmlOverrides = "-Dpowerapi-config-directory=${configDirectory}"
        if (clusterId != null) {
            webXmlOverrides += " -Drepose-cluster-id=${clusterId}"
        }
        if (nodeId != null) {
            webXmlOverrides += " -Drepose-node-id=${nodeId}"
        }

        if (debugEnabled) {

            if (!debugPort) {
                debugPort = PortFinder.Singleton.getNextOpenPort()
            }
            webXmlOverrides = webXmlOverrides + " -Xdebug -Xrunjdwp:transport=dt_socket,address=${debugPort},server=y,suspend=n"
        }

        def cmd = "java ${webXmlOverrides} -jar ${containerJar} -p ${reposePort} -w ${rootWarLocation} "

        if (appWars != null || appWars.length != 0) {
            for (String path : appWars) {
                cmd += " -war ${path}"
            }
        }
        println("Starting repose: ${cmd}")

        def th = new Thread({ this.process = cmd.execute() });

        th.run()
        th.join()
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

            print("Waiting for Repose Container to shutdown")
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

    private void killIfUp() {
        String processes = TestUtils.getJvmProcesses()
        def regex = /(\d*) ROOT.war .*/
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

    @Override
    boolean isUp() {
        return TestUtils.getJvmProcesses().contains("ROOT.war")
    }

}
