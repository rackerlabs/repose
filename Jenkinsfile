#!/usr/bin/env groovy

stage("Performance Test") {
    node("jdk8") {
        git(branch: "jenkins-pipeline-test", url: "https://github.com/rackerlabs/repose.git")
        stash(name: "script", includes: "test.sh,repose-aggregator/tests/performance-tests/")
    }

    def perfTestWithExtraVars = [
            ["filters/saml", ""],
            ["filters/scripting", "script_lang=python"],
            ["uses-cases/simple", ""]]
    def perfTestsToRun = [:]

    for (int index = 0; index < perfTestWithExtraVars.size(); index++) {
        def perfTest = perfTestWithExtraVars[index][0]
        def extraVars = perfTestWithExtraVars[index][1]
        def jobName = perfTest + (extraVars ? "-$extraVars" : "")

        perfTestsToRun[jobName] = {
            node("jdk8") {
                withEnv(["perf_test=$perfTest", "extra_vars=$extraVars"]) {
                    retry(3) {
                        deleteDir()
                        unstash("script")
                        sh "./test.sh"
                    }
                }
            }
        }
    }

    parallel(perfTestsToRun)
}
