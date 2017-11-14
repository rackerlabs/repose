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
package org.openrepose.core.services.routing.robin;

import org.openrepose.commons.config.manager.UpdateListener;
import org.openrepose.core.services.config.ConfigurationService;
import org.openrepose.core.services.routing.RoutingService;
import org.openrepose.core.systemmodel.config.Node;
import org.openrepose.core.systemmodel.config.SystemModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Named;
import java.net.URL;

//TODO: pass the systemModel to this guy instead of having it be its own thing
@Named
public class RoundRobinRoutingService implements RoutingService {
    private static final Logger LOG = LoggerFactory.getLogger(RoundRobinRoutingService.class);
    private ConfigurationService configurationService;
    private PowerApiConfigListener configListener;
    private Clusters domains;

    @Inject
    public RoundRobinRoutingService(ConfigurationService configurationService) {
        configListener = new PowerApiConfigListener();
        this.configurationService = configurationService;
    }

    @PostConstruct
    public void afterPropertiesSet() {
        URL xsdURL = getClass().getResource("/META-INF/schema/system-model/system-model.xsd");
        configurationService.subscribeTo("system-model.cfg.xml", xsdURL, configListener, SystemModel.class);
    }

    @PreDestroy
    public void destroy() {
        configurationService.unsubscribeFrom("system-model.cfg.xml", configListener);
    }

    @Override
    public Node getRoutableNode(String domainId) {
        ClusterWrapper domain = domains.getDomain(domainId);
        if (domain != null) {
            return domain.getNextNode();
        }

        LOG.debug("no route-able node found, returning null");
        return null;
    }

    private class PowerApiConfigListener implements UpdateListener<SystemModel> {

        private SystemModel config;
        private boolean isInitialized = false;

        @Override
        public void configurationUpdated(SystemModel configurationObject) {
            config = configurationObject;
            domains = new Clusters(config);

            isInitialized = true;
        }

        @Override
        public boolean isInitialized() {
            return isInitialized;
        }
    }

}
