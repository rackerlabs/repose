/*
 * _=_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_=
 * Repose
 * _-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-
 * Copyright (C) 2010 - 2015 Rackspace US, Inc.
 * _-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * =_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_=_
 */
package org.openrepose.framework.test.mocks

import groovy.util.logging.Log4j
import org.rackspace.deproxy.LineReader
import org.rackspace.deproxy.UnbufferedStreamReader

@Log4j
class MockTracerAgent {

    int port
    ServerSocket listener
    boolean _stop
    def threads = []
    final Object threadsLock = new Object();

    MockTracerAgent(int listenPort, boolean logTheData = false, String label = "MockTracerAgent") {

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
        } catch (Exception ignored) {
        }
    }

    static void logAndPrintln(String line) {
        log.debug(line)
        println(line)
    }
}
