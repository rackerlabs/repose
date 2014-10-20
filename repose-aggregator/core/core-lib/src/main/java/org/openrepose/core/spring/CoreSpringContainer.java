package org.openrepose.core.spring;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.support.AbstractApplicationContext;

/**
 * This class should not be used directly. It's built here so that it can be proven via tests.
 */
public class CoreSpringContainer implements SpringProvider {
    private final Logger LOG = LoggerFactory.getLogger(CoreSpringContainer.class);

    private final AnnotationConfigApplicationContext coreContext;

    public CoreSpringContainer(String coreScanPackage) {
        coreContext = new AnnotationConfigApplicationContext();
        coreContext.setDisplayName("ReposeCoreContext");
        coreContext.scan(coreScanPackage);
        coreContext.refresh();
    }

    public ApplicationContext getCoreContext() {
        return coreContext;
    }

    public AbstractApplicationContext getContextForFilter(ClassLoader loader, String className, String contextName) throws ClassNotFoundException {
        AnnotationConfigApplicationContext filterContext = new AnnotationConfigApplicationContext();
        filterContext.setClassLoader(loader);
        filterContext.setParent(getCoreContext());
        filterContext.setDisplayName(contextName);

        Class tehFilter = loader.loadClass(className);

        String packageToScan = tehFilter.getPackage().getName();
        LOG.debug("Filter scan package: {}", packageToScan);
        filterContext.scan(packageToScan);
        filterContext.refresh();

        return filterContext;
    }
}
