package framework

import org.rackspace.deproxy.PortFinder

class TestProperties {

    String configDirectory
    String rawConfigDirectory
    String logFile
    String configTemplates
    String connFramework
    String reposeContainer = "valve"
    String reposeHome
    String projectBuildDirectory

    String getReposeEndpoint() {
        return "http://${targetHostname}:${reposePort}"
    }

    String servoJar
    String jettyJar
    String reposeWar
    String glassfishJar
    String tomcatJar
    String mocksWar

    int reposePort

    // Property settings that aren't set for every test
    int targetPort
    int targetPort2
    int identityPort
    int atomPort
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
            logFile = properties.getProperty("repose.log")

            connFramework = "jersey"
            def value = properties.getProperty("repose.container")
            if (value) {
                reposeContainer = value
            }

            servoJar = properties.getProperty("repose.servo.jar")
            jettyJar = properties.getProperty("repose.jetty.jar")
            reposeWar = properties.getProperty("repose.root.war")
            reposePort = PortFinder.Singleton.getNextOpenPort()


            glassfishJar = properties.getProperty("glassfish.jar")
            tomcatJar = properties.getProperty("tomcat.jar")

            targetPort = PortFinder.Singleton.getNextOpenPort()
            targetPort2 = PortFinder.Singleton.getNextOpenPort()
            identityPort = PortFinder.Singleton.getNextOpenPort()
            atomPort = PortFinder.Singleton.getNextOpenPort()
            targetHostname = properties.getProperty("target.hostname")
            rawConfigDirectory = properties.getProperty("repose.raw.config.directory")
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
                reposePort: reposePort,
                targetPort: targetPort,
                targetPort1: targetPort,
                targetPort2: targetPort2,
                identityPort: identityPort,
                atomPort: atomPort,
                targetHostname: targetHostname,
                logFile: logFile,
                reposeLog: logFile,
                'repose.log': logFile,
                reposeHome: reposeHome,
                'repose.home': reposeHome,
                configDirectory: configDirectory,
                'repose.config.directory': configDirectory,
                'project.build.directory': projectBuildDirectory,
        ]
    }

}
