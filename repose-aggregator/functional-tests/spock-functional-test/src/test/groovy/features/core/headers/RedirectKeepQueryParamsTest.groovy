package features.core.headers

import framework.ReposeConfigurationProvider
import framework.ReposeValveLauncher
import framework.ReposeValveTest
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain
import org.rackspace.deproxy.Response
import spock.lang.Unroll

/**
 * Created by jennyvo on 8/27/14.
 * Test with redirect url from header keep query params
 */
class RedirectKeepQueryParamsTest extends ReposeValveTest{

    static int originServicePort
    static int reposePort
    static String url
    static ReposeConfigurationProvider reposeConfigProvider

    def setupSpec() {
        deproxy = new Deproxy()
        originServicePort = properties.targetPort
        deproxy.addEndpoint(originServicePort)

        reposePort = properties.reposePort
        url = "http://localhost:${reposePort}"

        reposeConfigProvider = new ReposeConfigurationProvider(configDirectory, configTemplates)
        repose = new ReposeValveLauncher(
                reposeConfigProvider,
                properties.getReposeJar(),
                url,
                properties.getConfigDirectory(),
                reposePort
        )
        repose.enableDebug()

        def params = properties.getDefaultTemplateParams()

        reposeConfigProvider.cleanConfigDirectory()
        reposeConfigProvider.applyConfigs("common", params)
        reposeConfigProvider.applyConfigs("features/core/headers", params)

        repose.start(killOthersBeforeStarting: false,
                waitOnJmxAfterStarting: false)

        repose.waitForNon500FromUrl(url)
    }

    @Unroll("#newlocation")
    def "When endpoint using resp with 302 redirect a new location the query params should be kept" () {
        when: "make a request with the given header and value"
        def headers = [
                'Content-Length': '0',
                'Location' : newlocation
        ]

        MessageChain mc = deproxy.makeRequest(url: url+queryparam, defaultHandler: {new Response(302, null, headers) })

        then:
        mc.handlings.size() == 1
        mc.handlings[0].response.headers.getFirstValue("Location") == newlocation

        where:
        newlocation                                     | queryparam
        "http://myhost.com/test/test?query=info"        | "/test?query=info"
        "http://myhost.com/details/details?query=all"   | "/details?query=all"
    }
}
