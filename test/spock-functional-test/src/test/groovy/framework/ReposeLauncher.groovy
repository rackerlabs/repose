package framework

interface ReposeLauncher {

    void start();

    void stop();

    void enableJmx()

    void enableDebug()

    void applyConfigs(String[] configLocations)

    void updateConfigs(String[] configLocations)
}
