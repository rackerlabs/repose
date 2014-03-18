package framework

abstract class ReposeLauncher {

    abstract void start();

    abstract void stop();

    abstract boolean isUp();

    abstract void enableDebug()

    abstract void addToClassPath(String path)
}
