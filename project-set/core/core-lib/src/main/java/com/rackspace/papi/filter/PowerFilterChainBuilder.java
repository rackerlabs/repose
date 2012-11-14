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
 * @author dan0288
 */
public interface PowerFilterChainBuilder extends Destroyable {

    Node getLocalhost();
    ReposeCluster getReposeCluster();
    ResourceConsumerCounter getResourceConsumerMonitor();
    void initialize(ReposeCluster domain, Node localhost, List<FilterContext> currentFilterChain, ServletContext servletContext, String defaultDst) throws PowerFilterChainException;
    PowerFilterChain newPowerFilterChain(FilterChain containerFilterChain) throws PowerFilterChainException;
}
