package org.openrepose.core.services.datastore.distributed.impl.distributed.jetty;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.openrepose.commons.utils.proxy.RequestProxyService;
import org.openrepose.core.services.datastore.distributed.impl.distributed.servlet.DistributedDatastoreServlet;
import org.openrepose.services.datastore.DatastoreAccessControl;
import org.openrepose.services.datastore.DatastoreService;
import org.openrepose.services.datastore.distributed.ClusterView;

/**
 * TODO: this could almost be a single function in the DDLauncherService class
 */
public class DistributedDatastoreJettyServerBuilder {

    private int port;
    private final ClusterView clusterView;
    private final DatastoreAccessControl datastoreAccessControl;
    private final RequestProxyService requestProxyService;
    private final String clusterId;
    private final DatastoreService datastoreService;

    public DistributedDatastoreJettyServerBuilder(int port,
                                                  String clusterId,
                                                  DatastoreService datastoreService,
                                                  ClusterView clusterView,
                                                  DatastoreAccessControl datastoreAccessControl,
                                                  RequestProxyService requestProxyService) {
        this.port = port;
        this.clusterId = clusterId;
        this.datastoreService = datastoreService;
        this.clusterView = clusterView;
        this.datastoreAccessControl = datastoreAccessControl;
        this.requestProxyService = requestProxyService;
    }

    /**
     * create a server with the elements provided from the constructor, as all elements are required
     *
     * @return a jetty server ready to be started
     */
    public Server newServer() {
        Server server = new Server();

        ServerConnector conn = new ServerConnector(server);
        conn.setPort(port);

        server.addConnector(conn);

        final ServletContextHandler rootContext = new ServletContextHandler(server, "/");
        final DistributedDatastoreServlet ddServlet = new DistributedDatastoreServlet(
                datastoreService,
                clusterView,
                datastoreAccessControl,
                requestProxyService
        );

        final ServletHolder distributedDatastoreServletHolder = new ServletHolder(ddServlet);
        distributedDatastoreServletHolder.setName("DistDatastoreServlet-" + clusterId);
        rootContext.addServlet(distributedDatastoreServletHolder, "/*");
        server.setHandler(rootContext);

        return server;
    }
}
