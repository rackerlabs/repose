package com.rackspace.papi.filter;

import com.rackspace.papi.model.Filter;
import com.rackspace.papi.model.PowerProxy;
import com.rackspace.papi.service.classloader.ApplicationClassLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.FilterConfig;
import java.util.LinkedList;
import java.util.List;

/**
 * @author fran
 */
public class PowerFilterChainBuilder {

    private static final Logger LOG = LoggerFactory.getLogger(PowerFilterChainBuilder.class);
    private final FilterContextManager filterContextManager;

    public PowerFilterChainBuilder(FilterConfig filterConfig) {
        filterContextManager = new FilterContextManagerImpl(filterConfig);
    }

    public List<FilterContext> build(ApplicationClassLoader classLoaderContextManager, PowerProxy powerProxy) {
        final List<FilterContext> filterContexts = new LinkedList<FilterContext>();

        for (com.rackspace.papi.model.Filter papiFilter : new LocalhostFilterList(powerProxy).getFilters()) {
            //TODO: Validate Filter configuration contents - i.e. null name
            
            final FilterContext context = getFilterContext(classLoaderContextManager, papiFilter);

            if (context != null) {
                filterContexts.add(context);
            }
        }

        return new LinkedList<FilterContext>(filterContexts);
    }

    public FilterContext getFilterContext(ApplicationClassLoader classLoaderContextManager, Filter papiFilter) {
        FilterContext context = null;

        try {
            context = filterContextManager.loadFilterContext(papiFilter.getName(),
                    classLoaderContextManager.getLoadedApplications());
        } catch (Exception e) {
            LOG.info("Problem loading the filter class. Just process the next filter.", e);
        }

        return context;
    }
}
