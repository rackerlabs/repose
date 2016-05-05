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
package framework.spockExtensions

import framework.TestProperties
import org.spockframework.runtime.AbstractRunListener
import org.spockframework.runtime.model.ErrorInfo
import org.spockframework.runtime.model.SpecInfo

/**
 * http://tomaszdziurko.pl/2013/05/taking-screenshot-in-failing-ui-tests-using-geb/
 *
 * Used this as an example to grab the repose logs on any test error.
 * Jenkins doesn't output them, and so it's stupid hard to figure out wtf is going on.
 */
class GrabReposeLogsOnFailureListener extends AbstractRunListener {

    @Override
    void beforeSpec(SpecInfo spec) {
        println("===================== Cleaning repose log file! =====================")
        def properties = new TestProperties()
        def logFile = new File(properties.logFile)
        if (logFile.exists() && logFile.canWrite()) {
            new FileOutputStream(logFile).getChannel().truncate(0).close()
            System.out.println("Truncated ${logFile}")
        }
        println("=============================  COMPLETED ============================")
    }

    @Override
    void error(ErrorInfo error) {

        //This should grab the repose logs on each error, and output them for Jenkins to have
        def properties = new TestProperties()

        def logFile = new File(properties.logFile)
        if (logFile.exists() && logFile.canRead()) {
            //Cat the contents of the log file?
            println("==================== REPOSE LOG FILE CONTENTS ====================")
            println(logFile.text)
            println("=============================== END ==============================")
        }
    }
}
