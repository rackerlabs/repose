package org.openrepose.core.filter;

import com.oracle.javaee6.FilterType;
import com.oracle.javaee6.ParamValueType;
import org.openrepose.commons.utils.classloader.ear.EarClassLoaderContext;
import org.openrepose.core.spring.CoreSpringProvider;
import org.openrepose.core.spring.SpringProvider;
import org.openrepose.core.systemmodel.Filter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanNotOfRequiredTypeException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.ApplicationContext;

import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import java.util.*;

public class FilterContextManager {

    private static final Logger LOG = LoggerFactory.getLogger(FilterContextInitializer.class);
    private final FilterConfig filterConfig;

    public FilterContextManager(FilterConfig filterConfig) {
        this.filterConfig = filterConfig;
    }

    public FilterContext loadFilterContext(Filter filter, Collection<EarClassLoaderContext> loadedApplications) throws FilterInitializationException {
        FilterType filterType = null;
        ClassLoader filterClassLoader = null;
        for (EarClassLoaderContext classLoaderContext : loadedApplications) {
            filterType = classLoaderContext.getEarDescriptor().getRegisteredFilters().get(filter.getName());
            if (filterType != null) {
                filterClassLoader = classLoaderContext.getClassLoader();
            }
        }

        if (filterType != null && filterClassLoader != null) {
            String filterClassName = filterType.getFilterClass().getValue();
            //We got a filter info and a classloader, we can do actual work
            try {
                LOG.info("Getting child application context for {} using classloader {}", filterType.getFilterClass().getValue(), filterClassLoader.toString());
                SpringProvider spring = CoreSpringProvider.getInstance();
                ApplicationContext filterContext = spring.getContextForFilter(filterClassLoader, filterType.getFilterClass().getValue(), getUniqueContextName(filter));

                //TODO: This used to have a wrapped classloader change thingy around it per thread, hopefully that's not actually necessary...
                Class c = filterClassLoader.loadClass(filterType.getFilterClass().getValue());
                //TODO: could have a classcast exception
                final javax.servlet.Filter newFilterInstance = (javax.servlet.Filter)filterContext.getBean(c);

                newFilterInstance.init(new FilterConfigWrapper(filterConfig, filterType, filter.getConfiguration()));

                LOG.info("Filter Instance: {} successfully created", newFilterInstance);

                return new FilterContext(newFilterInstance, filterClassLoader, filter);
            } catch (ClassNotFoundException | ServletException e) {
                LOG.error("Failed to initialize filter {}", filterClassName);
                throw new FilterInitializationException(e.getMessage(), e);
            } catch (BeanNotOfRequiredTypeException e) {
                throw new FilterInitializationException("Provided filter, \""
                        + filterClassName
                        + "\" does not implement javax.servlet.Filter - this class is unusable as a filter.");
            } catch (NoSuchBeanDefinitionException e) {
                LOG.error("Unable to find an annotated bean for {}", filterClassName, e);
                throw new FilterInitializationException("Requested filter, \"" + filterClassName + "\"" +
                        " is not an annotated Component. It must be annotated with @Component or @Named to be loaded");
            }
        } else {
            throw new FilterInitializationException("No deployed artifact found to satisfy filter: " + filter.getName());
        }
    }

    private String getUniqueContextName(Filter filterInfo) {
        StringBuilder sb = new StringBuilder();
        if (filterInfo.getId() != null) {
            sb.append(filterInfo.getId()).append("-");
        }
        sb.append(filterInfo.getName()).append("-");
        sb.append(UUID.randomUUID().toString());
        return sb.toString();
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
}
