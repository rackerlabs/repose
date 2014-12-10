package org.openrepose.core.spring;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.core.env.MapPropertySource;

import java.util.HashMap;
import java.util.Map;

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
     * @param configRoot    Configuration root directory
     * @param insecure      whether or not to check SSL certs
     */
    public void initializeCoreContext(String configRoot,
                                      boolean insecure) {
        if(!configured) {
            //NOTE: the repose version should only come from here. This should be the source of truth
            String reposeVersion = conf.getString("reposeVersion");

            String coreScanPackage = conf.getString(
                    "coreSpringContextPath"); //TODO: could get this from a param now...

            LOG.debug("Creating Core spring provider!");
            LOG.debug("Core service annotation scanning package {}", coreScanPackage);

            //Go ahead and print the narwhal banner at this point -- this is where startup happens
            ReposeBanner.print(LOG);

            coreContext = new AnnotationConfigApplicationContext();
            coreContext.setDisplayName("ReposeCoreContext");

            //Set up properties for core
            Map<String, Object> props = new HashMap<>();
            props.put(ReposeSpringProperties.CORE.CONFIG_ROOT, configRoot);
            props.put(ReposeSpringProperties.CORE.INSECURE, insecure);
            props.put(ReposeSpringProperties.CORE.REPOSE_VERSION, reposeVersion);

            MapPropertySource mps = new MapPropertySource("core-properties", props);
            coreContext.getEnvironment().getPropertySources().addFirst(mps);

            coreContext.scan(coreScanPackage);
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
        if(!configured){
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

        Map<String, Object> props = new HashMap<>();
        props.put(ReposeSpringProperties.NODE.NODE_ID, nodeId);
        props.put(ReposeSpringProperties.NODE.CLUSTER_ID, clusterId);
        MapPropertySource mps = new MapPropertySource(clusterId + "-" + nodeId + "-" + "props", props);
        nodeContext.getEnvironment().getPropertySources().addFirst(mps);

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

        Class tehFilter = loader.loadClass(className);

        String packageToScan = tehFilter.getPackage().getName();
        LOG.debug("Filter scan package: {}", packageToScan);
        filterContext.scan(packageToScan);
        filterContext.refresh();

        return filterContext;
    }

}
