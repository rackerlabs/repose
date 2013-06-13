package features.filters.responsemessaging

import framework.ReposeValveTest

class ResponseMessagingFilterTest extends ReposeValveTest {

    def setup() {
        repose.applyConfigs(
                "features/filters/responsemessaging")
        repose.start()
    }

    def cleanup() {
        repose.stop()
    }

    def "when starting up, should register validator MXBeans"() {

        when:
        def validatorBeans = true

        then:
        validatorBeans == 3
    }
}
