package features.core.startup

import framework.ReposeValveTest
import framework.category.Bug
import framework.category.Slow
import org.apache.http.client.ClientProtocolException
import org.apache.http.client.HttpClient
import org.apache.http.client.methods.HttpGet
import org.apache.http.impl.client.DefaultHttpClient
import org.linkedin.util.clock.SystemClock
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain
import org.rackspace.deproxy.PortFinder
import org.junit.experimental.categories.Category
import spock.lang.Ignore

import static org.linkedin.groovy.util.concurrent.GroovyConcurrentUtils.waitForCondition

/**
 * D-15183 Ensure passwords are not logged when in DEBUG mode and config files are updated.
 */
class ReposeStartup extends ReposeValveTest {

    static def List servers = [
        new Server('ubuntu', '198.101.159.216', deployDeb(), cleanupDeb()),
        new Server('centos', '198.61.176.151', deployRpm(), cleanupRpm()),
        new Server('rhel', '198.61.179.61', deployRpm(), cleanupRpm()),
        new Server('debian', '198.61.224.119', deployDeb(), cleanupDeb())
    ]

    def setupSpec() {
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort)
    }

    def cleanupSpec() {
        deproxy.shutdown()
    }

    static def params

    @Category(Bug)
    def "start repose with installation configs"(){
        given:
        def params = properties.getDefaultTemplateParams()
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/core/startup", params)
        repose.configurationProvider.applyConfigs("../../../../installation/configs/filters", params)
        repose.configurationProvider.applyConfigs("../../../../installation/configs/extensions", params)
        repose.start()
        waitUntilReadyToServiceRequests()

        when:
        MessageChain mc = deproxy.makeRequest([method: 'GET'])

        then:
        mc.receivedResponse.code == '301'

        cleanup:
        repose.stop()
    }

    @Ignore
    def "deploy and start repose - snapshots"() {
        //1. copy over repose to server - SNAPSHOT
        //2. call until get back a 301
        //3. update system-model to include all filters
        //4. call until get back a non 500
        //5. remove repose
        //TODO: retention policy
    }

    @Category(Slow)
    def "deploy and start repose - release"() {
        //1. create and deploy repose with configurations - RELEASE
        //2. call until get back a 200
        //3. update system-model to include all filters
        //4. call until get back a non 500
        //5. remove repose
        print server.name
        print server.ip

        server.deploymentSteps.each {
            i ->
                def execution = "ssh root@${server.ip} ${i}"
                println execution
                def proc = """${execution}""".execute()
                proc.waitFor()
                println proc.in.text
                println proc.err.text
        }

        def clock = new SystemClock()

        def url = "http://${server.ip}:8080/"

        print("Waiting for repose to start at ${url} ")
        when:
        waitForCondition(clock, "60s", "2s") {
            try {
                print(".")
                HttpClient client = new DefaultHttpClient()
                int status = client.execute(new HttpGet(url)).statusLine.statusCode
                println status
                status != 500
            } catch (IOException ignored) {
            } catch (ClientProtocolException ignored) {
            }
        }
        println()

        then:
        noExceptionThrown()

        cleanup:
        server.cleanupSteps.each {
            i ->
                def execution = "ssh root@${server.ip} ${i}"
                println execution
                def proc = """${execution}""".execute()
                proc.waitFor()
                println proc.in.text
                println proc.err.text
        }

        where:
        server << getServers()
    }

    static def deployDeb(){
        return [
                'wget -O - http://repo.openrepose.org/debian/pubkey.gpg | sudo apt-key add -',
                'echo "deb http://repo.openrepose.org/debian stable main" > /etc/apt/sources.list.d/openrepose.list',
                'sudo apt-get update',
                'sudo apt-get install -y repose-valve repose-filter-bundle repose-extensions-filter-bundle',
                '/etc/init.d/repose-valve start'
        ]
    }

    static def deployRpm(){
        return [
                'wget -O /etc/yum.repos.d/openrepose.repo http://repo.openrepose.org/el/openrepose.repo',
                'yum install -y repose-valve repose-filters repose-filters repose-extension-filters',
                '/etc/init.d/repose-valve start'
        ]
    }

    static def cleanupDeb(){
        return [
                'killall java',
                'sudo apt-get -y purge repose-valve',
                'sudo rm -rf /usr/share/repose',
                'sudo rm -rf /etc/repose/*'
        ]
    }

    static def cleanupRpm(){
        return [
                'killall java',
                'sudo yum -y remove repose-valve',
                'sudo rm -rf /usr/share/repose',
                'sudo rm -rf /etc/repose/*'
        ]
    }


}

class Server{
    def name, ip, deploymentSteps, cleanupSteps

    Server(name, ip, deploymentSteps, cleanupSteps){
        this.name = name
        this.ip = ip
        this.deploymentSteps = deploymentSteps
        this.cleanupSteps = cleanupSteps
    }
}
