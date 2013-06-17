package framework

interface ReposeLauncher {

    void start();

    void stop();

    void enableJmx()

    void applyConfigs(String[] configLocations)

    void updateConfigs(String[] configLocations)
}
