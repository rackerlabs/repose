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

import org.rackspace.deproxy.PortFinder

class TestProperties {

    String configDirectory
    String rawConfigDirectory
    String logFile
    String logFilePattern
    String configTemplates
    String connFramework
    String reposeContainer = "valve"
    String reposeVersion
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
    int valkyriePort
    int atomPort
    int phonehomePort
    String targetHostname

    TestProperties() {
        this("test.properties")
    }

    TestProperties(String resourceName) {
        this(ClassLoader.getSystemResource(resourceName).openStream())
    }

    TestProperties(InputStream propertiesStream) {

        try {
            Properties properties = new Properties()
            properties.load(propertiesStream)

            projectBuildDirectory = properties.getProperty("project.build.directory")
            configDirectory = properties.getProperty("repose.config.directory")
            configTemplates = properties.getProperty("repose.config.templates")
            logFile = properties.getProperty("repose.log.name")
            logFilePattern = properties.getProperty("repose.log.pattern")

            connFramework = "jersey"
            def value = properties.getProperty("repose.container")
            if (value) {
                reposeContainer = value
            }

            reposeJar = properties.getProperty("repose.jar")
            reposeLintJar = properties.getProperty("repose.lint.jar")
            reposeRootWar = properties.getProperty("repose.root.war")
            reposePort = PortFinder.Singleton.getNextOpenPort()


            glassfishJar = properties.getProperty("glassfish.jar")
            tomcatJar = properties.getProperty("tomcat.jar")

            targetPort = PortFinder.Singleton.getNextOpenPort()
            targetPort2 = PortFinder.Singleton.getNextOpenPort()
            identityPort = PortFinder.Singleton.getNextOpenPort()
            valkyriePort = PortFinder.Singleton.getNextOpenPort()
            atomPort = PortFinder.Singleton.getNextOpenPort()
            phonehomePort = PortFinder.Singleton.getNextOpenPort()
            targetHostname = properties.getProperty("target.hostname")
            rawConfigDirectory = properties.getProperty("repose.raw.config.directory")
            reposeVersion = properties.getProperty("repose.version")

            def reposeVersionMatcher = reposeVersion =~ /\.?(\d)/
            reposeMajorVersion = Integer.parseInt(reposeVersionMatcher[0][1] as String)
            reposeMinorVersion = Integer.parseInt(reposeVersionMatcher[1][1] as String)
            reposeFilterVersion = Integer.parseInt(reposeVersionMatcher[2][1] as String)
            reposePatchVersion = Integer.parseInt(reposeVersionMatcher[3][1] as String)
            reposeHome = properties.getProperty("repose.home")
            mocksWar = properties.getProperty("mocks.war")

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
                atomPort                 : atomPort,
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
