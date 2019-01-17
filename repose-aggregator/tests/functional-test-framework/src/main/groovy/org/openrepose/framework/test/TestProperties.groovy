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

class TestProperties {

    String runDirectory
    String configDirectory
    String logFile
    String logFilePattern
    String configTemplates
    String connFramework
    String reposeContainer = "valve"
    String reposeVersion
    String userRole
    String reposeHome
    String testRootDirectory

    String getReposeEndpoint() {
        return "http://${targetHostname}:${reposePort}"
    }

    String reposeJar
    String mocksWar

    int reposeMajorVersion
    int reposeMinorVersion
    int reposeFilterVersion
    int reposePatchVersion

    int reposePort

    // Property settings that aren't set for every test
    int targetPort
    int targetPort2
    int identityPort
    int identityPort2
    int valkyriePort
    int atomPort
    int atomPort2
    int phonehomePort
    int collectorTracingPort
    int agentTracingPort
    String targetHostname

    PortFinder portFinder = PortFinder.instance

    TestProperties(String testRunDirectory = null) {
        // Open a stream of the default properties, which should always be available.
        InputStream defaultPropertiesStream = ClassLoader.getSystemResource("default-repose-test.properties").openStream()

        defaultPropertiesStream.withCloseable {
            // Locate user properties, or throw an exception if user properties cannot be located.
            URL userPropertiesUrl = ClassLoader.getSystemResource("repose-test.properties")
            if (userPropertiesUrl == null) {
                throw new RuntimeException("Failure in setup of test: unable to read property files")
            }

            // Open a stream of the user properties.
            InputStream userPropertiesStream = userPropertiesUrl.openStream()

            userPropertiesStream.withCloseable {
                try {
                    // Set properties
                    Properties properties = new Properties()
                    properties.load(defaultPropertiesStream)
                    properties.load(userPropertiesStream)

                    runDirectory = testRunDirectory ? properties.getProperty("test.root.directory") + "/test-homes/" + testRunDirectory : properties.getProperty("repose.home")

                    testRootDirectory = properties.getProperty("test.root.directory")
                    configDirectory = runDirectory + properties.getProperty("repose.config.directory")
                    configTemplates = properties.getProperty("repose.config.templates")
                    logFile = runDirectory + properties.getProperty("repose.log.name")
                    logFilePattern = runDirectory + properties.getProperty("repose.log.pattern")

                    connFramework = "jersey"
                    def value = properties.getProperty("repose.container")
                    if (value) {
                        reposeContainer = value
                    }

                    reposeJar = properties.getProperty("repose.jar")

                    int portStart = properties.getProperty("port.finder.port.start") as int
                    int portMax = properties.getProperty("port.finder.port.max") as int
                    def workerIdPropertyName = properties.getProperty("port.finder.property.name.worker.id")
                    int workerId = System.getProperty(workerIdPropertyName, '1') as int
                    int portRange = properties.getProperty("port.finder.port.range") as int
                    portFinder.startPort = portStart + (workerId * portRange)
                    portFinder.maxPort = portMax

                    reposePort = portFinder.getNextOpenPort()


                    targetPort = portFinder.getNextOpenPort()
                    targetPort2 = portFinder.getNextOpenPort()
                    identityPort = portFinder.getNextOpenPort()
                    identityPort2 = portFinder.getNextOpenPort()
                    valkyriePort = portFinder.getNextOpenPort()
                    atomPort = portFinder.getNextOpenPort()
                    atomPort2 = portFinder.getNextOpenPort()
                    phonehomePort = portFinder.getNextOpenPort()
                    collectorTracingPort = portFinder.getNextOpenPort()
                    agentTracingPort = portFinder.getNextOpenPort()
                    targetHostname = properties.getProperty("target.hostname")
                    reposeVersion = properties.getProperty("repose.version")

                    def reposeVersionMatcher = reposeVersion =~ /\.?(\d)/
                    reposeMajorVersion = Integer.parseInt(reposeVersionMatcher[0][1] as String)
                    reposeMinorVersion = Integer.parseInt(reposeVersionMatcher[1][1] as String)
                    reposeFilterVersion = Integer.parseInt(reposeVersionMatcher[2][1] as String)
                    reposePatchVersion = Integer.parseInt(reposeVersionMatcher[3][1] as String)
                    reposeHome = properties.getProperty("repose.home")
                    mocksWar = properties.getProperty("mocks.war")
                    userRole = "foyer"
                } catch (Exception e) {
                    throw new RuntimeException("Failure in setup of test: unable to read property files", e)
                }
            }
        }
    }

    def getDefaultTemplateParams() {
        return [
            reposePort               : reposePort,
            targetPort               : targetPort,
            targetPort1              : targetPort,
            targetPort2              : targetPort2,
            identityPort             : identityPort,
            identityPort2            : identityPort2,
            atomPort                 : atomPort,
            atomPort2                : atomPort2,
            targetHostname           : targetHostname,
            logFile                  : logFile,
            logFileName              : logFile,
            reposeLog                : logFile,
            'repose.log.name'        : logFile,
            'repose.log.pattern'     : logFilePattern,
            reposeHome               : reposeHome,
            'repose.home'            : reposeHome,
            configDirectory          : configDirectory,
            'repose.config.directory': configDirectory,
            'test.root.directory'    : testRootDirectory,
            'valkyriePort'           : valkyriePort,
            'phonehomePort'          : phonehomePort,
            'repose.run.directory'   : runDirectory,
        ]
    }

}
