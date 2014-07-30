package com.rackspace.papi.service.routing.robin;

import com.rackspace.papi.model.Node;
import com.rackspace.papi.model.SystemModel;
import com.rackspace.papi.service.routing.RoutingService;
import org.openrepose.core.service.config.ConfigurationService;
import org.openrepose.core.service.config.manager.UpdateListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Named;
import java.net.URL;

/**
 * This no longer does weird stuff with ports
 * It just provides a way to get routable nodes from the SystemModel.
 */
@Named
public class RoundRobinRoutingService implements RoutingService {
    private static final Logger LOG = LoggerFactory.getLogger(RoundRobinRoutingService.class);
    private Clusters domains;
    private SystemModel config;
    private final ConfigurationService configurationManager;
    private final PowerApiConfigListener configurationListener;


    @Inject
    public RoundRobinRoutingService(
            ConfigurationService configurationManager) {
        this.configurationManager = configurationManager;
        this.configurationListener = new PowerApiConfigListener();
    }

    @PostConstruct
    public void afterPropertiesSet() {
        LOG.debug("initializing RoundRobinRoutingService");
        final URL xsdURL = getClass().getResource("/META-INF/schema/system-model/system-model.xsd");
        configurationManager.subscribeTo("system-model.cfg.xml", xsdURL, configurationListener, SystemModel.class);
    }

    @PreDestroy
    public void destroy() {
        if (configurationManager != null) {
            configurationManager.unsubscribeFrom("system-model.cfg.xml", configurationListener);
        }
    }

    @Override
    public void setSystemModel(final SystemModel config) {
        this.domains = new Clusters(config);
    }

    @Override
    public Node getRoutableNode(final String domainId) {
        final Node rtn;
        final ClusterWrapper domain = domains.getDomain(domainId);
        if (domain != null) {
            rtn = domain.getNextNode();
        } else {
            rtn = null;
        }
        return rtn;
    }

    private class PowerApiConfigListener implements UpdateListener<SystemModel> {
        private boolean isInitialized = false;

        @Override
        public void configurationUpdated(final SystemModel configurationObject) {
            config = configurationObject;
            setSystemModel(config);

            isInitialized = true;
        }

        @Override
        public boolean isInitialized() {
            return isInitialized;
        }
    }
}
