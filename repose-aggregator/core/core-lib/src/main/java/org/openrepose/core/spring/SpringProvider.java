package org.openrepose.core.spring;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.AbstractApplicationContext;

/**
 * Defining the common interface for something that provides a Spring Context
 */
public interface SpringProvider {

    /**
     * Just gets you the core application context
     *
     * @return the core application context
     */
    public ApplicationContext getCoreContext();

    /**
     * Provides an application context for a filter, given a classloader as to where that filter is.
     * The application context will have that classloader set to it.
     *
     * @param loader      the classloader from where to find the filter
     * @param className   the class of the filter
     * @param contextName the given name of the context
     * @return the application context for that filter
     * @throws ClassNotFoundException
     */
    public AbstractApplicationContext getContextForFilter(ClassLoader loader, String className, String contextName) throws ClassNotFoundException;


}
