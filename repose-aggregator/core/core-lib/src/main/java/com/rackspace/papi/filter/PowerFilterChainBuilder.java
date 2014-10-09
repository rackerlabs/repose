package com.rackspace.papi.filter;

import org.openrepose.commons.utils.Destroyable;
import com.rackspace.papi.model.Node;
import com.rackspace.papi.model.ReposeCluster;

import javax.servlet.FilterChain;
import javax.servlet.ServletContext;
import java.util.List;

/**
 *
 * @author dan0288
 */
public interface PowerFilterChainBuilder extends Destroyable {

    Node getLocalhost();
    ReposeCluster getReposeCluster();
    void initialize(ReposeCluster domain, Node localhost, List<FilterContext> currentFilterChain, ServletContext servletContext, String defaultDst) throws PowerFilterChainException;
    PowerFilterChain newPowerFilterChain(FilterChain containerFilterChain) throws PowerFilterChainException;
}
