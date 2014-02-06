package features.core.powerfilter

import framework.ReposeValveTest
import groovy.util.logging.Log4j
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.LineReader
import org.rackspace.deproxy.PortFinder
import org.rackspace.deproxy.UnbufferedStreamReader

@Log4j
class GraphiteTest extends ReposeValveTest {

    static String METRIC_PREFIX = "test.1.metrics"
    static String METRIC_NAME = "repose-node1-com.rackspace.papi.ResponseCode.Repose.2XX.count"

    int graphitePort;
    def reader
    int lastCount = -1

    def setup() {

        graphitePort = PortFinder.Singleton.getNextOpenPort()

        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort)

        lastCount = -1
        def lineProc = { line ->
            def m = (line =~ /${METRIC_PREFIX}\.${METRIC_NAME}\s+(\d+)/)
            if (m) {
                lastCount = m.group(1).toInteger()
            }
        }
        reader = new MockGraphite(graphitePort, lineProc)

        def params = properties.getDefaultTemplateParams() + [graphitePort: graphitePort]
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/core/powerfilter/graphite", params)
        repose.start()

    }

    def cleanup() {
        if (deproxy)
            deproxy.shutdown()
        repose.stop()
    }

    def "when sending requests, data should be logged to graphite"() {

        when:
        def mc1 = deproxy.makeRequest(url: reposeEndpoint + "/endpoint")
        def mc2 = deproxy.makeRequest(url: reposeEndpoint + "/endpoint")
        def mc3 = deproxy.makeRequest(url: reposeEndpoint + "/cluster")
        sleep(2000)

        then:
        lastCount == 3
        mc1.receivedResponse.code == "200"
        mc2.receivedResponse.code == "200"
        mc3.receivedResponse.code == "200"
    }

    class MockGraphite {

        int port
        ServerSocket listener
        boolean _stop
        def threads = []
        final Object threadsLock = new Object();

        public MockGraphite(int listenPort, Closure lineProcessor = null, boolean logTheData = false, String label = "MockGraphite") {

            this.port = listenPort
            listener = new ServerSocket(listenPort)
            def t = Thread.startDaemon {

                while (!_stop) {

                    def repose = listener.accept()
                    repose.soTimeout = 1000

                    def t1 = Thread.startDaemon {
                        try {

                            def reader = new UnbufferedStreamReader(repose.inputStream)
                            while (!_stop) {
                                try {
                                    String line = LineReader.readLine(reader)
                                    if (line == null) break;

                                    if (lineProcessor != null) {
                                        lineProcessor(line)
                                    }

                                    if (logTheData) {
                                        logAndPrintln("${label}: read a line: ${line}")
                                    }

                                } catch (Exception ignored) {
                                    logAndPrintln("${label}: Caught an exception: ${ignored}")
                                    sleep(100)
                                }
                            }
                        } finally {
                            repose.close()
                        }
                    }
                    synchronized (threadsLock) {
                        threads.add(t1)
                    }
                }
            }

            synchronized (threadsLock) {
                threads.add(t)
            }
        }

        public void stop() {
            _stop = true

            Thread[] threads2
            synchronized (threadsLock) {
                threads2 = threads.toArray() as Thread[]
                threads.clear()
            }

            try {
                for (Thread th in threads2) {
                    th.interrupt()
                    th.join(100)
                }
            } catch (Exception ignored) {}
        }

        void logAndPrintln(String line) {
            log.debug(line)
            println(line)
        }
    }

}
