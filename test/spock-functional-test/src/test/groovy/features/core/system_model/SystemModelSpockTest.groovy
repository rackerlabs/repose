package features.filters.apivalidator

import framework.ReposeValveTest

class SystemModelSpockTest extends ReposeValveTest {

    def setup() {
        repose.applyConfigs("features/core/system_model/dist_datastore_filter_and_service")
    }

    def cleanup() {
        repose.stop()
    }

    def "when starting repose with both DD service and filter, it should fail to start"() {

        def List<String> stringList;

        when:
        try {
            repose.start()
        } catch (Exception e) {}

        stringList = reposeLogSearch.searchByString("The distributed datastore filter and service can not")

        then:
        stringList.size() > 0
    }
}
