package features.filters.apivalidator.absolutePaths

import framework.ReposeLogSearch
import framework.ReposeValveTest
import org.rackspace.deproxy.Deproxy

/**
 * Created by jennyvo on 2/17/15.
 */
class ApiValidatorWadlAbsPathTest extends ReposeValveTest {

    def setupSpec() {
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort)
    }

    def setup() {
        cleanLogDirectory()
        reposeLogSearch = new ReposeLogSearch(logFile)
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

    def "when loading validators on startup, should also pass with related path config"() {

        given: "repose is started using a non-uri path for the wadl, in this case the path does_not_exist.wadl"
        def params = properties.getDefaultTemplateParams()
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/filters/apivalidator/common", params)
        repose.configurationProvider.applyConfigs("features/filters/apivalidator/absolutepathconfig", params)
        repose.configurationProvider.applyConfigs("features/filters/apivalidator/absolutepathconfig/relatedconfig", params)
        repose.start([waitOnJmxAfterStarting: false])
        repose.waitForNon500FromUrl(reposeEndpoint)

        when: "a request is made using the api validator"
        def resp = deproxy.makeRequest([url: reposeEndpoint + "/test", method: "get", headers:['X-Roles':'test_user']])
        def List<String> wadlError;
        wadlError = reposeLogSearch.searchByString(errorMessage)

        then: "wadl error is thrown"
        wadlError.size() == 0
    }

    def "when loading validators on startup, should work with absolute wadl path"() {

        given: "repose is started using a non-uri path for the wadl, in this case the path generic_pass.wadl"
        def params = properties.getDefaultTemplateParams()
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/filters/apivalidator/common", params)
        repose.configurationProvider.applyConfigs("features/filters/apivalidator/absolutepathconfig", params)
        repose.start([waitOnJmxAfterStarting: false])
        repose.waitForNon500FromUrl(reposeEndpoint)

        when: "a request is made using the api validator"
        def resp = deproxy.makeRequest([url: reposeEndpoint + "/test", method: "get", headers:['X-Roles':'test_user']])
        def List<String> wadlError;
        wadlError = reposeLogSearch.searchByString(errorMessage)

        then: "request returns a 404 and and no error wadl error is thrown"
        wadlError.size() == 0
        resp.getReceivedResponse().code == 404.toString()
    }
}
