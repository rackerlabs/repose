package org.openrepose.powerfilter;

import org.openrepose.commons.utils.Destroyable;
import org.openrepose.powerfilter.filtercontext.FilterContext;
import org.openrepose.core.systemmodel.Node;
import org.openrepose.core.systemmodel.ReposeCluster;

import javax.servlet.FilterChain;
import javax.servlet.ServletContext;
import java.util.List;

/**
 *
 * @author dan0288
 */
public interface PowerFilterChainBuilder extends Destroyable {

    void initialize(ReposeCluster domain, Node localhost, List<FilterContext> currentFilterChain, ServletContext servletContext, String defaultDst) throws PowerFilterChainException;
    PowerFilterChain newPowerFilterChain(FilterChain containerFilterChain) throws PowerFilterChainException;
}
