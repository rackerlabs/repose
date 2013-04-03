package com.rackspace.papi.filter;

import com.oracle.javaee6.FilterType;
import com.oracle.javaee6.ParamValueType;
import com.rackspace.papi.commons.util.classloader.ear.EarClassLoaderContext;
import com.rackspace.papi.model.Filter;
import java.util.*;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

public class FilterContextManagerImpl implements FilterContextManager {

    private static final Logger LOG = LoggerFactory.getLogger(FilterContextInitializer.class);
    private final FilterConfig filterConfig;
    private final ApplicationContext applicationContext;

    public FilterContextManagerImpl(FilterConfig filterConfig, ApplicationContext applicationContext) {
        this.filterConfig = filterConfig;
        this.applicationContext = applicationContext;
    }

    @Override
    public FilterContext loadFilterContext(Filter filter, Collection<EarClassLoaderContext> loadedApplications) throws ClassNotFoundException {
        FilterClassFactory filterClassFactory = FilterContextManagerImpl.getFilterClassFactory(filter.getName(), loadedApplications);

        return initializeFilter(filterClassFactory, filter);
    }

    public static FilterClassFactory getFilterClassFactory(String filterName, Collection<EarClassLoaderContext> loadedApplications) {
        for (EarClassLoaderContext classLoaderCtx : loadedApplications) {
            final FilterType filterClass = classLoaderCtx.getEarDescriptor().getRegisteredFilters().get(filterName);

            if (filterClass != null) {
                return new FilterClassFactory(filterClass, classLoaderCtx.getClassLoader());
            }
        }

        throw new IllegalStateException("Unable to look up filter " + filterName + " - this is protected by a validation guard in a higher level of the architecture and should be logged as a defect");
    }

    private static class FilterConfigWrapper implements FilterConfig {

        private final FilterConfig parent;
        private final FilterType filterType;
        private final Map<String, String> initParams;
        private final String config;

        public FilterConfigWrapper(FilterConfig parent, FilterType filterType, String config) {
            if (parent == null) {
                throw new IllegalArgumentException("filter config cannot be null");
            }

            if (filterType == null) {
                throw new IllegalArgumentException("filter type cannot be null");
            }
            this.parent = parent;
            this.filterType = filterType;
            this.config = config;
            initParams = new HashMap<String, String>();

            initParams.put("filter-config", config);

            for (ParamValueType param : filterType.getInitParam()) {
                initParams.put(param.getParamName().getValue(), param.getParamValue().getValue());
            }
        }

        @Override
        public String getFilterName() {
            return filterType.getFilterName().getValue();
        }

        @Override
        public ServletContext getServletContext() {
            return parent.getServletContext();
        }

        @Override
        public String getInitParameter(String name) {
            return initParams.get(name);
        }

        @Override
        public Enumeration<String> getInitParameterNames() {
            return Collections.enumeration(initParams.keySet());
        }

        public String getFilterConfig() {
            return config;
        }
    }

    public FilterContext initializeFilter(FilterClassFactory filterClassFactory, Filter filter) {
        final Thread currentThread = Thread.currentThread();
        final ClassLoader previousClassLoader = currentThread.getContextClassLoader();
        final ClassLoader nextClassLoader = filterClassFactory.getClassLoader();
       
        try {
            currentThread.setContextClassLoader(nextClassLoader);
            final javax.servlet.Filter newFilterInstance = filterClassFactory.newInstance(applicationContext);

            newFilterInstance.init(new FilterConfigWrapper(filterConfig, filterClassFactory.getFilterType(), filter.getConfiguration()));
            
          

            LOG.info("Filter: " + newFilterInstance + " successfully created");

            return new FilterContext(newFilterInstance, filterClassFactory.getClassLoader(), filter);
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
