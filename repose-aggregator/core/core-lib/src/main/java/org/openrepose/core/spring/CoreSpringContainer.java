package org.openrepose.core.spring;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.support.AbstractApplicationContext;

/**
 * This class should not be used directly. It's built here so that it can be proven via tests.
 */
public class CoreSpringContainer {

    private final AnnotationConfigApplicationContext coreContext;

    public CoreSpringContainer(String coreScanPackage){
        coreContext = new AnnotationConfigApplicationContext();
        coreContext.setDisplayName("ReposeCoreContext");
        coreContext.scan(coreScanPackage);
        coreContext.refresh();
    }

    /**
     * Just gets you the core application context
     * @return the core application context
     */
    public ApplicationContext getCoreContext() {
        return coreContext;
    }

    /**
     * Provides an application context for a filter, given a classloader as to where that filter is.
     * The application context will have that classloader set to it.
     * @param loader the classloader from where to find the filter
     * @param className the class of the filter
     * @param contextName the given name of the context
     * @return the application context for that filter
     * @throws ClassNotFoundException
     */
    public AbstractApplicationContext getContextForFilter(ClassLoader loader, String className, String contextName) throws ClassNotFoundException {
        AnnotationConfigApplicationContext filterContext = new AnnotationConfigApplicationContext();
        filterContext.setClassLoader(loader);
        filterContext.setParent(getCoreContext());
        filterContext.setDisplayName(contextName);

        Class tehFilter = loader.loadClass(className);

        filterContext.scan(tehFilter.getPackage().getName());
        filterContext.refresh();

        return filterContext;
    }
}
