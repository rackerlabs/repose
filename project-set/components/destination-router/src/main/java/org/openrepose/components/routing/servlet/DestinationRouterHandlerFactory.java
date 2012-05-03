package org.openrepose.components.routing.servlet;

import com.rackspace.papi.commons.config.manager.UpdateListener;
import com.rackspace.papi.domain.Port;
import com.rackspace.papi.filter.logic.AbstractConfiguredFilterHandlerFactory;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.openrepose.components.routing.servlet.config.DestinationRouterConfiguration;

public class DestinationRouterHandlerFactory extends AbstractConfiguredFilterHandlerFactory<RoutingTagger> {

    private DestinationRouterConfiguration contextRouterConfiguration;
    private final List<Port> ports;

    public DestinationRouterHandlerFactory(List<Port> ports) {
        this.ports = ports;
    }

    private class RoutingConfigurationListener implements UpdateListener<DestinationRouterConfiguration> {

        @Override
        public void configurationUpdated(DestinationRouterConfiguration configurationObject) {
            contextRouterConfiguration = configurationObject;
        }
    }

    @Override
    protected RoutingTagger buildHandler() {
        return new RoutingTagger(contextRouterConfiguration.getTarget());
    }

    @Override
    protected Map<Class, UpdateListener<?>> getListeners() {
        final Map<Class, UpdateListener<?>> updateListeners = new HashMap<Class, UpdateListener<?>>();
        updateListeners.put(DestinationRouterConfiguration.class, new RoutingConfigurationListener());
        return updateListeners;
    }
}
