package com.rackspace.papi.filter;

import com.rackspace.papi.servlet.PowerApiContextException;
import javax.servlet.Filter;

/**
 * @author franshua
 */
public class FilterClassFactory {
    private final String className;
    private final ClassLoader classLoader;

    public FilterClassFactory(String className, ClassLoader classLoader) {
        this.className = className;
        this.classLoader = classLoader;
    }

    public ClassLoader getClassLoader() {
        return classLoader;
    }

    public void validate(Class clazz) {
        if (clazz == null) {
            throw new PowerApiContextException("No deployed artifact found to satisfy required filter, \""
                    + className
                    + "\" - please verify that the artifact directory contains the required artifacts.");
        }

        if (!javax.servlet.Filter.class.isAssignableFrom(clazz)) {
            throw new PowerApiContextException("Provided filter, \""
                    + clazz.getCanonicalName()
                    + "\" does not implement javax.servlet.Filter - this class is unusable as a filter.");
        }
    }

    public Filter newInstance() throws ClassNotFoundException {
        Class clazz = classLoader.loadClass(className);       
        validate(clazz);

        try {
            // just loadClass in here and no need to keep Class as member
            return (Filter) clazz.newInstance();
        } catch (InstantiationException e) {
            throw new FilterClassException("failed to create new instance of " + className, e);
        } catch (IllegalAccessException e) {
            throw new FilterClassException("illegal access exception encountered when attempting to instantiate " + className, e);
        }
    }

    @Override
    public String toString() {
        return className;
    }
}