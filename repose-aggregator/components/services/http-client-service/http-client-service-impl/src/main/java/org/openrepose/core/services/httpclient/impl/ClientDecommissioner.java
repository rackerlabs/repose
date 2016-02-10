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

import org.apache.http.client.HttpClient;
import org.apache.http.impl.conn.PoolingClientConnectionManager;
import org.apache.http.pool.PoolStats;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * Thread runner which will  monitor and shutdown when no active connections are being processed by passed
 * HttpClient(s)
 */
public class ClientDecommissioner implements Runnable {

    private static final Logger LOG = org.slf4j.LoggerFactory.getLogger(ClientDecommissioner.class);

    private static final long DEFAULT_INTERVAL = 5000;
    List<HttpClient> clientList;
    HttpClientUserManager userManager;
    private boolean done;
    private Object listLock;

    public ClientDecommissioner(HttpClientUserManager userManager) {

        clientList = new ArrayList<HttpClient>();
        listLock = new Object();
        done = false;
        this.userManager = userManager;
    }

    public void addClientToBeDecommissioned(HttpClient client) {

        synchronized (listLock) {
            PoolingClientConnectionManager connMan = (PoolingClientConnectionManager) client.getConnectionManager();
            connMan.closeExpiredConnections();
            connMan.setMaxTotal(1);
            connMan.setDefaultMaxPerRoute(1);
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

                List<HttpClient> clientsToRemove = new ArrayList<HttpClient>();

                for (HttpClient client : clientList) {

                    String clientId = client.getParams().getParameter(HttpConnectionPoolProvider.CLIENT_INSTANCE_ID).toString();

                    if (userManager.hasUsers(clientId)) {
                        LOG.warn("Failed to shutdown connection pool client {} due to a connection still in " +
                                "use after last reconfiguration of Repose.", clientId);
                        break;
                    }

                    PoolingClientConnectionManager connMan = (PoolingClientConnectionManager) client.getConnectionManager();
                    PoolStats stats = connMan.getTotalStats();

                    if (stats.getLeased() == 0) {   // if no active connections we will shutdown this client
                        LOG.debug("Shutting down client {}", clientId);
                        connMan.shutdown();
                        clientsToRemove.add(client);
                    }
                }
                for (HttpClient client : clientsToRemove) {
                    clientList.remove(client);
                    LOG.info("HTTP connection pool {} has been destroyed.", client.getParams().getParameter(HttpConnectionPoolProvider.CLIENT_INSTANCE_ID));
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
