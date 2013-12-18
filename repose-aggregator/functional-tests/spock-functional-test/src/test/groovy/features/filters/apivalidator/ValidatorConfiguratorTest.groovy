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

    def setup() {
        cleanLogDirectory()
    }

    def cleanup() {

        if (repose) {
            repose.stop()
        }
    }

    def cleanupSpec() {
        deproxy.shutdown()
    }

    def errorMessage = "WADL Processing Error:"

    def "when loading validators on startup, should work with local uri path"() {

        given: "repose is started using a non-uri path for the wadl, in this case the path generic_pass.wadl"
        def params = properties.getDefaultTemplateParams()
        repose.configurationProvider.applyConfigsRuntime("common", params)
        repose.configurationProvider.applyConfigsRuntime("features/filters/apivalidator/common", params)
        repose.configurationProvider.applyConfigsRuntime("features/filters/apivalidator/wadlpath/good", params)
        repose.start()
        sleep(10000)
        reposeLogSearch = new ReposeLogSearch(logFile);

        when: "a request is made using the api validator"
        def resp = deproxy.makeRequest([url: reposeEndpoint + "/test", method: "get", headers:['X-Roles':'test_user']])
        def List<String> wadlError;
        wadlError = reposeLogSearch.searchByString(errorMessage)

        then: "request returns a 404 and and no error wadl error is thrown"
        wadlError.size() == 0
        resp.getReceivedResponse().code == 404.toString()
    }

    def "when loading validators on startup, should fail bad local uri path"() {

        given: "repose is started using a non-uri path for the wadl, in this case the path does_not_exist.wadl"
        def params = properties.getDefaultTemplateParams()
        repose.configurationProvider.applyConfigsRuntime("common", params)
        repose.configurationProvider.applyConfigsRuntime("features/filters/apivalidator/common", params)
        repose.configurationProvider.applyConfigsRuntime("features/filters/apivalidator/wadlpath/bad", params)
        repose.start()
        sleep(15000)
        reposeLogSearch = new ReposeLogSearch(logFile);

        when: "a request is made using the api validator"
        def resp = deproxy.makeRequest([url: reposeEndpoint + "/test", method: "get", headers:['X-Roles':'test_user']])
        def List<String> wadlError;
        wadlError = reposeLogSearch.searchByString(errorMessage)

        then: "wadl error is thrown"
        wadlError.size() == 2
    }
}
