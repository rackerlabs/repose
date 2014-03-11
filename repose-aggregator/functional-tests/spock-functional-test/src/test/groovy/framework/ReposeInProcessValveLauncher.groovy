package framework

import com.rackspace.cloud.valve.server.PowerApiValveServerControl

class ReposeInProcessValveLauncher {

    ReposeConfigurationProvider configurationProvider
    String configDir
    int stopPort
    PowerApiValveServerControl valve

    ReposeInProcessValveLauncher(ReposeConfigurationProvider configurationProvider,
                                 String configDir,
                                 int stopPort) {

        this.configurationProvider = configurationProvider
        this.stopPort = stopPort
        this.configDir = configDir

        this.valve = new PowerApiValveServerControl(null, null, stopPort, configDir, null, null)

        this.valve.startPowerApiValve()
    }

    void stop() {

        this.valve.stopPowerApiValve()
    }

}
