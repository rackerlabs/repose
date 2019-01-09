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
package org.openrepose.nodeservice.distributed.jetty;

import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.openrepose.commons.utils.io.FileUtilities;
import org.openrepose.core.services.datastore.distributed.config.DistributedDatastoreConfiguration;
import org.openrepose.nodeservice.distributed.servlet.DistributedDatastoreServlet;

/**
 * Perhaps encapsulate the monitoring bits better
 */
public class DistributedDatastoreServer {

    private final String nodeId;
    private final DistributedDatastoreServlet ddServlet;
    private final DistributedDatastoreConfiguration ddConfig;

    private int port;
    private Server server;

    public DistributedDatastoreServer(String nodeId,
                                      DistributedDatastoreServlet ddServlet,
                                      DistributedDatastoreConfiguration ddConfig
    ) {
        this.nodeId = nodeId;
        this.ddServlet = ddServlet;
        this.ddConfig = ddConfig;
    }

    /**
     * Start the server on a port. If it's already started on that port, it won't do anything at all.
     * If the port changes, the server will be stopped and a new one will be turned on using the same servlet.
     *
     * @param port
     * @throws Exception
     */
    public void runServer(int port, String configRoot) throws Exception {
        if (this.port != port) {
            if (server != null) {
                server.stop();
            }

            server = new Server();

            ServerConnector conn;
            if (ddConfig.getKeystoreFilename() != null) {
                SslContextFactory cf = new SslContextFactory();
                cf.setKeyStorePath(FileUtilities.guardedAbsoluteFile(configRoot, ddConfig.getKeystoreFilename()).getAbsolutePath());
                cf.setKeyStorePassword(ddConfig.getKeystorePassword());
                cf.setKeyManagerPassword(ddConfig.getKeyPassword());
                cf.setNeedClientAuth(true);

                if (ddConfig.getTruststoreFilename() != null) {
                    cf.setTrustStorePath(FileUtilities.guardedAbsoluteFile(configRoot, ddConfig.getTruststoreFilename()).getAbsolutePath());
                    cf.setTrustStorePassword(ddConfig.getTruststorePassword());
                }

                // TODO: Always use a certain protocol/cipher?

                conn = new ServerConnector(server, cf);
            } else {
                conn = new ServerConnector(server);
            }
            conn.getConnectionFactory(HttpConnectionFactory.class)
                    .getHttpConfiguration()
                    .setSendServerVersion(false);

            conn.setPort(port);
            this.port = port; //Save the port so we know if it's changed
            server.addConnector(conn);

            ServletContextHandler rootContext = new ServletContextHandler(server, "/");

            ServletHolder holder = new ServletHolder(ddServlet);
            holder.setName("DistDatastoreServlet-" + nodeId);
            rootContext.addServlet(holder, "/*");
            server.setHandler(rootContext);
            server.setStopAtShutdown(true);
            server.start();
        }
    }

    public void stop() throws Exception {
        if (server != null && server.isStarted()) {
            server.stop();
        }
    }

    public int getPort() {
        return port;
    }
}
