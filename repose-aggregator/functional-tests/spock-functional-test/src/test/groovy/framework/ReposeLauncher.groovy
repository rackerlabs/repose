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

import org.apache.commons.io.FileUtils
import org.apache.http.client.ClientProtocolException
import org.apache.http.client.HttpClient
import org.apache.http.client.methods.HttpGet
import org.apache.http.impl.client.DefaultHttpClient
import org.linkedin.util.clock.SystemClock

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

import static org.linkedin.groovy.util.concurrent.GroovyConcurrentUtils.waitForCondition

abstract class ReposeLauncher {

    ReposeLauncher(TestProperties testProps) {
        //Internal testProperties, so I can always have the build directory
        //Sadly this isn't always passed in, and so it's a hot mess
        sandboxPath = Files.createTempDirectory(Paths.get(testProps.projectBuildDirectory), "reposeValveTest")

        testProps.configDirectory = sandboxPath.resolve("configs").toString()
        testProps.reposeHome = sandboxPath.toString()

        //Resolve all the jar files and war files to the new sandboxPath
        //Sadly this is hardcoded, because things are dumb
        //This mutates stuff in the testProps, mostly for backwards compatibility :(
        testProps.glassfishJar = sandboxPath.resolve(fileNamePath(testProps.glassfishJar))
        testProps.tomcatJar = sandboxPath.resolve(fileNamePath(testProps.tomcatJar))
        testProps.mocksWar = sandboxPath.resolve(fileNamePath(testProps.mocksWar))
        testProps.reposeJar = sandboxPath.resolve(fileNamePath(testProps.reposeJar))
        testProps.reposeRootWar = sandboxPath.resolve(fileNamePath(testProps.reposeRootWar))

        this.configurationProvider = new ReposeConfigurationProvider(testProps)
        this.configDir = testProps.configDirectory
        this.reposeLogSearch = new ReposeLogSearch(sandboxPath.resolve("logs/repose.log").toString()) //TODO: make this a bit more configurable

        prepare()
    }

    static def fileNamePath(String wat) {
        Paths.get(wat).getFileName().toString()
    }

    //Where the valve will be started in, stuff's gotta be copied into here, or linked
    Path sandboxPath
    ReposeLogSearch reposeLogSearch
    ReposeConfigurationProvider configurationProvider

    boolean keepSandbox = false
    String configDir
    boolean debugEnabled
    boolean doSuspend
    Process process
    def classPaths = []
    SystemClock clock = new SystemClock() //TODO: use eventually instead


    /**
     * Prepare this ReposeLauncher's sandbox directory and get everything set up
     * Prepare should be idempotent, so if it's hit twice, no biggie
     * Doesn't matter if it's container or valve, this is the same
     * @return
     */
    def prepare() {
        //Set up a repose root in the build directory I think...
        def itp = new TestProperties()

        //Make sure we have this root directory
        FileUtils.forceMkdir(sandboxPath.toFile())
        //Create all the things from the repose_home into here again

        //We'll use a HardLink, so that it's fast on the filesystem for things that are read only (artifacts!)
        Path artifactsPath = sandboxPath.resolve("artifacts")
        Path configPath = sandboxPath.resolve("configs")
        Path logsPath = sandboxPath.resolve("logs")

        FileUtils.forceMkdir(artifactsPath.toFile())
        FileUtils.forceMkdir(configPath.toFile())
        FileUtils.forceMkdir(logsPath.toFile())

        //Copy all artifacts from the repose_home/artifacts directory
        Path reposeHome = Paths.get(itp.getReposeHome())
        FileUtils.listFiles(new File(reposeHome.toFile(), "artifacts"), ["ear"] as String[], false).toList().each { artifact ->
            Path existing = artifact.toPath()
            Path newLink = artifactsPath.resolve(artifact.getName())
            Files.createLink(newLink, existing) //LINK IT!
        }

        //link the root artifacts
        FileUtils.listFiles(reposeHome.toFile(), ["war", "jar"] as String[], false).toList().each { rootArtifact ->
            Path existing = rootArtifact.toPath()
            Path newLink = sandboxPath.resolve(rootArtifact.getName())
            Files.createLink(newLink, existing)
        }
        //Yay all the things are prepped
    }


    abstract void start();

    abstract void stop();

    abstract boolean isUp();

    void enableDebug() {
        this.debugEnabled = true
    }

    /**
     * This enables easier debugging from the actual code under test.
     * This should call or fullfill all of the contracts performed by enableDebug().
     * This will print the port number to connect the debugger to and suspend the VM until it is connected.
     */
    def enableSuspend() {
        this.debugEnabled = true
        this.doSuspend = true
    }

    def keepSandbox() {
        this.keepSandbox = true
    }


    abstract void addToClassPath(String path)

    def waitForNon500FromUrl(url, int timeoutInSeconds = 60, int intervalInSeconds = 2) {

        waitForResponseCodeFromUrl(url, timeoutInSeconds, intervalInSeconds) { code -> code < 500 }
    }

    def waitForDesiredResponseCodeFromUrl(url, desiredCodes, timeoutInSeconds = 60, int intervalInSeconds = 2) {

        waitForResponseCodeFromUrl(url, timeoutInSeconds, intervalInSeconds) { code -> code in desiredCodes }
    }

    def waitForResponseCodeFromUrl(url, timeoutInSeconds, int intervalInSeconds, isResponseAcceptable) {

        print("\n\nWaiting for repose to start at ${url} \n\n")
        waitForCondition(clock, "${timeoutInSeconds}s", "${intervalInSeconds}s") {
            try {
                print(".")
                HttpClient client = new DefaultHttpClient()
                isResponseAcceptable(client.execute(new HttpGet(url)).statusLine.statusCode)
            } catch (IOException ignored) {
            } catch (ClientProtocolException ignored) {
            }
        }
        println()
    }
}
