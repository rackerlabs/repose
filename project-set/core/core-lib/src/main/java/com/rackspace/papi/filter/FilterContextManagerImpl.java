package com.rackspace.papi.filter;

import com.rackspace.papi.commons.util.classloader.ear.EarClassLoaderContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.FilterConfig;
import java.util.Collection;

/**
 * Created by IntelliJ IDEA.
 * User: joshualockwood
 * Date: 6/27/11
 * Time: 12:46 PM
 */
public class FilterContextManagerImpl implements FilterContextManager {
    private static final Logger LOG = LoggerFactory.getLogger(PowerFilterChainBuilder.class);
    private final FilterConfig filterConfig;

    public FilterContextManagerImpl(FilterConfig filterConfig) {
        this.filterConfig = filterConfig;
    }

    public FilterContext loadFilterContext(String filterName, Collection<EarClassLoaderContext> loadedApplications) throws ClassNotFoundException {
        FilterClassFactory filterClassFactory = FilterContextManagerImpl.getFilterClassFactory(filterName, loadedApplications);

        return initializeFilter(filterClassFactory);
    }

    public static FilterClassFactory getFilterClassFactory(String filterName, Collection<EarClassLoaderContext> loadedApplications) {
        FilterClassFactory filterClassFactory = null;

        for (EarClassLoaderContext classLoaderCtx : loadedApplications) {
            final String filterClassName = classLoaderCtx.getEarDescriptor().getRegisteredFilters().get(filterName);

            if (filterClassName != null) {
                filterClassFactory = new FilterClassFactory(filterClassName, classLoaderCtx.getClassLoader());

                break;
            }
        }

        return filterClassFactory;
    }

    public FilterContext initializeFilter(FilterClassFactory filterClassFactory) {
        FilterContext newFilterContext;
        final Thread currentThread = Thread.currentThread();
        final ClassLoader previousClassLoader = currentThread.getContextClassLoader();
        final ClassLoader nextClassLoader = filterClassFactory.getClassLoader();

        try {
            currentThread.setContextClassLoader(nextClassLoader);
            final javax.servlet.Filter newFilterInstance = filterClassFactory.newInstance();

            newFilterInstance.init(filterConfig);

            newFilterContext = new FilterContext(newFilterInstance, filterClassFactory.getClassLoader());

            LOG.info("Filter: " + newFilterInstance + " successfully created");
        } catch (Throwable e) {
            LOG.error("Failed to initialize filter " + filterClassFactory + ".");
            throw(new FilterInitializationException(e.getMessage(), e));
        } finally {
            currentThread.setContextClassLoader(previousClassLoader);
        }

        return newFilterContext;
    }
}
