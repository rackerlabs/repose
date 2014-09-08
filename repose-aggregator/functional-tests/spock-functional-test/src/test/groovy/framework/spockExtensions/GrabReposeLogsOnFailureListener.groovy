package framework.spockExtensions

import framework.TestProperties
import org.spockframework.runtime.AbstractRunListener
import org.spockframework.runtime.model.ErrorInfo

/**
 * http://tomaszdziurko.pl/2013/05/taking-screenshot-in-failing-ui-tests-using-geb/
 *
 * Used this as an example to grab the repose logs on any test error.
 * Jenkins doesn't output them, and so it's stupid hard to figure out wtf is going on.
 */
class GrabReposeLogsOnFailureListener extends AbstractRunListener {

    @Override
    void error(ErrorInfo error) {

        //This should grab the repose logs on each error, and output them for Jenkins to have
        def properties = new TestProperties()

        def logFile = new File(properties.logFile)
        if(logFile.exists() && logFile.canRead()) {
            //Cat the contents of the log file?
            println("==================== REPOSE LOG FILE CONTENTS ====================")
            println(logFile.text)
            println("=============================== END ==============================")
        }
    }
}
