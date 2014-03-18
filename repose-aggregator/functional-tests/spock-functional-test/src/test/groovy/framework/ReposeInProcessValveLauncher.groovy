package framework

import com.rackspace.cloud.valve.server.PowerApiValveServerControl
import org.apache.http.client.ClientProtocolException
import org.apache.http.client.HttpClient
import org.apache.http.client.methods.HttpGet
import org.apache.http.impl.client.DefaultHttpClient
import org.linkedin.util.clock.SystemClock

import static org.linkedin.groovy.util.concurrent.GroovyConcurrentUtils.waitForCondition

class ReposeInProcessValveLauncher implements ReposeLauncher {

    ReposeConfigurationProvider configurationProvider
    String configDir
    int stopPort
    PowerApiValveServerControl valve
    boolean isUp = false

    ReposeInProcessValveLauncher(ReposeConfigurationProvider configurationProvider,
                                 String configDir,
                                 int stopPort) {

        this.configurationProvider = configurationProvider
        this.stopPort = stopPort
        this.configDir = configDir

        this.valve = new PowerApiValveServerControl(null, null, stopPort, configDir, null, null)
    }

    @Override
    void start() {

        this.valve.startPowerApiValve()
        this.isUp = true
    }

    void stop() {

        this.valve.stopPowerApiValve()
        this.isUp = false
    }

    @Override
    boolean isUp() {
        return this.isUp
    }

    @Override
    void enableDebug() {
    }

    @Override
    void addToClassPath(String path) {
    }

    def clock = new SystemClock()
    def waitForNon500FromUrl(url, int timeoutInSeconds=60, int intervalInSeconds=2) {

        print("Waiting for repose to start at ${url} ")
        waitForCondition(clock, "${timeoutInSeconds}s", "${intervalInSeconds}s") {
            try {
                print(".")
                HttpClient client = new DefaultHttpClient()
                client.execute(new HttpGet(url)).statusLine.statusCode != 500
            } catch (ClientProtocolException ignored) {
            } catch (IOException ignored) {
            }
        }
        println()
    }
}
