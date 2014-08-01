package features.filters.uritranslation

import framework.ReposeValveTest
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain

class UriTranslationTest extends ReposeValveTest {
    def setupSpec() {
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort)

        def params = properties.getDefaultTemplateParams()
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/filters/translation/common", params)
        repose.configurationProvider.applyConfigs("features/filters/translation/uri", params)
        repose.start()
        repose.waitForNon500FromUrl(getReposeEndpoint())
    }

    def "path should be converted to query parameters"() {
        when:
        MessageChain mc = deproxy.makeRequest(getReposeEndpoint() + "/FL/Colier")

        then:
        mc.getHandlings().get(0).getRequest().getPath() == "/?state=FL&county=Colier"
    }
}
