package org.openrepose.core.services.datastore.distributed.impl.distributed.jetty;

import org.openrepose.core.domain.ReposeInstanceInfo;
import org.openrepose.services.datastore.DatastoreService;
import org.openrepose.core.services.datastore.distributed.impl.distributed.servlet.DistributedDatastoreServlet;
import org.openrepose.core.services.datastore.distributed.impl.distributed.servlet.DistributedDatastoreServletContextManager;
import org.openrepose.core.servlet.InitParameter;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

import java.util.ArrayList;
import java.util.List;

public class DistributedDatastoreJettyServerBuilder {

    private int port;
    private ReposeInstanceInfo reposeInstanceInfo;
    private DatastoreService datastoreService;
    private String configDirectory;
    private DistributedDatastoreServletContextManager manager;

    public DistributedDatastoreJettyServerBuilder(int port, ReposeInstanceInfo reposeInstanceInfo, String configDirectory, DistributedDatastoreServletContextManager manager) {
        this.port = port;
        this.reposeInstanceInfo = reposeInstanceInfo;
        this.configDirectory = configDirectory;
        this.manager = manager;
    }

    public Server newServer(DatastoreService datastore, ReposeInstanceInfo instanceInfo) {

        this.datastoreService = datastore;
        this.reposeInstanceInfo = instanceInfo;
        Server server = new Server();

        List<Connector> connectors = new ArrayList<Connector>();

        ServerConnector conn = new ServerConnector(server);
        conn.setPort(port);
        connectors.add(conn);

        server.setConnectors(connectors.toArray(new Connector[connectors.size()]));

        final ServletContextHandler rootContext = buildRootContext(server);
        final ServletHolder distributedDatastoreServletHolder = new ServletHolder(new DistributedDatastoreServlet(datastore));
        rootContext.addServlet(distributedDatastoreServletHolder, "/*");
        server.setHandler(rootContext);


        return server;
    }

    private ServletContextHandler buildRootContext(Server serverReference) {
        final ServletContextHandler servletContext = new ServletContextHandler(serverReference, "/");
        servletContext.getInitParams().put(InitParameter.REPOSE_CLUSTER_ID.getParameterName(), reposeInstanceInfo.getClusterId());
        servletContext.getInitParams().put(InitParameter.REPOSE_NODE_ID.getParameterName(), reposeInstanceInfo.getNodeId());
        servletContext.getInitParams().put(InitParameter.POWER_API_CONFIG_DIR.getParameterName(), configDirectory);
        servletContext.getInitParams().put("datastoreServicePort", String.valueOf(port));

        manager.setDatastoreSystemProperties(datastoreService, reposeInstanceInfo);
        servletContext.addEventListener(manager);

        return servletContext;
    }
}
