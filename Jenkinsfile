stage("Performance Test") {
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
                        sh "./test.sh"
                    }
                }
            }
        }
    }

    parallel(perfTestsToRun)
}
