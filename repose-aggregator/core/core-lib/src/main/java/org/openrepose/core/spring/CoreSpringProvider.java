package org.openrepose.core.spring;

import org.springframework.context.ApplicationContext;

/**
 * THis will be eagerly loaded as soon as the JVM fires up, which is exactly what we want.
 */
public class CoreSpringProvider {
    private static volatile CoreSpringProvider instance = new CoreSpringProvider();

    private CoreSpringContainer coreSpringContainer;

    private CoreSpringProvider() {
        //TODO: probably a property
        coreSpringContainer = new CoreSpringContainer("org.openrepose.core");
    }

    public static CoreSpringProvider getInstance() {
        return instance;
    }


    public ApplicationContext getCoreContext(){
        return coreSpringContainer.getCoreContext();
    }
}
