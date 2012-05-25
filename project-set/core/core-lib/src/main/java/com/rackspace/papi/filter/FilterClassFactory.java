package com.rackspace.papi.filter;

import com.oracle.javaee6.FilterType;
import com.rackspace.papi.servlet.PowerApiContextException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import javax.servlet.Filter;

/**
 * @author franshua
 */
public class FilterClassFactory {
    private final FilterType filterClass;
    private final ClassLoader classLoader;

    public FilterClassFactory(FilterType className, ClassLoader classLoader) {
        this.filterClass = className;
        this.classLoader = classLoader;
    }

    public ClassLoader getClassLoader() {
        return classLoader;
    }

    public void validate(Class clazz) {
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

    public Filter newInstance(ApplicationContext parentContext) throws ClassNotFoundException {
        Class clazz = classLoader.loadClass(filterClass.getFilterClass().getValue());       
        validate(clazz);

        try {
            // just loadClass in here and no need to keep Class as member
            Filter filter = (Filter) clazz.newInstance();
            if (filter instanceof ApplicationContextAware) {
               ((ApplicationContextAware)filter).setApplicationContext(parentContext);
            }
            return filter;
        } catch (InstantiationException e) {
            throw new FilterClassException("failed to create new instance of " + filterClass, e);
        } catch (IllegalAccessException e) {
            throw new FilterClassException("illegal access exception encountered when attempting to instantiate " + filterClass, e);
        }
    }
    
    public FilterType getFilterType() {
       return filterClass;
    }

    @Override
    public String toString() {
        return filterClass.getFilterClass().getValue();
    }
}