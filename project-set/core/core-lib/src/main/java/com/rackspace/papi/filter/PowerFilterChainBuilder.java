package com.rackspace.papi.filter;

import com.rackspace.papi.commons.util.Destroyable;
import com.rackspace.papi.filter.resource.ResourceConsumerCounter;
import com.rackspace.papi.model.Node;
import com.rackspace.papi.model.ReposeCluster;
import java.util.List;
import javax.servlet.FilterChain;
import javax.servlet.ServletContext;

/**
 *
 * @author zinic
 */
public class PowerFilterChainBuilder implements Destroyable {

    private final ResourceConsumerCounter resourceConsumerMonitor;
    private final List<FilterContext> currentFilterChain;
    private final ReposeCluster domain;
    private final Node localhost;
    private final PowerFilterRouter router;

    public PowerFilterChainBuilder(ReposeCluster domain, Node localhost, List<FilterContext> currentFilterChain, ServletContext servletContext) throws PowerFilterChainException {
        this.currentFilterChain = currentFilterChain;
        resourceConsumerMonitor = new ResourceConsumerCounter();
        this.domain = domain;
        this.localhost = localhost;
        this.router = new PowerFilterRouterImpl(domain, localhost, servletContext);
    }

    public ResourceConsumerCounter getResourceConsumerMonitor() {
        return resourceConsumerMonitor;
    }
    
    public PowerFilterChain newPowerFilterChain(FilterChain containerFilterChain) throws PowerFilterChainException {
        if (router == null) {
            throw new PowerFilterChainException("Power Filter Router has not been initialized yet.");
        }
        return new PowerFilterChain(currentFilterChain, containerFilterChain, resourceConsumerMonitor, router);
    }

    public ReposeCluster getReposeCluster() {
        return domain;
    }

    public Node getLocalhost() {
        return localhost;
    }

    @Override
    public void destroy() {
        for (FilterContext context : currentFilterChain) {
            context.destroy();
        }
    }
}
