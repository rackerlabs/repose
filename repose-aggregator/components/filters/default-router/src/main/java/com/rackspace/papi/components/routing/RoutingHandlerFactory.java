package com.rackspace.papi.components.routing;

import com.google.common.base.Optional;
import com.rackspace.papi.commons.config.manager.UpdateListener;
import com.rackspace.papi.filter.SystemModelInterrogator;
import com.rackspace.papi.filter.logic.AbstractConfiguredFilterHandlerFactory;
import com.rackspace.papi.model.Destination;
import com.rackspace.papi.model.ReposeCluster;
import com.rackspace.papi.model.SystemModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import java.util.HashMap;
import java.util.Map;

public class RoutingHandlerFactory extends AbstractConfiguredFilterHandlerFactory<RoutingTagger> implements ApplicationContextAware {

    private ApplicationContext applicationContext;
    private SystemModel systemModel;
    private final SystemModelInterrogator modelInterrogator;
    private Destination dst;
    private static final Logger LOG = LoggerFactory.getLogger(RoutingTagger.class);

    public RoutingHandlerFactory(SystemModelInterrogator modelInterrogator) {
        this.modelInterrogator = modelInterrogator;
    }

    @Override
    public void setApplicationContext(ApplicationContext ac) {
        this.applicationContext = ac;
    }

    private class RoutingConfigurationListener implements UpdateListener<SystemModel> {

        private boolean isInitialized = false;

        @Override
        public void configurationUpdated(SystemModel configurationObject) {
            Optional<Destination> destination = modelInterrogator.getDefaultDestination(configurationObject);
            Optional<ReposeCluster> cluster = modelInterrogator.getLocalServiceDomain(configurationObject);

            dst = destination.orNull();
            if (cluster.isPresent() && !destination.isPresent()) {
                LOG.warn("No default destination configured for service domain: " + cluster.get().getId());
            } else if (!cluster.isPresent() && !destination.isPresent()) {
                LOG.warn("Unable to identify the local host in the system model - please check your system-model.cfg.xml");
            }

            systemModel = configurationObject;
            isInitialized = true;
        }

        @Override
        public boolean isInitialized() {
            return isInitialized;
        }
    }

    @Override
    protected RoutingTagger buildHandler() {

        if (!this.isInitialized()) {
            return null;
        }
        return applicationContext.getBean("routingTagger", RoutingTagger.class).setDestination(dst);
    }

    @Override
    protected Map<Class, UpdateListener<?>> getListeners() {
        return new HashMap<Class, UpdateListener<?>>() {

            {
                put(SystemModel.class, new RoutingConfigurationListener());
            }
        };
    }
}
