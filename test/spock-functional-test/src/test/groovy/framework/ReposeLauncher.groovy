package framework

interface ReposeLauncher {

    void start();

    void stop();

    void enableJmx(boolean isEnabled)

    void applyConfigs(String[] configLocations)

    void updateConfigs(String[] configLocations)
}
