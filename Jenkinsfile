#!/usr/bin/env groovy

stage("Performance Test") {
    def perfTestWithExtraVars = [
            ["filters/saml", ""],
            ["filters/scripting", "script_lang=python"],
            ["uses-cases/simple", ""]]
    def perfTestsToRun = [:]

    for (int index = 0; index < perfTestWithExtraVars.size(); index++) {
        def perfTest = perfTestWithExtraVars[index][0]
        def extraVars = perfTestWithExtraVars[index][1]
        def pipelineBranch = perfTest + (extraVars ? "-$extraVars" : "")

        perfTestsToRun[pipelineBranch] = {
            build(job: "mario-test-job", parameters: [
                    [$class: "StringParameterValue", name: "perf_test", value: perfTest],
                    [$class: "StringParameterValue", name: "extra_vars", value: extraVars]])
        }
    }

    parallel(perfTestsToRun)
}
