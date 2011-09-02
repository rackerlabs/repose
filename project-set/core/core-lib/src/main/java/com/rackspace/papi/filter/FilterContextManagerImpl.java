package com.rackspace.papi.filter;

import com.rackspace.papi.commons.util.classloader.ear.EarClassLoaderContext;
import javax.servlet.ServletException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.FilterConfig;
import java.util.Collection;

public class FilterContextManagerImpl implements FilterContextManager {
    private static final Logger LOG = LoggerFactory.getLogger(PowerFilterChainBuilder.class);
    private final FilterConfig filterConfig;

    public FilterContextManagerImpl(FilterConfig filterConfig) {
        this.filterConfig = filterConfig;
    }

    @Override
    public FilterContext loadFilterContext(String filterName, Collection<EarClassLoaderContext> loadedApplications) throws ClassNotFoundException {
        FilterClassFactory filterClassFactory = FilterContextManagerImpl.getFilterClassFactory(filterName, loadedApplications);

        return initializeFilter(filterClassFactory);
    }

    public static FilterClassFactory getFilterClassFactory(String filterName, Collection<EarClassLoaderContext> loadedApplications) {
        for (EarClassLoaderContext classLoaderCtx : loadedApplications) {
            final String filterClassName = classLoaderCtx.getEarDescriptor().getRegisteredFilters().get(filterName);

            if (filterClassName != null) {
                return new FilterClassFactory(filterClassName, classLoaderCtx.getClassLoader());
            }
        }

        throw new IllegalStateException("Unable to look up filter " + filterName + " - this is protected by a validation guard in a higher level of the architecture and should be logged as a defect");
    }

    public FilterContext initializeFilter(FilterClassFactory filterClassFactory) {
        final Thread currentThread = Thread.currentThread();
        final ClassLoader previousClassLoader = currentThread.getContextClassLoader();
        final ClassLoader nextClassLoader = filterClassFactory.getClassLoader();

        try {
            currentThread.setContextClassLoader(nextClassLoader);
            final javax.servlet.Filter newFilterInstance = filterClassFactory.newInstance();

            newFilterInstance.init(filterConfig);

            LOG.info("Filter: " + newFilterInstance + " successfully created");
            
            return new FilterContext(newFilterInstance, filterClassFactory.getClassLoader());
        } catch (ClassNotFoundException e) {
            LOG.error("Failed to initialize filter " + filterClassFactory + ".");
            throw new FilterInitializationException(e.getMessage(), e);
        } catch (ServletException e) {
            LOG.error("Failed to initialize filter " + filterClassFactory + ".");
            throw new FilterInitializationException(e.getMessage(), e);
        } finally {
            currentThread.setContextClassLoader(previousClassLoader);
        }
    }
}
