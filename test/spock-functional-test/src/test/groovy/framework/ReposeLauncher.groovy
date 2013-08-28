package framework

interface ReposeLauncher {

    void start();

    void stop();

    boolean isUp();

    void enableDebug()

    void applyConfigs(String[] configLocations)

    void updateConfigs(String[] configLocations)

    void addToClassPath(String path)
}
