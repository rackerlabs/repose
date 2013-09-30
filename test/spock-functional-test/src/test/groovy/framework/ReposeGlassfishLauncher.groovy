package framework

class ReposeGlassfishLauncher extends AbstractReposeLauncher {

    int shutdownPort
    int reposePort

    String clusterId
    String nodeId

    String glassfishJar
    String rootWarLocation

    ReposeGlassfishLauncher(ReposeConfigurationProvider configurationProvider, String glassfishJar, String clusterId="cluster1", String nodeId="node1", String rootWarLocation, int reposePort) {
        this.configurationProvider = configurationProvider
        this.glassfishJar = glassfishJar
        this.clusterId = clusterId
        this.nodeId = nodeId
        this.reposePort = reposePort
        this.rootWarLocation = rootWarLocation
    }

    @Override
    void start() {
        String configDirectory = configurationProvider.getReposeConfigDir()

        String webXmlOverrides = "-Dpowerapi-config-directory=${configDirectory} -Drepose-cluster-id=${clusterId} -Drepose-node-id=${nodeId}"

        def cmd = "java ${webXmlOverrides} -jar ${glassfishJar} -p ${reposePort} -w ${rootWarLocation}"
//        cmd = cmd + " start"
        println("Starting repose: ${cmd}")

        def th = new Thread({ cmd.execute() });

        th.run()
        th.join()
    }

    @Override
    void stop() {
        def cmd = "java -jar ${glassfishJar} -s ${shutdownPort} stop"
        println("Stopping Glassfish: ${cmd}")
    }

    @Override
    boolean isUp() {
        throw new UnsupportedOperationException("implement me")
    }
}
