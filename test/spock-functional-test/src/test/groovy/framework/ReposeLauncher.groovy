package framework

interface ReposeLauncher {

    void start();

    void stop();

    void enableDebug()

    void applyConfigs(String[] configLocations)

    void updateConfigs(String[] configLocations)
}
