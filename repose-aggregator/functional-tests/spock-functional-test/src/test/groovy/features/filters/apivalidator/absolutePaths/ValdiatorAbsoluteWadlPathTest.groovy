package features.filters.apivalidator.absolutePaths

import framework.ReposeLogSearch
import framework.ReposeValveTest
import framework.category.Slow
import org.apache.commons.io.FileUtils
import org.junit.experimental.categories.Category
import org.rackspace.deproxy.Deproxy

import java.nio.file.Files

class ValdiatorAbsoluteWadlPathTest extends ReposeValveTest {

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

    def "when loading validators on startup, it can find a wadl when given an absolute path"() {

        given: "Repose is started pointing at an absolute path to a wadl"
        def params = properties.getDefaultTemplateParams()
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/filters/apivalidator/common", params)
        //Get me a temp dir to use for my file
        def dir = Files.createTempDirectory("wadl")
        dir.toFile().deleteOnExit()

        def wadlFile = new File(dir.toFile(), "generic_path.wadl")
        wadlFile.deleteOnExit()
        //Copy the generic_pass.wadl to the temp dir
        FileUtils.copyFile(
                new File(repose.configurationProvider.configTemplatesDir, "/features/filters/apivalidator/absolutePath/generic_pass.wadl"),
                wadlFile
        )

        //Create the validator config speshul this time
        def validatorConfig = new File(repose.configurationProvider.reposeConfigDir, "validator.cfg.xml")
        validatorConfig.write(
                """<?xml version="1.0" encoding="UTF-8"?>
<validators multi-role-match="true" xmlns='http://docs.openrepose.org/repose/validator/v1.0'>
  <validator role="test_user" wadl="${wadlFile.getAbsolutePath()}" default="false" />
</validators>
"""
        )


        repose.start([waitOnJmxAfterStarting: false])
        repose.waitForNon500FromUrl(reposeEndpoint)

        when: "a request is made using the api validator"
        def resp = deproxy.makeRequest([url: reposeEndpoint + "/test", method: "get", headers: ['X-Roles': 'test_user']])
        def List<String> wadlError;
        wadlError = reposeLogSearch.searchByString(errorMessage)

        then: "request returns a 404 and and no error wadl error is thrown"
        wadlError.size() == 0
        resp.getReceivedResponse().code == 404.toString()
    }

    def "when loading validators on startup, it will fail when it cannot find a wadl when given an absolute path"() {

        given: "Repose is started pointing at an absolute path to a wadl"
        def params = properties.getDefaultTemplateParams()
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/filters/apivalidator/common", params)
        //Get me a temp dir to use for my file
        def dir = Files.createTempDirectory("wadl")
        dir.toFile().deleteOnExit()

        def wadlFile = new File(dir.toFile(), "generic_path.wadl")
        wadlFile.deleteOnExit()
        //Don't actually create that wadl file, so it'll not exist

        //Create the validator config speshul this time
        def validatorConfig = new File(repose.configurationProvider.reposeConfigDir, "validator.cfg.xml")
        validatorConfig.write(
                """<?xml version="1.0" encoding="UTF-8"?>
<validators multi-role-match="true" xmlns='http://docs.openrepose.org/repose/validator/v1.0'>
  <validator role="test_user" wadl="${wadlFile.getAbsolutePath()}" default="false" />
</validators>
"""
        )


        repose.start([waitOnJmxAfterStarting: false])
        repose.waitForNon500FromUrl(reposeEndpoint)

        when: "a request is made using the api validator"
        def resp = deproxy.makeRequest([url: reposeEndpoint + "/test", method: "get", headers: ['X-Roles': 'test_user']])
        def List<String> wadlError;
        wadlError = reposeLogSearch.searchByString(errorMessage)

        then: "wadl error is thrown"
        wadlError.size() == 2
    }
}
