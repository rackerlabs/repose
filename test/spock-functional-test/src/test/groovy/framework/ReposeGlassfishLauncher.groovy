package framework

class ReposeGlassfishLauncher extends AbstractReposeLauncher {

    def int shutdownPort

    String configDirectory
    String clusterId
    String nodeId

    def ReposeConfigurationProvider configurationProvider
    String glassfishJar

    ReposeGlassfishLauncher(String glassfishJar, String configDirectory, String clusterId="cluster1", String nodeId="node1") {
        this.configurationProvider = configurationProvider
        this.glassfishJar = glassfishJar
        this.clusterId = clusterId
        this.nodeId = nodeId
    }

    @Override
    void start() {

        String webXmlOverrides = "-Dpowerapi-config-directory=${configDirectory} -Drepose-cluster-id=${clusterId} -Drepose-node-id=${nodeId}"

        def cmd = "java -jar ${glassfishJar} -s ${shutdownPort} ${webXmlOverrides}"
        if (!connFramework.isEmpty()) {
            cmd = cmd + " -cf ${connFramework}"
        }
        cmd = cmd + " start"
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
