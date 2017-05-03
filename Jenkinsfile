stage("Performance Test") {
    def perfTestWithExtraVars = [
            ["filters/saml", ""],
            ["filters/scripting", "script_lang=python"],
            ["uses-cases/simple", ""]]
    def perfTestsToRun = [:]

    for (int index = 0; index < perfTestWithExtraVars.size(); index++) {
        def (perfTest, extraVars) = perfTestWithExtraVars[index]
        def jobName = perfTest + (extraVars ? "-$extraVars" : "")

        perfTestsToRun[jobName] = {
            node("jdk8") {
                withEnv(["perf_test=$perfTest", "extra_vars=$extraVars"]) {
                    retry(3) {
                        sh "./test.sh"
                    }
                }
            }
        }
    }

    parallel(perfTestsToRun)
}
