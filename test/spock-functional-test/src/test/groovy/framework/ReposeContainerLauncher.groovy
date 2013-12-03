package framework

import java.nio.charset.Charset

class ReposeContainerLauncher extends AbstractReposeLauncher {

    int shutdownPort
    int reposePort

    String clusterId
    String nodeId

    String containerJar
    String rootWarLocation
    String[] appWars

    ReposeContainerLauncher(ReposeConfigurationProvider configurationProvider, String containerJar,
                            String clusterId = "cluster1", String nodeId = "node1",
                            String rootWarLocation, int reposePort, int stopPort, String... appWars) {
        this.configurationProvider = configurationProvider
        this.containerJar = containerJar
        this.clusterId = clusterId
        this.nodeId = nodeId
        this.reposePort = reposePort
        this.rootWarLocation = rootWarLocation
        this.shutdownPort = stopPort
        this.appWars = appWars
    }

    @Override
    void start() {
        String configDirectory = configurationProvider.getReposeConfigDir()

        String webXmlOverrides = "-Dpowerapi-config-directory=${configDirectory} -Drepose-cluster-id=${clusterId} -Drepose-node-id=${nodeId}"

        def cmd = "java ${webXmlOverrides} -jar ${containerJar} -p ${reposePort} -w ${rootWarLocation} -s ${shutdownPort}"

        if (appWars != null || appWars.length != 0) {
            for (String path : appWars) {
                cmd += " -war ${path}"
            }
        }
        println("Starting repose: ${cmd}")


        def th = new Thread({ cmd.execute() });

        th.run()
        th.join()
    }

    @Override
    void stop() {
        try {
            final Socket s = new Socket(InetAddress.getByName("127.0.0.1"), shutdownPort);
            final OutputStream out = s.getOutputStream();

            println("Sending Repose stop request to port $shutdownPort");

            out.write(("\r\n").getBytes(Charset.forName("UTF-8")));
            out.flush();
            s.close();
        } catch (IOException ioex) {
            println("An error occurred while attempting to stop Repose Controller. Reason: " + ioex.getMessage());
        }
    }

    @Override
    boolean isUp() {
        throw new UnsupportedOperationException("implement me")
    }

}
