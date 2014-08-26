package framework
import com.typesafe.config.Config
import org.openrepose.servo.Servo

import java.util.concurrent.Callable
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.FutureTask
import java.util.concurrent.TimeUnit

class ReposeInProcessValveLauncher extends ReposeLauncher {

    def ReposeConfigurationProvider configurationProvider

    def String[] args
    def InputStream ins
    def PrintStream out
    def PrintStream err
    def Config config
    def Servo servo
    def boolean isUp = false
    def ExecutorService executor = Executors.newSingleThreadExecutor()
    def FutureTask<Integer> future = null
    def int futureRtn

    ReposeInProcessValveLauncher(String[] args,
                                 InputStream ins,
                                 PrintStream out,
                                 PrintStream err,
                                 Config config) {

        this.args = args
        this.ins = ins
        this.out = out
        this.err = err
        this.config = config

        this.servo = new Servo()
    }

    @Override
    void start() {
        future =  new FutureTask<Integer>(
                new Callable<Integer>() {
                    public Integer call() {
                        return servo.execute(args, ins, out, err, config)
                    }})
        executor.execute(future)
        this.isUp = true
    }

    void stop() {
        if(this.isUp) {
            this.servo.shutdown()
            futureRtn = future.get(10, TimeUnit.SECONDS)
            this.isUp = false
        }
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
