package org.openrepose.core.services.routing.robin;

import org.openrepose.commons.config.manager.UpdateListener;
import org.openrepose.core.services.config.ConfigurationService;
import org.openrepose.core.systemmodel.Node;
import org.openrepose.core.systemmodel.SystemModel;
import org.openrepose.core.services.routing.RoutingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Named;
import java.net.URL;

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
