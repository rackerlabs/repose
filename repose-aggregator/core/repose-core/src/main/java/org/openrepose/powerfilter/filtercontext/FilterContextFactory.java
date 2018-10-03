/*
 * _=_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_=
 * Repose
 * _-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-
 * Copyright (C) 2010 - 2015 Rackspace US, Inc.
 * _-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * =_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_=_
 */
package org.openrepose.powerfilter.filtercontext;

import com.oracle.javaee6.FilterType;
import org.openrepose.commons.utils.classloader.EarClassLoaderContext;
import org.openrepose.core.services.classloader.ClassLoaderManagerService;
import org.openrepose.core.spring.CoreSpringProvider;
import org.openrepose.core.systemmodel.config.Filter;
import org.openrepose.powerfilter.FilterInitializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.AbstractApplicationContext;

import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import java.util.*;


// @TODO: This class is OBE'd with REP-7231
@Deprecated
@Named
public class FilterContextFactory {

    private static final Logger LOG = LoggerFactory.getLogger(FilterContextFactory.class);
    private final ClassLoaderManagerService classLoaderManagerService;
    private final ApplicationContext applicationContext;

    @Inject
    public FilterContextFactory(
            ApplicationContext applicationContext,
            ClassLoaderManagerService classLoaderManagerService
    ) {
        this.applicationContext = applicationContext;
        this.classLoaderManagerService = classLoaderManagerService;
    }

    public List<FilterContext> buildFilterContexts(ServletContext servletContext, List<Filter> filtersToCreate) throws FilterInitializationException {
        final List<FilterContext> filterContexts = new LinkedList<>();

        for (Filter papiFilter : filtersToCreate) {

            if (classLoaderManagerService.hasFilter(papiFilter.getName())) {
                final FilterContext context = loadFilterContext(papiFilter, classLoaderManagerService.getLoadedApplications(), servletContext);
                filterContexts.add(context);
            } else {
                LOG.error("Unable to satisfy requested filter chain - none of the loaded artifacts supply a filter named " +
                        papiFilter.getName());
                throw new FilterInitializationException("Unable to satisfy requested filter chain - none of the loaded artifacts supply a filter named " + papiFilter.getName());
            }
        }
        return filterContexts;
    }

    /**
     * Load a FilterContext for a filter
     *
     * @param filter             the Jaxb filter configuration information from the system-model
     * @param loadedApplications The list of EarClassLoaders
     * @return a FilterContext containing an instance of the filter and metatadata
     * @throws org.openrepose.powerfilter.FilterInitializationException
     */
    @SuppressWarnings("squid:S2259")
    private FilterContext loadFilterContext(Filter filter, Collection<EarClassLoaderContext> loadedApplications, ServletContext servletContext) throws FilterInitializationException {
        FilterType filterType = null;
        ClassLoader filterClassLoader = null;

        Iterator<EarClassLoaderContext> classLoaderContextIterator = loadedApplications.iterator();
        while (classLoaderContextIterator.hasNext() && filterType == null) {
            EarClassLoaderContext classLoaderContext = classLoaderContextIterator.next();
            filterType = classLoaderContext.getEarDescriptor().getRegisteredFilters().get(filter.getName());
            if (filterType != null) {
                filterClassLoader = classLoaderContext.getClassLoader();
            }
        }

        // FilterType and filterClassloader are guaranteed to not be null, by a different check in the previous method
        // So it is safe to suppress warning squid:S2259
        String filterClassName = filterType.getFilterClass().getValue();

        //We got a filter info and a classloader, we can do actual work
        try {
            LOG.info("Getting child application context for {} using classloader {}", filterClassName, filterClassLoader.toString());

            AbstractApplicationContext filterContext = CoreSpringProvider.getContextForFilter(applicationContext, filterClassLoader, filterClassName, getUniqueContextName(filter));

            //Get the specific class to load from the application context
            Class c = filterClassLoader.loadClass(filterClassName);

            javax.servlet.Filter newFilterInstance;
            try {
                newFilterInstance = (javax.servlet.Filter) filterContext.getBean(c);
            } catch (NoSuchBeanDefinitionException e) {
                LOG.debug("Could not load the filter {} using Spring. Will try to manually load the class instead.", filterClassName, e);

                //Spring didn't load the filter as a bean, try manually creating a new instance of the class
                newFilterInstance = (javax.servlet.Filter) c.newInstance();

                //Add the instance to the application context using its full class name
                filterContext.getBeanFactory().registerSingleton(
                        newFilterInstance.getClass().getName(), newFilterInstance);
            }

            newFilterInstance.init(new FilterConfigWrapper(servletContext, filterType, filter.getConfiguration()));

            LOG.info("Filter Instance: {} successfully created", newFilterInstance);

            return new FilterContext(newFilterInstance, filterContext, filter);
        } catch (ClassNotFoundException e) {
            throw new FilterInitializationException("Requested filter, " + filterClassName + " does not exist in any loaded artifacts", e);
        } catch (ServletException e) {
            LOG.error("Failed to initialize filter {}", filterClassName);
            throw new FilterInitializationException("Failed to initialize filter " + filterClassName, e);
        } catch (ClassCastException e) {
            throw new FilterInitializationException("Requested filter, " + filterClassName + " is not of type javax.servlet.Filter", e);
        } catch (InstantiationException | IllegalAccessException e) {
            throw new FilterInitializationException("Requested filter, " + filterClassName +
                    " is not an annotated Component nor does it have a public zero-argument constructor.", e);
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
}
