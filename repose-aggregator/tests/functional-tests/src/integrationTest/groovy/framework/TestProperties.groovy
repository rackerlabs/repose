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

class TestProperties {

    String configDirectory
    String logFile
    String reposeLintLogFile
    String logFilePattern
    String configTemplates
    String connFramework
    String reposeContainer = "valve"
    String reposeVersion
    String userRole
    String reposeHome
    String projectBuildDirectory

    String getReposeEndpoint() {
        return "http://${targetHostname}:${reposePort}"
    }

    String reposeJar
    String reposeLintJar
    String glassfishJar
    String tomcatJar
    String reposeRootWar
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
    String targetHostname

    PortFinder portFinder = PortFinder.instance

    TestProperties(String testRunDirectory = null) {
        InputStream propertiesStream = ClassLoader.getSystemResource("test.properties").openStream()

        try {
            Properties properties = new Properties()
            properties.load(propertiesStream)

            String runDirectory = testRunDirectory ? properties.getProperty("project.build.directory") + "/test-homes/" + testRunDirectory : properties.getProperty("repose.home")

            projectBuildDirectory = properties.getProperty("project.build.directory")
            configDirectory = runDirectory + properties.getProperty("repose.config.directory")
            configTemplates = properties.getProperty("repose.config.templates")
            logFile = runDirectory + properties.getProperty("repose.log.name")
            reposeLintLogFile = runDirectory + properties.getProperty("repose.lint.log.name")
            logFilePattern = runDirectory + properties.getProperty("repose.log.pattern")

            connFramework = "jersey"
            def value = properties.getProperty("repose.container")
            if (value) {
                reposeContainer = value
            }

            reposeJar = properties.getProperty("repose.jar")
            reposeLintJar = properties.getProperty("repose.lint.jar")
            reposeRootWar = properties.getProperty("repose.root.war")

            int portStart = properties.getProperty("port.finder.port.start") as int
            int portMax = properties.getProperty("port.finder.port.max") as int
            def workerIdPropertyName = properties.getProperty("port.finder.property.name.worker.id")
            int workerId = System.getProperty(workerIdPropertyName, '1') as int
            int portRange = properties.getProperty("port.finder.port.range") as int
            portFinder.startPort = portStart + (workerId * portRange)
            portFinder.maxPort = portMax

            reposePort = portFinder.getNextOpenPort()

            glassfishJar = properties.getProperty("glassfish.jar")
            tomcatJar = properties.getProperty("tomcat.jar")

            targetPort = portFinder.getNextOpenPort()
            targetPort2 = portFinder.getNextOpenPort()
            identityPort = portFinder.getNextOpenPort()
            identityPort2 = portFinder.getNextOpenPort()
            valkyriePort = portFinder.getNextOpenPort()
            atomPort = portFinder.getNextOpenPort()
            atomPort2 = portFinder.getNextOpenPort()
            phonehomePort = portFinder.getNextOpenPort()
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
        } finally {
            propertiesStream.close()
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
                'project.build.directory': projectBuildDirectory,
                'valkyriePort'           : valkyriePort,
                'phonehomePort'          : phonehomePort
        ]
    }

}
