package com.rackspace.papi.filter;

import com.rackspace.papi.commons.util.StringUtilities;
import com.rackspace.papi.domain.ReposeInstanceInfo;
import com.rackspace.papi.model.Node;
import com.rackspace.papi.model.ReposeCluster;
import com.rackspace.papi.service.classloader.ClassLoaderManagerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.FilterConfig;
import java.util.LinkedList;
import java.util.List;

/**
 * Takes info from config file to initialize a filter context.
 * TODO: I think this needs to be a spring bean?
 */
@Named
public class FilterContextInitializer implements ApplicationContextAware{

    private static final Logger LOG = LoggerFactory.getLogger(FilterContextInitializer.class);
    private final ReposeInstanceInfo instanceInfo;
    private final ClassLoaderManagerService classLoaderManagerService;
    private ApplicationContext applicationContext;

    @Inject
    public FilterContextInitializer(
            ReposeInstanceInfo instanceInfo,
            ClassLoaderManagerService classLoaderManagerService) {

        this.classLoaderManagerService = classLoaderManagerService;
        this.instanceInfo = instanceInfo;
    }

    public List<FilterContext> buildFilterContexts(FilterConfig filterConfig, ReposeCluster domain, Node localHost) {
        FilterContextManager filterContextManager = new FilterContextManagerImpl(filterConfig, applicationContext);
        Thread.currentThread().setName(instanceInfo.toString());

        final List<FilterContext> filterContexts = new LinkedList<FilterContext>();

        if (localHost == null || domain == null) {
            LOG.error("Unable to identify the local host in the system model - please check your system-model.cfg.xml");
            throw new IllegalArgumentException("Domain and host cannot be null");
        }

        if (domain.getFilters() != null && domain.getFilters().getFilter() != null) {
            for (com.rackspace.papi.model.Filter papiFilter : domain.getFilters().getFilter()) {

                if (StringUtilities.isBlank(papiFilter.getName())) {
                    LOG.error(
                            "Filter declaration has a null or empty name value - please check your system model configuration");
                    continue;
                }

                if (classLoaderManagerService.hasFilter(papiFilter.getName())) {
                    FilterContext context = null;

                    try {
                        context = filterContextManager.loadFilterContext(
                                papiFilter,
                                classLoaderManagerService.getLoadedApplications());
                    } catch (Exception e) {
                        LOG.info("Problem loading the filter class. Just process the next filter. Reason: " + e.getMessage(), e);
                    }

                    if (context != null) {
                        filterContexts.add(context);
                    } else {
                        filterContexts.add(new FilterContext(null, null, papiFilter));
                    }
                } else {
                    LOG.error(
                            "Unable to satisfy requested filter chain - none of the loaded artifacts supply a filter named " +
                                    papiFilter.getName());
                    filterContexts.add(new FilterContext(null, null, papiFilter));
                }
            }
        }

        return filterContexts;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}
