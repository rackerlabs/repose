package framework

class TestProperties {

    String configDirectory
    String logFile
    String configSamples
    String connFramework
    String reposeEndpoint
    String reposeContainer = "valve"

    String reposeJar
    String glassfishJar

    int reposePort
    int reposeShutdownPort

    // Property settings that aren't set for every test
    String targetPort
    String identityPort

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

            reposePort = properties.getProperty("repose.port").toInteger()
            reposeShutdownPort = properties.getProperty("repose.shutdown.port").toInteger()

            glassfishJar = properties.getProperty("glassfish.jar")


            targetPort = properties.getProperty("target.port")
            identityPort = properties.getProperty("identity.port")

        } catch (Exception e) {
            throw new RuntimeException("Failure in setup of test: unable to read property files")
        } finally {
            propertiesStream.close()
        }
    }

    // Writing this method to minimize test changes when converting from a java.util.Properties
    // to this class
    def getProperty(String propertyName) {

        switch (propertyName) {
            case "target.port":
                return targetPort
                break
            case "identity.port":
                return identityPort
            default:
                return null
        }
    }

}
