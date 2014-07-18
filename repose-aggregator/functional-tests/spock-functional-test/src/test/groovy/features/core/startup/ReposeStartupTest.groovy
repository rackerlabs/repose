package features.core.startup
import framework.ReposeValveTest
import framework.category.Release
import org.apache.commons.io.FileUtils
import org.apache.http.client.ClientProtocolException
import org.apache.http.client.HttpClient
import org.apache.http.client.methods.HttpGet
import org.apache.http.impl.client.DefaultHttpClient
import org.junit.experimental.categories.Category
import org.linkedin.util.clock.SystemClock
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.PortFinder
import spock.lang.Unroll

import java.nio.file.Path
import java.util.concurrent.TimeoutException

import static org.linkedin.groovy.util.concurrent.GroovyConcurrentUtils.waitForCondition
/**
 * D-15183 Ensure passwords are not logged when in DEBUG mode and config files are updated.
 */
class ReposeStartupTest extends ReposeValveTest {

    def setupSpec() {
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort)
    }

    def cleanupSpec() {
        deproxy.shutdown()
    }

    static def params

    def "repose should start with installation configs"(){
        setup:
        def params = properties.getDefaultTemplateParams()
        def nextPort = PortFinder.Singleton.getNextOpenPort()

        //note: Order matters here. The common directory overwrites some of the configs from the core directory.
        //      This means that the core configs we provide may not get tested, but due to the structure of our tests,
        //      this is currently "hard" to fix.
        repose.configurationProvider.applyConfigs("../../../../installation/configs/core", params)
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("../../../../installation/configs/extensions", params)
        repose.configurationProvider.applyConfigs("../../../../installation/configs/filters", params)
        String systemModelTemp = "${repose.configurationProvider.reposeConfigDir}/system-model.cfg.xml.${nextPort}"
        String systemModelSource = "${repose.configurationProvider.reposeConfigDir}/system-model.cfg.xml"
        new File(systemModelTemp).withWriter {
            out ->
            new File(systemModelSource).eachLine {
                line ->
                    out << line.replaceAll("http-port=\"8080\"", "http-port=\"${nextPort}\"")
            }
        }
        FileUtils.copyFile(new File(systemModelTemp), new File(systemModelSource))

        repose.start()


        when:
        //todo: use a dynamic port (will require tinkering with [a copy of] the installation system-model).
        repose.waitForNon500FromUrl("http://localhost:${nextPort}")

        then:
        notThrown(TimeoutException)

        cleanup:
        repose.stop([throwExceptionOnKill: false])
    }

    @Category(Release)
    @Unroll("deploy and start #server.name")
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
                def execution = "ssh -t -oStrictHostKeyChecking=no ${server.ip} ${i}"
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
                def execution = "ssh -t -oStrictHostKeyChecking=no ${server.ip} ${i}"
                println execution
                def proc = """${execution}""".execute()
                proc.waitFor()
                println proc.in.text
                println proc.err.text
        }

        where:
        server << [
                new Server(
                        name: 'ubuntu 14.04', ip: '192.237.216.53',
                        deploymentSteps: deployDeb(), cleanupSteps: cleanupDeb()),
                new Server(
                        name: 'centos 6.5', ip: '192.237.212.239',
                        deploymentSteps: deployRpm(), cleanupSteps: cleanupRpm()),
// RHEL defaults to IBM jvm which doesn't support xerces by default
//                new Server(
//                        name: 'rhel 6.5', ip: '192.237.216.86',
//                        deploymentSteps: deployRpm(), cleanupSteps: cleanupRpm()),
                new Server(
                        name: 'debian 7', ip: '192.237.216.68',
                        deploymentSteps: deployDeb(), cleanupSteps: cleanupDeb())//,
// Fedora has filesystems package that conflicts with repose-valve in /usr/local/bin
//                new Server(
//                        name: 'fedora 20', ip: '192.237.216.166',
//                        deploymentSteps: deployRpm(), cleanupSteps: cleanupRpm())
        ]
    }

    static def deployDeb(){
        return [
                'sudo apt-get update -y',
                'sudo apt-get install openjdk-6-jdk -y',
                'sudo wget -O - http://repo.openrepose.org/debian/pubkey.gpg | sudo apt-key add -',
                'sudo sh -c \'echo "deb http://repo.openrepose.org/debian stable main" > /etc/apt/sources.list.d/openrepose.list\'',
                'sudo apt-get update -y',
                'sudo apt-get install -y repose-valve repose-filter-bundle repose-extensions-filter-bundle',
                'sudo /etc/init.d/repose-valve start'
        ]
    }

    static def deployRpm(){
        return [
                'sudo yum update -y',
                'sudo yum install java-1.6.0-openjdk -y',
                'sudo wget -O /etc/yum.repos.d/openrepose.repo http://repo.openrepose.org/el/openrepose.repo',
                'sudo yum update -y',
                'sudo yum install -y repose-valve repose-filters repose-filters repose-extension-filters',
                'sudo chmod +x /etc/init.d/repose-valve',
                'sudo /etc/init.d/repose-valve start',
                'sudo /etc/init.d/iptables stop',
                'sudo /etc/init.d/iptables save'
        ]
    }

    static def cleanupDeb(){
        return [
                'sudo killall java',
                'sudo apt-get -y purge repose-valve',
                'sudo rm -rf /usr/share/repose',
                'sudo rm -rf /etc/repose/*'
        ]
    }

    static def cleanupRpm(){
        return [
                'sudo killall java',
                'sudo yum -y remove repose-valve',
                'sudo rm -rf /usr/share/repose',
                'sudo rm -rf /etc/repose/*',
                'sudo /etc/init.d/iptables start',
                'sudo /etc/init.d/iptables save'
        ]
    }


}

class Server{
    def name, ip, deploymentSteps, cleanupSteps
}
