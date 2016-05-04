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
package framework

import org.slf4j.LoggerFactory

import java.util.concurrent.*

/**
 * Responsible for searching log file for an instance of Repose
 */
class ReposeLogSearch {

    private final LOG = LoggerFactory.getLogger(this.getClass())

    String logFileLocation;

    ReposeLogSearch(String logFileLocation) {
        this.logFileLocation = logFileLocation
    }

    /**
     * Search the repose log file using the properties log locations specified
     * @param searchString is used to search log file for matches
     */
    public List<String> searchByString(String searchString) {
        File logFile = new File(logFileLocation);

        def foundMatches = []
        logFile.eachLine {

            ln ->
                if (ln =~ searchString) {
                    foundMatches << "${ln}"
                }
        }


        return foundMatches;

    }

    /**
     * A mechanism to provide a blocking awaitable for log lines. It will wait for at least the specified number of
     * log lines, and it will wait for the specified period of time. If unable to get that, it'll throw an
     * TimeoutException or an ExecutionException if the work threw some other exception
     * @param searchString
     * @param atLeast
     * @param duration
     * @param timeUnit
     * @return
     */
    public List<String> awaitByString(String searchString, int atLeast = 1, long duration = 3, TimeUnit timeUnit = TimeUnit.SECONDS) throws TimeoutException, ExecutionException {

        def ec = Executors.newFixedThreadPool(1)
        def logSearchFuture = ec.submit(new Callable<List<String>>() {
            @Override
            List<String> call() throws Exception {
                boolean stopSearching = false
                def foundMatches = []
                while (!stopSearching) {
                    try {
                        foundMatches = searchByString(searchString)
                        if (foundMatches.size() >= atLeast) {
                            stopSearching = true
                            return foundMatches
                        }
                    } catch (InterruptedException e) {
                        LOG.trace("Interrupted, so being terminated")
                        stopSearching = true
                    } catch (Exception e) {
                        LOG.trace("Exception caught when searching for log lines", e)
                    } finally {
                        Thread.sleep(100)
                    }
                }
                return foundMatches;
            }
        })

        try {
            def result = logSearchFuture.get(duration, timeUnit)

            return result
        } catch (Exception e) {
            logSearchFuture.cancel(true)
            throw e
        }
    }

    public List<String> printLog() {
        File logFile = new File(logFileLocation);
        logFile.eachLine {
            println it
        }
    }

    public String logToString() {
        File logFile = new File(logFileLocation);
        String log = new String()
        logFile.eachLine {
            log += it.trim()
        }
        return log
    }

    public def cleanLog() {
        println("============================== Cleaning log file ==============================")
        def logFile = new File(logFileLocation)
        if (logFile.exists() && logFile.canWrite()) {
            new FileOutputStream(logFile).getChannel().truncate(0).close()
            System.out.println("Truncated ${logFile}")
        }
        println("================================== COMPLETED ==================================")
    }
}
