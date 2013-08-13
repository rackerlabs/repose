package com.rackspace.papi.service.httpclient.impl;

import org.apache.http.client.HttpClient;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.impl.conn.PoolingClientConnectionManager;
import org.apache.http.pool.PoolStats;

import javax.resource.spi.ConnectionManager;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Thread runner which will  monitor and shutdown when no active connections are being processed by passed
 * HttpClient(s)
 */
public class ClientDecommissioner implements Runnable {


    private static final long DEFAULT_INTERVAL = 5000;
    List<HttpClient> clientList;
    private boolean done = false;
    private Object listLock;

    public ClientDecommissioner() {

        clientList = new ArrayList<HttpClient>();
        listLock = new Object();
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

    public void addClientsToBeDecommissioned(List<HttpClient> clients) {
        for (HttpClient client : clients) {
            addClientToBeDecommissioned(client);
        }
    }

    public void stop() {
        done = true;
    }

    @Override
    public void run() {
        while (!done) {
            synchronized (listLock) {

                for (HttpClient client : clientList) {

                    PoolingClientConnectionManager connMan = (PoolingClientConnectionManager) client.getConnectionManager();
                    PoolStats stats = connMan.getTotalStats();

                    if (stats.getLeased() == 0) {   // if no active connections we will shutdown this client
                        connMan.shutdown();
                        clientList.remove(client);
                    }
                }
            }

            try {
                Thread.sleep(DEFAULT_INTERVAL);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }

        }
    }
}
