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

import org.openrepose.core.services.httpclient.ExtendedHttpClient;
import org.slf4j.Logger;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class ClientDecommissionManager {

    private static final Logger LOG = org.slf4j.LoggerFactory.getLogger(ClientDecommissionManager.class);
    private final Thread decommThread;
    private final ClientDecommissioner decommissioner;

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

    public void decommissionClient(Map<String, ExtendedHttpClient> clients) {
        Set<Entry<String, ExtendedHttpClient>> entrySet = clients.entrySet();

        for (Map.Entry<String, ExtendedHttpClient> clientEntry : entrySet) {
            decommissioner.addClientToBeDecommissioned(clientEntry.getValue());
        }
    }

    public void decommissionClient(ExtendedHttpClient client) {
        decommissioner.addClientToBeDecommissioned(client);
    }
}
