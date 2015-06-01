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
package org.openrepose.core.services.httpclient.impl;

import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.pool.PoolStats;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Thread runner which will monitor and shutdown when no active connections are being processed by passed
 * HttpClient(s)
 */
public class ClientDecommissioner implements Runnable {

    private static final Logger LOG = org.slf4j.LoggerFactory.getLogger(ClientDecommissioner.class);
    private static final long DEFAULT_INTERVAL = 5000;

    private final Object listLock;

    private boolean done;

    List<ExtendedHttpClient> clientList;
    HttpClientUserManager userManager;

    public ClientDecommissioner(HttpClientUserManager userManager) {
        clientList = new ArrayList<>();
        listLock = new Object();
        done = false;
        this.userManager = userManager;
    }

    public void addClientToBeDecommissioned(ExtendedHttpClient client) {
        synchronized (listLock) {
            PoolingHttpClientConnectionManager connectionManager = client.getConnectionManager();
            connectionManager.close();
            connectionManager.setMaxTotal(1);
            connectionManager.setDefaultMaxPerRoute(1);
            clientList.add(client);
        }
    }

    public void stop() {
        this.done = true;
    }

    @Override
    public void run() {
        while (!this.done) {
            synchronized (listLock) {
                LOG.trace("Iterating through decommissioned clients...");

                List<ExtendedHttpClient> clientsToRemove = new ArrayList<>();

                for (ExtendedHttpClient client : clientList) {

                    String clientId = client.getClientInstanceId();

                    if (userManager.hasUsers(clientId)) {
                        LOG.warn("Failed to shutdown connection pool client {} due to a connection still in " +
                                "use after last reconfiguration of Repose.", clientId);
                        break;
                    }

                    PoolingHttpClientConnectionManager connMan = client.getConnectionManager();
                    PoolStats stats = connMan.getTotalStats();

                    if (stats.getLeased() == 0) {   // if no active connections we will shutdown this client
                        LOG.debug("Shutting down client {}", clientId);
                        try {
                            client.getHttpClient().close();
                        } catch (IOException ioe) {
                            LOG.error("Could not close HTTP Client: {}", clientId);
                        }
                        clientsToRemove.add(client);
                    }
                }
                for (ExtendedHttpClient client : clientsToRemove) {
                    clientList.remove(client);
                    LOG.info("HTTP connection pool {} has been destroyed.", client.getClientInstanceId());
                }
            }

            try {
                Thread.sleep(DEFAULT_INTERVAL);
            } catch (InterruptedException ex) {
                LOG.info("Interrupted", ex);
                break;
            }

        }

        LOG.info("Shutting down HTTP Client Service Decommissioner");
        Thread.currentThread().interrupt();
    }
}
