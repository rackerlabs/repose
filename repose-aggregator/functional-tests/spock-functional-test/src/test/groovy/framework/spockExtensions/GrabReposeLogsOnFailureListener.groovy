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
        if(logFile.exists() && logFile.canWrite()) {
            def deleted = logFile.delete()
            def created = logFile.createNewFile()
            System.out.println("Deleted: ${deleted}  Created: ${created}")
        }
        println("=============================  COMPLETED ============================")
    }

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
