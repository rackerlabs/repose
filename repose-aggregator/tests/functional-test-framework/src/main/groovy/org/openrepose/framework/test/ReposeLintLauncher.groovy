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

import java.util.concurrent.TimeoutException

import static org.linkedin.groovy.util.concurrent.GroovyConcurrentUtils.waitForCondition

class ReposeLintLauncher {

    boolean debugEnabled
    boolean doSuspend
    String reposeLintJar
    String configDir
    String reposeVer
    String roleName
    String logFileLocation

    ReposeConfigurationProvider configurationProvider

    Process process

    def clock = new SystemClock()

    def debugPort = null
    def additionalEnvironment = [:]

    ReposeLintLauncher(ReposeConfigurationProvider configurationProvider,
                       TestProperties properties) {
        this(configurationProvider,
                properties.reposeLintJar,
                properties.configDirectory,
                properties.reposeVersion,
                properties.userRole,
                properties.reposeLintLogFile
        )
    }

    ReposeLintLauncher(ReposeConfigurationProvider configurationProvider,
                       String reposeLintJar,
                       String configDir,
                       String reposeVer,
                       String userRole,
                       String logFileLocation) {
        TestProperties
        this.configurationProvider = configurationProvider
        this.reposeLintJar = reposeLintJar
        this.configDir = configDir
        this.reposeVer = reposeVer
        this.roleName = userRole
        this.logFileLocation = logFileLocation
    }

    void start(String command) {
        File jarFile = new File(reposeLintJar)
        if (!jarFile.exists() || !jarFile.isFile()) {
            throw new FileNotFoundException("Missing or invalid Repose Lint Jar file.")
        }

        File configFolder = new File(configDir)
        if (!configFolder.exists() || !configFolder.isDirectory()) {
            throw new FileNotFoundException("Missing or invalid configuration folder.")
        }

        File logFile = new File(logFileLocation)
        if (!logFile.exists()) {
            logFile.getParentFile()?.mkdirs()
            logFile.createNewFile()
        }
        PrintWriter logWriter = new PrintWriter(logFile)

        def debugProps = ""

        if (debugEnabled) {
            if (!debugPort) {
                debugPort = PortFinder.instance.getNextOpenPort()
            }
            debugProps = "-Xdebug -Xrunjdwp:transport=dt_socket,address=${debugPort},server=y,suspend="
            if (doSuspend) {
                debugProps += "y"
                println("\n\n~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n\nConnect debugger to repose-lint on port: ${debugPort}\n\n~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n\n")
            } else {
                debugProps += "n"
            }
        }
        if (reposeVer.contains("~SNAPSHOT"))
            reposeVer = reposeVer - "~SNAPSHOT"

        def cmd = "java $debugProps -jar $reposeLintJar $command -r $reposeVer -c $configDir -v --role $roleName"
        println("Running repose-lint with the following command:")
        println(cmd)

        //Construct a new environment, including all from the previous, and then overriding with our new one
        def newEnv = new HashMap<String, String>()
        newEnv.putAll(System.getenv())

        additionalEnvironment.each { k, v ->
            newEnv.put(k, v) //Should override anything, if there's anything to override
        }
        def envList = newEnv.collect { k, v -> "$k=$v" }
        process = cmd.execute(envList, null)
        process.consumeProcessOutput(logWriter, logWriter)
        // todo: if repose-lint does not terminate, neither does the test
        process.waitFor()

        logWriter.flush()
        logWriter.close()
    }

    void stop(int timeout = 45000, boolean throwExceptionOnKill = true) {
        try {
            println("Stopping Repose Lint");
            this.process?.destroy()

            print("Waiting for Repose Lint to shutdown")
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
        } finally {
            configurationProvider.cleanConfigDirectory()
        }
    }

    void enableDebug() {
        this.debugEnabled = true
    }

    void enableSuspend() {
        this.debugEnabled = true
        this.doSuspend = true
    }

    /**
     * This takes a single string and will append it to the list of environment vars to be set for the .execute() method
     * Following docs from: http://groovy.codehaus.org/groovy-jdk/java/lang/String.html#execute%28java.util.List,%20java.io.File%29
     * @param environmentPair
     */
    void addToEnvironment(String key, String value) {
        additionalEnvironment.put(key, value)
    }

    public static boolean isUp() {
        println TestUtils.getJvmProcesses()
        return TestUtils.getJvmProcesses().contains("repose-lint.jar")
    }

    private static void killIfUp() {
        String processes = TestUtils.getJvmProcesses()
        def regex = /(\d*) repose-lint.jar .*spocktest .*/
        def matcher = (processes =~ regex)
        if (matcher.size() > 0) {
            for (int i = 1; i <= matcher.size(); i++) {
                String pid = matcher[0][i]

                if (pid != null && !pid.isEmpty()) {
                    println("Killing running repose-lint process: " + pid)
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
