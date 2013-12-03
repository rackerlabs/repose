package com.rackspace.papi.service.httpclient.impl;

import org.apache.http.client.HttpClient;
import org.slf4j.Logger;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class ClientDecommissionManager {

    private final Thread decommThread;
    private final ClientDecommissioner decommissioner;
    private static final Logger LOG = org.slf4j.LoggerFactory.getLogger(ClientDecommissionManager.class);

    public ClientDecommissionManager(HttpClientUserManager userManager) {
        this.decommissioner = new ClientDecommissioner(userManager);
        this.decommThread = new Thread(decommissioner);
    }

    public void startThread() {
        LOG.debug("Starting HttpClient Decommissioner Thread");
        decommThread.start();
    }

    public void stopThread() {

        LOG.info("Shutting down HttpClient Service Decommissioner");
        decommissioner.stop();
        decommThread.interrupt();
    }

    public void decommissionClient(Map<String, HttpClient> clients) {

        Set<Entry<String, HttpClient>> entrySet = clients.entrySet();

        for (Map.Entry<String, HttpClient> clientEntry : entrySet) {
            decommissioner.addClientToBeDecommissioned(clientEntry.getValue());
        }
    }

    public void decommissionClient(HttpClient client) {
        decommissioner.addClientToBeDecommissioned(client);
    }
}
