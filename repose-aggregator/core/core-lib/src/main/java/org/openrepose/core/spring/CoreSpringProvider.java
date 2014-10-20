package org.openrepose.core.spring;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.AbstractApplicationContext;

/**
 * THis will be eagerly loaded as soon as the JVM fires up, which is exactly what we want.
 */
public class CoreSpringProvider implements SpringProvider{
    private static volatile CoreSpringProvider instance = new CoreSpringProvider();

    private CoreSpringContainer coreSpringContainer;

    private CoreSpringProvider() {
        //TODO: probably a property
        coreSpringContainer = new CoreSpringContainer("org.openrepose.core");
    }

    public static SpringProvider getInstance() {
        return instance;
    }

    public ApplicationContext getCoreContext() {
        return coreSpringContainer.getCoreContext();
    }

    public AbstractApplicationContext getContextForFilter(ClassLoader loader, String className, String contextName) throws ClassNotFoundException {
        return coreSpringContainer.getContextForFilter(loader, className, contextName);
    }
}
