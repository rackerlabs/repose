package framework

interface ReposeLauncher {

    void start();

    void stop();

    void enableJmx(boolean isEnabled)
}
