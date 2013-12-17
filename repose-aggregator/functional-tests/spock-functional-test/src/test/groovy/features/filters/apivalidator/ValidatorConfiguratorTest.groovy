package features.filters.apivalidator

import framework.ReposeLogSearch
import framework.ReposeValveTest
import framework.category.Slow
import org.junit.experimental.categories.Category
import org.rackspace.deproxy.Deproxy

@Category(Slow.class)
class ValidatorConfiguratorTest extends ReposeValveTest {

    def setupSpec() {
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort)
    }

    def cleanupSpec() {
        deproxy.shutdown()
    }

    def errorMessage = "WADL Processing Error:"

    def "when loading validators on startup, should work with local uri path"() {
        cleanLogDirectory()
        def List<String> wadlError;

        given: "repose is started using a non-uri path for the wadl, in this case the path generic_pass.wadl"
        repose.applyConfigs("features/filters/apivalidator/common", "features/filters/apivalidator/wadlpath/good")
        repose.start()
        sleep(10000)
        reposeLogSearch = new ReposeLogSearch(logFile);

        when: "a request is made using the api validator"
        def resp = deproxy.makeRequest([url: reposeEndpoint + "/test", method: "get", headers:['X-Roles':'test_user']])
        wadlError = reposeLogSearch.searchByString(errorMessage)

        then: "request returns a 404 and and no error wadl error is thrown"
        wadlError.size() == 0
        resp.getReceivedResponse().code == 404.toString()
        repose.stop()
        sleep(5000)
    }

    def "when loading validators on startup, should fail bad local uri path"() {
        cleanLogDirectory()
        def List<String> wadlError;

        given: "repose is started using a non-uri path for the wadl, in this case the path does_not_exist.wadl"
        repose.applyConfigs("features/filters/apivalidator/common", "features/filters/apivalidator/wadlpath/bad")
        repose.start()
        sleep(15000)
        reposeLogSearch = new ReposeLogSearch(logFile);

        when: "a request is made using the api validator"
        def resp = deproxy.makeRequest([url: reposeEndpoint + "/test", method: "get", headers:['X-Roles':'test_user']])
        wadlError = reposeLogSearch.searchByString(errorMessage)

        then: "wadl error is thrown"
        wadlError.size() == 2
        repose.stop()
    }
}
