stage("Performance Test") {
    def perfTestWithExtraVars = [
            "filters/saml": "",
            "filters/scripting": "script_lang=python",
            "uses-cases/simple": ""]
    def perfTestsToRun = [:]

    def perfTestWithExtraVarsEntries = perfTestWithExtraVars.entrySet().toArray()
    for (int index = 0; index < perfTestWithExtraVarsEntries.size(); index++) {
        def perfTest = perfTestWithExtraVarsEntries[index].key
        def extraVars = perfTestWithExtraVarsEntries[index].value
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
