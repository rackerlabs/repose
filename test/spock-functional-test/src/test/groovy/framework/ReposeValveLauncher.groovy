package framework

import framework.client.jmx.JmxClient
import org.apache.http.client.ClientProtocolException
import org.apache.http.client.HttpClient
import org.apache.http.client.methods.HttpGet
import org.apache.http.impl.client.DefaultHttpClient
import org.linkedin.util.clock.SystemClock

import java.nio.charset.Charset
import java.util.concurrent.TimeoutException

import static org.junit.Assert.fail
import static org.linkedin.groovy.util.concurrent.GroovyConcurrentUtils.waitForCondition

class ReposeValveLauncher implements ReposeLauncher {

    def boolean debugEnabled
    def String reposeJar
    def String configDir
    def String connFramework = "jersey"

    def clock = new SystemClock()

    def reposeEndpoint
    def int shutdownPort
    def int reposePort

    def JmxClient jmx
    def int debugPort = 8011
    def classPaths =[]

    Process process

    def ReposeConfigurationProvider configurationProvider

    ReposeValveLauncher(ReposeConfigurationProvider configurationProvider,
                        String reposeJar,
                        String reposeEndpoint,
                        String configDir,
                        int reposePort,
                        int shutdownPort,
                        String connFramework="jersey") {
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


        if (killOthersBeforeStarting) {
            waitForCondition(clock, '5s', '1s', {
                killIfUp()
                !isUp()
            })
        }

        def jmxprops = ""
        def debugProps = ""
        def classPath = ""

        if (debugEnabled) {
            debugProps = "-Xdebug -Xrunjdwp:transport=dt_socket,address=${debugPort},server=y,suspend=n"
        }

        int jmxPort = nextAvailablePort()
        jmxprops = "-Dspock=spocktest -Dcom.sun.management.jmxremote.port=${jmxPort} -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.ssl=false -Dcom.sun.management.jmxremote.local.only=true"

        if(!classPaths.isEmpty()){
            classPath = "-cp " + (classPaths as Set).join(";")

        }

        def cmd = "java ${classPath} ${debugProps} ${jmxprops} -jar ${reposeJar} -s ${shutdownPort} -c ${configDir}"
        if (!connFramework.isEmpty()) {
            cmd = cmd + " -cf ${connFramework}"
        }
        cmd = cmd + " start"
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

        int socketTimeout = (timeout < 5000 ? timeout : 5000)

        try {

            Socket s = new Socket()
            s.setSoTimeout(socketTimeout)
            s.connect(new InetSocketAddress("localhost", shutdownPort), socketTimeout)
            s.outputStream.write("\r\n".getBytes(Charset.forName("US-ASCII")))
            s.outputStream.flush()
            s.close()

            waitForCondition(clock, "${timeout}", '1s', {
                !isUp()
            })

        } catch (Exception) {

            this.process.waitForOrKill(5000)

            if (throwExceptionOnKill) {
                throw new TimeoutException("Repose failed to stop cleanly")
            }
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

    private String getJvmProcesses() {
        def runningJvms = "jps -v".execute()
        runningJvms.waitFor()

        return runningJvms.in.text
    }

    @Override
    public boolean isUp() {
        return getJvmProcesses().contains("repose-valve.jar")
    }

    private void killIfUp() {
        String processes = getJvmProcesses()
        def regex = /(\d*) repose-valve.jar .*spocktest .*/
        def matcher = (processes =~ regex)
        if (matcher.size() > 0) {

            for (int i=1;i<=matcher.size();i++){
            String pid = matcher[0][i]

                if (pid!=null && !pid.isEmpty()) {
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

    def waitForNon500FromUrl(url, int timeoutInSeconds=60, int intervalInSeconds=2) {
        waitForCondition(clock, "${timeoutInSeconds}s", "${intervalInSeconds}s") {
            try {
                HttpClient client = new DefaultHttpClient()
                client.execute(new HttpGet(url)).statusLine.statusCode != 500
            } catch (IOException ignored) {
            } catch (ClientProtocolException ignored) {
            }
        }
    }
}
