package features.core.classloader

import framework.ReposeConfigurationProvider
import framework.ReposeValveLauncher
import framework.ReposeValveTest
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain

/**
 * Created by dimi5963 on 1/5/15.
 */
class ClassLoaderTest extends ReposeValveTest {
    static int originServicePort
    static int reposePort
    static String url
    static ReposeConfigurationProvider reposeConfigProvider

    def "test class loader one"(){
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
        reposeConfigProvider.applyConfigs("features/core/classloader/one", params)

        repose.start(killOthersBeforeStarting: false,
                waitOnJmxAfterStarting: false)

        repose.waitForNon500FromUrl(url)
        //copy the bundle from /repose-aggregator/functional-tests/test-bundles/bundle-one/target/
        //and copy the bundle from /repose-aggregator/functional-tests/test-bundles/bundle-two/target/
        //to artifacts directory
        //set up config that has in system model:
        /*
        filter-one
        filter-two
         */
        //start repose with the launcher
        //make a request with header foo. validate that header bar returns
        //make a request with another header.  validate we get a failure back
        when: "make a request with the given header and value"
        def headers = [
                'Content-Length': '0'
        ]
        headers[headerName.toString()] = headerValue.toString()

        MessageChain mc = deproxy.makeRequest(url: url, headers: headers)


        then: "the request should keep headerName and headerValue case"
        mc.handlings.size() == 1
        mc.handlings[0].request.headers.contains(headerName)
        mc.handlings[0].request.headers.getFirstValue(headerName) == headerValue
    }

    def "test class loader two"(){
        //copy the bundle from /repose-aggregator/functional-tests/test-bundles/bundle-one/target/
        //and copy the bundle from /repose-aggregator/functional-tests/test-bundles/bundle-three/target/
        //to artifacts directory
        //set up config that has in system model:
        /*
        filter-one
        filter-three
         */
        //start repose with the launcher
        //make a request with header foo. validate that ClassNotFoundException is logged
    }

    def "test class loader three"(){
        //copy the bundle from /repose-aggregator/functional-tests/test-bundles/bundle-four/target/
        //and copy the bundle from /repose-aggregator/functional-tests/test-bundles/bundle-five/target/
        //to artifacts directory
        //set up config that has in system model:
        /*
        filter-one
        filter-two
         */
        //start repose with the launcher
        //make a request with header foo. validate that BAR is logged in repose.log
        //validate that BARRR is logged in repose.log
    }

    def cleanup(){
        if (repose) {
            repose.stop()
        }
        if (deproxy) {
            deproxy.shutdown()
        }
    }
}
