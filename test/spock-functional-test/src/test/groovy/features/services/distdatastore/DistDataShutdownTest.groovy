package features.services.distdatastore

import framework.ReposeValveTest

class DistDataShutdownTest extends ReposeValveTest {


    def "when shutting down repose, should stop dd service" () {
        given:
        repose.applyConfigs("features/services/distdatastore/")
        repose.enableDebug()
        repose.start()

        when:
        repose.stop()

        then:
        repose.isUp() == false
    }


}
