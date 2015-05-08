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
package framework

import org.linkedin.util.clock.SystemClock
import org.rackspace.deproxy.PortFinder

import java.util.concurrent.TimeoutException

import static org.linkedin.groovy.util.concurrent.GroovyConcurrentUtils.waitForCondition

//TODO: Refactor this to just be a different command than the valve launcher :(
//So much duplicated logic
class ReposeContainerLauncher extends ReposeLauncher {

    int reposePort

    String clusterId
    String nodeId

    String containerJar
    String rootWarLocation
    String[] appWars
    String debugPort
    def classPaths = []

    def boolean debugEnabled
    def boolean doSuspend

    def clock = new SystemClock()
    def Process process

    def ReposeConfigurationProvider configurationProvider

    ReposeContainerLauncher(ReposeConfigurationProvider configurationProvider, String containerJar,
                            String clusterId, String nodeId,
                            String rootWarLocation, int reposePort, String... appWars) {
        this.configurationProvider = configurationProvider
        this.containerJar = containerJar
        this.clusterId = clusterId
        this.nodeId = nodeId
        this.reposePort = reposePort
        this.rootWarLocation = rootWarLocation

        this.appWars = appWars
    }

    @Override
    void start() {
        String configDirectory = configurationProvider.getReposeConfigDir()

        String webXmlOverrides = "-Dpowerapi-config-directory=${configDirectory}"
        if (clusterId != null) {
            webXmlOverrides += " -Drepose-cluster-id=${clusterId}"
        }
        if (nodeId != null) {
            webXmlOverrides += " -Drepose-node-id=${nodeId}"
        }

        if (debugEnabled) {
            if (!debugPort) {
                debugPort = PortFinder.Singleton.getNextOpenPort()
            }
            webXmlOverrides += " -Xdebug -Xrunjdwp:transport=dt_socket,address=${debugPort},server=y,suspend="
            if (doSuspend) {
                webXmlOverrides += "y"
                println("\n\n~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n\nConnect debugger to repose on port: ${debugPort}\n\n~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n\n")
            } else {
                webXmlOverrides += "n"
            }
        }

        def cmd = "java ${webXmlOverrides} -jar ${containerJar} -p ${reposePort} -w ${rootWarLocation} "

        if (appWars != null || appWars.length != 0) {
            for (String path : appWars) {
                cmd += " -war ${path}"
            }
        }
        println("Starting repose: ${cmd}")

        def th = new Thread({
            this.process = cmd.execute()
            // TODO: This should probably go somewhere else and not just be consumed to the garbage.
            this.process.consumeProcessOutput(System.out, System.err)
        });

        th.run()
        th.join()
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
            this.process.destroy()

            print("Waiting for Repose Container to shutdown")
            waitForCondition(clock, "${timeout}", '1s', {
                print(".")
                !isUp()
            })

            println()
        } catch (IOException ioex) {
            this.process.waitForOrKill(5000)
            killIfUp()
            if (throwExceptionOnKill) {
                throw new TimeoutException("An error occurred while attempting to stop Repose Controller. Reason: " + ioex.getMessage());
            }
        }
    }

    private void killIfUp() {
        String processes = TestUtils.getJvmProcesses()
        def regex = /(\d*) ROOT.war .*/
        def matcher = (processes =~ regex)
        if (matcher.size() > 0) {

            for (int i = 1; i <= matcher.size(); i++) {
                String pid = matcher[0][i]

                if (pid != null && !pid.isEmpty()) {
                    println("Killing running repose-valve process: " + pid)
                    Runtime rt = Runtime.getRuntime();
                    if (System.getProperty("os.name").toLowerCase().indexOf("windows") > -1)
                        rt.exec("taskkill " + pid.toInteger());
                    else
                        rt.exec("kill -9 " + pid.toInteger());
                }
            }
        }
    }

    @Override
    boolean isUp() {
        return TestUtils.getJvmProcesses().contains("ROOT.war")
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

    @Override
    void keepSandbox() {
        //Nothing?
    }
}
