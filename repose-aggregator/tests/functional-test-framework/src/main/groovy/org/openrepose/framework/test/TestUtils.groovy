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

import java.util.concurrent.TimeUnit

class TestUtils {

    def static String getJvmProcesses() {
        def runningJvms = "jps -v".execute()
        runningJvms.waitFor()

        return runningJvms.in.text
    }

    /**
     * Takes a boolean closure that will indicate whatever it's looking for
     * If the timeout hits, it's going to fail via throwing an exception
     * This could probably be reused in many places.
     *
     * Additionally, you can pass in an optional TimeUnit, it operates in seconds right now.
     * it will wait 500ms between iterations right now.
     * @param timeout
     * @param timeUnit
     * @param block
     * @return
     */
    static def timedSearch(int timeout, TimeUnit timeUnit = TimeUnit.SECONDS, Closure block) {
        def startTime = System.currentTimeMillis()
        boolean foundIt = false

        while (System.currentTimeMillis() < startTime + timeUnit.toMillis(timeout) && !foundIt) {
            foundIt = block.call()
            Thread.sleep(500)
        }

        if (!foundIt) {
            throw new Exception("Unable to satisfy condition within ${timeout} seconds")
        }
        foundIt
    }


}
