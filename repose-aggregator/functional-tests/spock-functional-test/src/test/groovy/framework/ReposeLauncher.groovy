package framework

interface ReposeLauncher {

    void start();

    void stop();

    boolean isUp();

    void enableDebug()

    void addToClassPath(String path)
}
