/*
 * _=_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_=
 * Repose
 * _-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-
 * Copyright (C) 2010 - 2015 Rackspace US, Inc.
 * _-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * =_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_=_
 */
package org.openrepose.framework.test

import org.linkedin.util.clock.SystemClock
import org.openrepose.commons.config.parser.jaxb.JaxbConfigurationParser
import org.openrepose.commons.config.resource.impl.BufferedURLConfigurationResource
import org.openrepose.core.filter.SystemModelInterrogator
import org.openrepose.core.systemmodel.config.SystemModel
import org.openrepose.framework.test.client.jmx.JmxClient

import javax.management.ObjectName
import java.util.concurrent.TimeoutException

import static org.linkedin.groovy.util.concurrent.GroovyConcurrentUtils.waitForCondition

class ReposeValveLauncher extends ReposeLauncher {

    boolean debugEnabled
    boolean doSuspend
    String reposeJar
    String configDir

    def clock = new SystemClock()

    def reposeEndpoint
    int reposePort

    JmxClient jmx
    def jmxPort = null
    def debugPort = null
    def classPaths = []
    def additionalEnvironment = [:]

    ReposeConfigurationProvider configurationProvider

    ReposeValveLauncher(ReposeConfigurationProvider configurationProvider,
                        TestProperties properties) {
        this(configurationProvider,
                properties.reposeJar,
                properties.reposeEndpoint,
                properties.configDirectory,
                properties.reposePort
        )
    }

    ReposeValveLauncher(ReposeConfigurationProvider configurationProvider,
                        String reposeJar,
                        String reposeEndpoint,
                        String configDir,
                        int reposePort) {
        this.configurationProvider = configurationProvider
        this.reposeJar = reposeJar
        this.reposeEndpoint = reposeEndpoint
        this.reposePort = reposePort
        this.configDir = configDir
    }

    @Override
    void start() {
        this.start([:])
    }

    void start(Map params) {

        boolean killOthersBeforeStarting = true
        if (params.containsKey("killOthersBeforeStarting")) {
            killOthersBeforeStarting = params.killOthersBeforeStarting
        }
        boolean waitOnJmxAfterStarting = true
        if (params.containsKey("waitOnJmxAfterStarting")) {
            waitOnJmxAfterStarting = params.waitOnJmxAfterStarting
        }

        String clusterId = params.get('clusterId', "")
        String nodeId = params.get('nodeId', "")

        start(killOthersBeforeStarting, waitOnJmxAfterStarting, clusterId, nodeId)
    }

    /**
     * TODO: need to know what node in the system model we care about. There might be many, for multiple local node testing...
     * @param killOthersBeforeStarting
     * @param waitOnJmxAfterStarting
     */
    void start(boolean killOthersBeforeStarting, boolean waitOnJmxAfterStarting, String clusterId, String nodeId) {

        File jarFile = new File(reposeJar)
        if (!jarFile.exists() || !jarFile.isFile()) {
            throw new FileNotFoundException("Missing or invalid Repose Valve Jar file.")
        }

        File configFolder = new File(configDir)
        if (!configFolder.exists() || !configFolder.isDirectory()) {
            throw new FileNotFoundException("Missing or invalid configuration folder.")
        }

        if (killOthersBeforeStarting) {
            waitForCondition(clock, '5s', '1s') {
                killIfUp()
                !isUp()
            }
        }

        def jmxprops = ""
        def debugProps = ""
        def jacocoProps = ""
        def classPath = ""

        if (debugEnabled) {
            if (!debugPort) {
                debugPort = PortFinder.instance.getNextOpenPort()
            }
            debugProps = "-Xdebug -Xrunjdwp:transport=dt_socket,address=${debugPort},server=y,suspend="
            if (doSuspend) {
                debugProps += "y"
                println("\n\n~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n\nConnect debugger to repose on port: ${debugPort}\n\n~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n\n")
            } else {
                debugProps += "n"
            }
        }

        if (!jmxPort) {
            jmxPort = PortFinder.instance.getNextOpenPort()
        }
        jmxprops = "-Dspock=spocktest -Dcom.sun.management.jmxremote.port=${jmxPort} -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.ssl=false -Dcom.sun.management.jmxremote.local.only=true"

        if (!classPaths.isEmpty()) {
            classPath = "-cp " + (classPaths as Set).join(";")
        }

        if (System.getProperty('jacocoArguments')) {
            jacocoProps = System.getProperty('jacocoArguments')
        }

        //TODO: possibly add a -Dlog4j.configurationFile to the guy so that we can load a different log4j config for early logging

        //Prepended the JUL logging manager from log4j2 so I can capture JUL logs, which are things in the JVM (like JMX)
        def cmd = "java -Djava.util.logging.manager=org.apache.logging.log4j.jul.LogManager -Xmx1536M -Xms1024M -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/tmp/dump-${debugPort}.hprof $classPath $debugProps $jmxprops $jacocoProps -jar $reposeJar -c $configDir"
        println("Starting repose: $cmd")

        def th = new Thread({
            //Construct a new environment, including all from the previous, and then overriding with our new one
            def newEnv = new HashMap<String, String>()
            newEnv.putAll(System.getenv())

            additionalEnvironment.each { k, v ->
                newEnv.put(k, v) //Should override anything, if there's anything to override
            }
            def envList = newEnv.collect { k, v -> "$k=$v" }
            this.process = cmd.execute(envList, null)
            this.process.consumeProcessOutput(System.out, System.err)
        })

        th.run()
        th.join()

        def jmxUrl = "service:jmx:rmi:///jndi/rmi://localhost:${jmxPort}/jmxrmi"

        if (waitOnJmxAfterStarting) {
            waitForCondition(clock, '60s', '1s') {
                connectViaJmxRemote(jmxUrl)
            }
            if (clusterId && nodeId) {
                print("Waiting for repose node: ${clusterId}:${nodeId} to start: ")
            } else {
                print("Waiting for repose auto-guessed node to start: ")
            }

            waitForCondition(clock, "${MAX_STARTUP_TIME}s", '1s') {
                isReposeNodeUp(clusterId, nodeId)
            }
        }
    }

    def connectViaJmxRemote(jmxUrl) {
        try {
            jmx = new JmxClient(jmxUrl)
            return true
        } catch (Exception ex) {
            return false
        }
    }

    @Override
    void stop() {
        this.stop([:])
    }

    void stop(Map params) {
        def timeout = params?.timeout ?: 45000
        def throwExceptionOnKill = true

        if (params.containsKey("throwExceptionOnKill")) {
            throwExceptionOnKill = params.throwExceptionOnKill
        }

        stop(timeout, throwExceptionOnKill)
    }

    void stop(int timeout, boolean throwExceptionOnKill) {
        try {
            println("Stopping Repose");
            this.process?.destroy()

            print("Waiting for Repose to shutdown")
            waitForCondition(clock, "${timeout}", '1s') {
                print(".")
                !isUp()
            }

            println()
        } catch (IOException ioex) {
            this.process.waitForOrKill(5000)
            killIfUp()
            if (throwExceptionOnKill) {
                throw new TimeoutException("An error occurred while attempting to stop Repose Controller. Reason: ${ioex.getMessage()}", ioex)
            }
        }
    }

    @Override
    void enableDebug() {
        this.debugEnabled = true
    }

    @Override
    void enableSuspend() {
        this.debugEnabled = true
        this.doSuspend = true
    }

    @Override
    void addToClassPath(String path) {
        classPaths.add(path)
    }

    /**
     * This takes a single string and will append it to the list of environment vars to be set for the .execute() method
     * Following docs from: http://groovy.codehaus.org/groovy-jdk/java/lang/String.html#execute%28java.util.List,%20java.io.File%29
     * @param environmentPair
     */
    void addToEnvironment(String key, String value) {
        additionalEnvironment.put(key, value)
    }

    /**
     * TODO: introspect the system model for expected filters in filter chain and validate that they
     * are all present and accounted for
     * @return
     */
    private boolean isReposeNodeUp(String clusterId, String nodeId) {
        print('.')

        //Marshal the SystemModel if possible, and try to get information from it about which node we care about....
        def systemModelFile = configurationProvider.getSystemModel()
        def systemModelXSDUrl = getClass().getResource("/META-INF/schema/system-model/system-model.xsd")
        def parser = new JaxbConfigurationParser(SystemModel.class, systemModelXSDUrl, this.getClass().getClassLoader())
        def systemModel = parser.read(new BufferedURLConfigurationResource(systemModelFile.toURI().toURL()))

        //If the systemModel didn't validate, we're going to toss an exception here, which is fine

        //Get the systemModel cluster/node, if there's only one we can guess. If there's many, bad things happen.
        if (clusterId == "" || nodeId == "") {
            Map<String, List<String>> clusterNodes = SystemModelInterrogator.allClusterNodes(systemModel)

            if (clusterNodes.size() == 1) {
                clusterId = clusterNodes.keySet().toList().first()
                if (clusterNodes.get(clusterId).size() == 1) {
                    nodeId = clusterNodes.get(clusterId).first()
                } else {
                    throw new Exception("Unable to guess what nodeID you want in cluster: " + clusterId)
                }
            } else {
                throw new Exception("Unable to guess what clusterID you want!")
            }
        }

        // First query for the mbean.  The name of the mbean is partially configurable, so search for a match.
        def HashSet cfgBean = (HashSet) jmx.getMBeans('*:001=\"org\",002=\"openrepose\",003=\"core\",004=\"services\",005=\"jmx\",006=\"ConfigurationInformation\"')
        if (cfgBean == null || cfgBean.isEmpty()) {
            return false
        }

        def String beanName = cfgBean.iterator().next().name.toString()

        //Doing the JMX invocation here, because it's kinda ugly
        Object[] opParams = [nodeId]
        String[] opSignature = [String.class.getName()]

        //Invoke the 'is repose ready' bit on it
        def nodeIsReady = jmx.server.invoke(new ObjectName(beanName), "isNodeReady", opParams, opSignature)
        return nodeIsReady
    }

    @Override
    boolean areAnyUp() {
        println TestUtils.getJvmProcesses()
        return TestUtils.getJvmProcesses().contains("repose.jar")
    }

    private static void killIfUp() {
        String processes = TestUtils.getJvmProcesses()
        def regex = /(\d*) repose.jar .*spocktest .*/
        def matcher = (processes =~ regex)
        if (matcher.size() > 0) {

            for (int i = 1; i <= matcher.size(); i++) {
                String pid = matcher[0][i]

                if (pid != null && !pid.isEmpty()) {
                    println("Killing running repose process: " + pid)
                    Runtime rt = Runtime.getRuntime();
                    if (System.getProperty("os.name").toLowerCase().indexOf("windows") > -1)
                        rt.exec("taskkill " + pid.toInteger());
                    else
                        rt.exec("kill -9 " + pid.toInteger());
                }
            }
        }
    }
}
