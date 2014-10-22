package org.openrepose.core.spring;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.support.AbstractApplicationContext;

/**
 * THis will be eagerly loaded as soon as the JVM fires up, which is exactly what we want.
 */
public class CoreSpringProvider implements SpringProvider{
    private static Logger LOG = LoggerFactory.getLogger(CoreSpringProvider.class);
    private static CoreSpringProvider instance = new CoreSpringProvider();

    private final AnnotationConfigApplicationContext coreContext;


    private CoreSpringProvider() {
        Config conf = ConfigFactory.load("springConfiguration.conf");

        String coreScanPackage = conf.getString("coreSpringContextPath");

        LOG.debug("Creating Core spring provider singleton!");
        LOG.debug("Annotation scanning package {}", coreScanPackage);

        coreContext = new AnnotationConfigApplicationContext();
        coreContext.setDisplayName("ReposeCoreContext");
        coreContext.scan(coreScanPackage);
        coreContext.refresh();
    }

    public static SpringProvider getInstance() {
        return instance;
    }

    public ApplicationContext getCoreContext() {
        return coreContext;
    }

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
    public static AbstractApplicationContext getContextForFilter(ApplicationContext parentContext, ClassLoader loader, String className, String contextName) throws ClassNotFoundException {
        AnnotationConfigApplicationContext filterContext = new AnnotationConfigApplicationContext();
        filterContext.setClassLoader(loader);
        filterContext.setParent(parentContext);
        filterContext.setDisplayName(contextName);

        Class tehFilter = loader.loadClass(className);

        String packageToScan = tehFilter.getPackage().getName();
        LOG.debug("Filter scan package: {}", packageToScan);
        filterContext.scan(packageToScan);
        filterContext.refresh();

        return filterContext;
    }

}
