package framework
import org.openrepose.core.valve.PowerApiValveServerControl

class ReposeInProcessValveLauncher extends ReposeLauncher {

    ReposeConfigurationProvider configurationProvider
    String configDir
    PowerApiValveServerControl valve
    boolean isUp = false

    ReposeInProcessValveLauncher(ReposeConfigurationProvider configurationProvider,
                                 String configDir) {

        this.configurationProvider = configurationProvider
        this.configDir = configDir

        this.valve = new PowerApiValveServerControl(null, null, configDir, null, null)
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
}
