package com.rackspace.papi.filter;

import com.oracle.javaee6.FilterType;
import com.rackspace.papi.servlet.PowerApiContextException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import javax.servlet.Filter;

public class FilterClassFactory {
    private final FilterType filterClass;
    private final ClassLoader classLoader;
    private static final Logger LOG = LoggerFactory.getLogger(FilterClassFactory.class);

    public FilterClassFactory(FilterType className, ClassLoader classLoader) {
        this.filterClass = className;
        this.classLoader = classLoader;
    }

    public ClassLoader getClassLoader() {
        return classLoader;
    }

    /**
     * TODO this should probably not be public
     * @param clazz
     */
    public void validate(Class clazz) {
        //TODO: I don't think it's possible to get null here....
        if (clazz == null) {
            throw new PowerApiContextException("No deployed artifact found to satisfy required filter, \""
                    + filterClass
                    + "\" - please verify that the artifact directory contains the required artifacts.");
        }

        if (!javax.servlet.Filter.class.isAssignableFrom(clazz)) {
            throw new PowerApiContextException("Provided filter, \""
                    + clazz.getCanonicalName()
                    + "\" does not implement javax.servlet.Filter - this class is unusable as a filter.");
        }
    }

    /**
     * Creates a new filter bean based on a child context, and get that filter bean and ship it as the filter.
     * @param applicationContext The parent application context
     * @return A filter
     * @throws ClassNotFoundException
     */
    public Filter newInstance(final ApplicationContext applicationContext) throws ClassNotFoundException {
        Class clazz = classLoader.loadClass(filterClass.getFilterClass().getValue());       
        validate(clazz);

        //OH GOD SPRING
        LOG.info("Creating child application context for {} using classLoader {}", filterClass.getFilterClass().getValue(), classLoader.toString());
        AnnotationConfigApplicationContext filterApplicationContext = new AnnotationConfigApplicationContext();
        filterApplicationContext.setClassLoader(classLoader);
        filterApplicationContext.setParent(applicationContext);
        LOG.debug("Scanning package for child context: {}", clazz.getPackage().getName());
        filterApplicationContext.scan(clazz.getPackage().getName());
        LOG.debug("Refreshing child application context");
        filterApplicationContext.refresh();

        LOG.debug("Getting a filter from the child application context");
        Filter filter = (Filter) filterApplicationContext.getBean(clazz);

        LOG.debug("ACQUIRED FILTER BEAN: {}", filter.toString());
        return filter;
    }
    
    public FilterType getFilterType() {
       return filterClass;
    }

    @Override
    public String toString() {
        return filterClass.getFilterClass().getValue();
    }
}