package org.openrepose.core.spring;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/**
 * This class should not be used directly. It's built here so that it can be proven via tests.
 */
public class CoreSpringContainer {

    private final AnnotationConfigApplicationContext coreContext;

    public CoreSpringContainer(String coreScanPackage){
        coreContext = new AnnotationConfigApplicationContext();
        coreContext.setDisplayName("ReposeCoreContext");
        coreContext.scan(coreScanPackage);
    }

    public ApplicationContext getCoreContext() {
        return coreContext;
    }
}
