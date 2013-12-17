package framework

import org.rackspace.deproxy.PortFinder

class TestProperties {

    String configDirectory
    String rawConfigDirectory
    String logFile
    String configSamples
    String connFramework
    String reposeEndpoint
    String reposeContainer = "valve"
    String reposeHome

    String reposeJar
    String glassfishJar
    String tomcatJar
    String reposeRootWar
    String mocksWar

    int reposePort
    int reposeShutdownPort
    int dynamicPortBase

    // Property settings that aren't set for every test
    String targetPort
    String targetPort2
    String identityPort
    String atomPort
    String targetHostname

    TestProperties(InputStream propertiesStream) {

        try {
            Properties properties = new Properties()
            properties.load(propertiesStream)

            configDirectory = properties.getProperty("repose.config.directory")
            configSamples = properties.getProperty("repose.config.samples")
            reposeEndpoint = properties.getProperty("repose.endpoint")
            logFile = properties.getProperty("repose.log")

            connFramework = "jersey"
            def value = properties.getProperty("repose.container")
            if (value) {
                reposeContainer = value
            }

            reposeJar = properties.getProperty("repose.jar")
            reposeRootWar = properties.getProperty("repose.root.war")
            reposePort = PortFinder.Singleton.getNextOpenPort()
            reposeShutdownPort = PortFinder.Singleton.getNextOpenPort()
            dynamicPortBase = properties.getProperty("repose.dynamic.port.base").toInteger()

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
            throw new RuntimeException("Failure in setup of test: unable to read property files")
        } finally {
            propertiesStream.close()
        }
    }

    // Writing this method to minimize test changes when converting from a java.util.Properties
    // to this class
    def getReposeProperty(String propertyName) {

        switch (propertyName) {
            case "target.port":
                return targetPort
                break
            case "target.port2":
                return targetPort2
                break
            case "identity.port":
                return identityPort
            case "atom.port":
                return atomPort
            default:
                return null
        }
    }

}
