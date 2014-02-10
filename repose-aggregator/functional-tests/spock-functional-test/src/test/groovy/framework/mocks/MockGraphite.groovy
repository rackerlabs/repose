package framework.mocks

import groovy.util.logging.Log4j
import org.rackspace.deproxy.LineReader
import org.rackspace.deproxy.UnbufferedStreamReader

@Log4j
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

    static void logAndPrintln(String line) {
        log.debug(line)
        println(line)
    }
}
