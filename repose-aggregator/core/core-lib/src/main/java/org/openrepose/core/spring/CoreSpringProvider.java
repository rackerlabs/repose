package org.openrepose.core.spring;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.jmx.export.annotation.AnnotationJmxAttributeSource;
import org.springframework.jmx.export.annotation.AnnotationMBeanExporter;

import java.util.Properties;

/**
 * This should be instantiated once during startup of repose.
 * There's an eager loaded singleton for the contraption, and the initialize core context needs to be called at least once.
 */
public class CoreSpringProvider {
    private static Logger LOG = LoggerFactory.getLogger(CoreSpringProvider.class);
    private static CoreSpringProvider instance = new CoreSpringProvider();
    private Config conf = ConfigFactory.load("springConfiguration.conf");
    private volatile boolean configured = false;

    private AnnotationConfigApplicationContext coreContext = null;

    private CoreSpringProvider() {
        //Just an empty constructor for singleton ability
    }

    public static CoreSpringProvider getInstance() {
        return instance;
    }

    /**
     * Intended to be called once by Valve or the War file on startup to configure the core context.
     * Create a core spring provider with the core properties available to all of the spring contexts.
     * The params to this constructor are the things that need to be in the core spring context for all anything to access
     *
     * @param configRoot Configuration root directory
     * @param insecure   whether or not to check SSL certs
     */
    public void initializeCoreContext(String configRoot,
                                      boolean insecure) {
        if (!configured) {
            //NOTE: the repose version should only come from here. This should be the source of truth
            String reposeVersion = conf.getString("reposeVersion");

            String coreScanPackage = conf.getString("coreSpringContextPath");

            //TODO: logger won't exist yet.... Need to ship a config for early logging
            LOG.debug("Creating Core spring provider!");
            LOG.debug("Core service annotation scanning package {}", coreScanPackage);

            LOG.debug("Config Root: {}", configRoot);
            LOG.debug(" Insecurity: {}", insecure);

            //Go ahead and print the narwhal banner at this point -- this is where startup happens
            ReposeBanner.print(LOG);

            coreContext = new AnnotationConfigApplicationContext();
            coreContext.setDisplayName("ReposeCoreContext");

            //Set up properties for core
            //http://forum.spring.io/forum/spring-projects/container/49923-how-to-set-properties-programmatically?p=394903#post394903
            // You have to actually create a PropertyPlaceholderConfigurer to use the @value stuff!
            Properties props = new Properties();
            props.put(ReposeSpringProperties.stripSpringValueStupidity(ReposeSpringProperties.CORE.CONFIG_ROOT), configRoot);
            props.put(ReposeSpringProperties.stripSpringValueStupidity(ReposeSpringProperties.CORE.INSECURE), insecure);
            props.put(ReposeSpringProperties.stripSpringValueStupidity(ReposeSpringProperties.CORE.REPOSE_VERSION), reposeVersion);

            PropertiesPropertySource mps = new PropertiesPropertySource("core-properties", props);
            coreContext.getEnvironment().getPropertySources().addFirst(mps);

            PropertySourcesPlaceholderConfigurer propConfig = new PropertySourcesPlaceholderConfigurer();
            propConfig.setEnvironment(coreContext.getEnvironment());

            coreContext.addBeanFactoryPostProcessor(propConfig);

            if (LOG.isDebugEnabled()) {
                for (PropertySource source : coreContext.getEnvironment().getPropertySources()) {
                    EnumerablePropertySource eps = (EnumerablePropertySource) source;
                    LOG.debug("COREContext - Property names for {}: {}", eps.getName(), eps.getPropertyNames());
                }
            }

            coreContext.scan(coreScanPackage);

            //Have to set up the JMX stuff by hand
            GenericBeanDefinition mBeanExporter = new GenericBeanDefinition();
            mBeanExporter.setBeanClass(AnnotationMBeanExporter.class);
            mBeanExporter.setAutowireCandidate(true);
            coreContext.registerBeanDefinition("exporter", mBeanExporter);

            GenericBeanDefinition jmxAttributeSource = new GenericBeanDefinition();
            jmxAttributeSource.setBeanClass(AnnotationJmxAttributeSource.class);
            jmxAttributeSource.setAutowireCandidate(true);
            coreContext.registerBeanDefinition("jmxAttributeSource", jmxAttributeSource);

            coreContext.refresh();

            //TODO: set up the JMX mbean stuff
            /**
             *
             <bean id="exporter" class="org.springframework.jmx.export.annotation.AnnotationMBeanExporter" lazy-init="true">
             <property name="namingStrategy" ref="reposeJmxNamingStrategy"/>
             </bean>

             <!--
             <bean id="namingStrategy" class="org.openrepose.core.spring.ReposeJmxNamingStrategy">
             <property name="attributeSource" ref="attributeSource"/>
             </bean>

             <bean id="jmxAttributeSource"
             class="org.springframework.jmx.export.metadata.JmxAttributeSource"/>
             -->
             <bean id="jmxAttributeSource"
             class="org.springframework.jmx.export.annotation.AnnotationJmxAttributeSource"/>
             */

            configured = true;
        } else {
            //TODO: should this throw some kind of exception, or just log failures silently?
            //It's a programming error that multiple calls to initialize get called, and so having to catch an exception is annoying
            LOG.error("Additional call to initialize Core Context ignored!");
        }
    }

    public ApplicationContext getCoreContext() {
        if (!configured) {
            LOG.error("Core Context requested before configured!");
        }
        return coreContext;
    }

    /**
     * Get an application context, with the core context as it's parent, for a clusterId and a nodeId.
     * Then things can be scanned. Like node specific services and such. (distDatastore needs this)
     * per-node services must exist under org.openrepose.nodeservice to be scanned and picked up at this phase
     *
     * @param clusterId
     * @param nodeId
     */
    public AbstractApplicationContext getNodeContext(String clusterId, String nodeId) {
        AnnotationConfigApplicationContext nodeContext = new AnnotationConfigApplicationContext();
        nodeContext.setParent(getCoreContext());

        nodeContext.setDisplayName(clusterId + "-" + nodeId + "-context");

        Properties props = new Properties();
        props.put(ReposeSpringProperties.stripSpringValueStupidity(ReposeSpringProperties.NODE.NODE_ID), nodeId);
        props.put(ReposeSpringProperties.stripSpringValueStupidity(ReposeSpringProperties.NODE.CLUSTER_ID), clusterId);
        PropertiesPropertySource mps = new PropertiesPropertySource(clusterId + "-" + nodeId + "-" + "props", props);
        nodeContext.getEnvironment().getPropertySources().addFirst(mps);

        if (LOG.isDebugEnabled()) {
            for (PropertySource source : nodeContext.getEnvironment().getPropertySources()) {
                EnumerablePropertySource eps = (EnumerablePropertySource) source;
                LOG.debug("NODEContext - Property names for {}: {}", eps.getName(), eps.getPropertyNames());
            }
        }

        PropertySourcesPlaceholderConfigurer propConfig = new PropertySourcesPlaceholderConfigurer();
        propConfig.setEnvironment(nodeContext.getEnvironment());
        nodeContext.addBeanFactoryPostProcessor(propConfig);

        String nodeServicePackage = conf.getString("nodeSpringContextPath");
        LOG.debug("Creating node service context for {}-{}", clusterId, nodeId);
        LOG.debug("Node service annotation scanning package {}", nodeServicePackage);

        nodeContext.scan(nodeServicePackage);
        nodeContext.refresh();

        return nodeContext;
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

        LOG.debug("Creating Filter Context using parent context: {}", parentContext.getDisplayName());

        Class tehFilter = loader.loadClass(className);

        String packageToScan = tehFilter.getPackage().getName();
        LOG.debug("Filter Context scan package: {}", packageToScan);

        PropertySourcesPlaceholderConfigurer propConfig = new PropertySourcesPlaceholderConfigurer();
        propConfig.setEnvironment(filterContext.getEnvironment());
        filterContext.addBeanFactoryPostProcessor(propConfig);

        filterContext.scan(packageToScan);
        filterContext.refresh();

        if (LOG.isDebugEnabled()) {
            for (String s : filterContext.getBeanDefinitionNames()) {
                LOG.debug("FilterContext bean: {}", s);
            }
        }

        return filterContext;
    }

}
