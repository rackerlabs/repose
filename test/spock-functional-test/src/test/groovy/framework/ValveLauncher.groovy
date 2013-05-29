package framework

import org.linkedin.util.clock.SystemClock

import static org.linkedin.groovy.util.concurrent.GroovyConcurrentUtils.waitForCondition

class ValveLauncher implements ReposeLauncher {

    def String reposeJar = "/Users/lisa.clark/myworkspace/repose/test/spock-functional-test/target/repose_home/repose-valve.jar"
    def String configDir = "/Users/lisa.clark/myworkspace/repose/test/spock-functional-test/target/repose_home/configs"

    def ReposeClient reposeClient

    def int port = 8888


    @Override
    void start() {

        def cmd = "java -jar ${reposeJar} -s ${port} -c ${configDir} start"
        println("Starting repose for spock testing: ${cmd}")

        def th = Thread.start {
            def output = cmd.execute().text
        }

        th.start()

        def clock = new SystemClock()
        waitForCondition(clock, '30s', '1s', {
            isRunning()
        })
    }

    boolean isRunning() {
        def http = new HTTPBuilder('http://www.google.com')

        if (reposeClient == null) {
            reposeClient = new ReposeClient(8888)
        }

        def response = reposeClient.doGet()
    }



    @Override
    void stop() {
        def cmd = ""
    }
}
