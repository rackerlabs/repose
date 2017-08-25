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
package org.openrepose.framework.test

import org.apache.http.client.HttpClient
import org.apache.http.client.methods.HttpGet
import org.apache.http.impl.client.DefaultHttpClient
import org.linkedin.util.clock.SystemClock

import static org.linkedin.groovy.util.concurrent.GroovyConcurrentUtils.waitForCondition

abstract class ReposeLauncher {

    protected Process process

    abstract void start()

    abstract void stop()

    abstract void enableDebug()

    boolean isUp() {
        this.process?.isAlive() ?: false
    }

    /**
     * This enables easier debugging from the actual code under test.
     * This should call or fullfill all of the contracts performed by enableDebug().
     * This will print the port number to connect the debugger to and suspend the VM until it is connected.
     */
    abstract void enableSuspend()

    static def waitForNon500FromUrl(String url, int timeoutInSeconds = 60, int intervalInSeconds = 2) {
        waitForResponseCodeFromUrl(url, timeoutInSeconds, intervalInSeconds) { code -> code < 500 }
    }

    static def waitForDesiredResponseCodeFromUrl(String url, desiredCodes, timeoutInSeconds = 60, int intervalInSeconds = 2) {
        waitForResponseCodeFromUrl(url, timeoutInSeconds, intervalInSeconds) { code -> code in desiredCodes }
    }

    static def waitForResponseCodeFromUrl(String url, timeoutInSeconds, int intervalInSeconds, isResponseAcceptable) {
        print("\n\nWaiting for repose to start at ${url} \n\n")
        waitForCondition(SystemClock.INSTANCE, "${timeoutInSeconds}s", "${intervalInSeconds}s") {
            try {
                print(".")
                HttpClient client = new DefaultHttpClient()
                isResponseAcceptable(client.execute(new HttpGet(url)).statusLine.statusCode)
            } catch (IOException ignored) {
            }
        }
        println()
    }
}
