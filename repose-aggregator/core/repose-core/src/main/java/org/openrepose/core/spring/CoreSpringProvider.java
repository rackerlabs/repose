/*
 * _=_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_=
 * Repose
 * _-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-
 * Copyright (C) 2010 - 2015 Rackspace US, Inc.
 * _-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * =_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_=_
 */
package org.openrepose.core.spring;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
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
import org.springframework.jmx.support.MBeanServerFactoryBean;
import org.springframework.scheduling.annotation.ScheduledAnnotationBeanPostProcessor;

import java.util.Properties;

/**
 * This should be instantiated once during startup of repose.
 * There's an eager loaded singleton for the contraption, and the initialize core context needs to be called at least once.
 */
public class CoreSpringProvider {
    private static final Logger LOG = LoggerFactory.getLogger(CoreSpringProvider.class);
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

        if (LOG.isTraceEnabled()) {
            for (String s : filterContext.getBeanDefinitionNames()) {
                LOG.trace("FilterContext bean: {}", s);
            }
        }

        return filterContext;
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

            LOG.info("Starting up Repose Core Spring Context (logging may be redirected as the logging service comes up)");
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

            if (LOG.isTraceEnabled()) {
                for (PropertySource source : coreContext.getEnvironment().getPropertySources()) {
                    EnumerablePropertySource eps = (EnumerablePropertySource) source;
                    LOG.trace("COREContext - Property names for {}: {}", eps.getName(), eps.getPropertyNames());
                }
            }

            // Enabled Spring scheduling
            coreContext.register(ScheduledAnnotationBeanPostProcessor.class);

            coreContext.scan(coreScanPackage);

            //Additionally, if configured to, scan another package
            if (conf.hasPath("extendedSpringContextPath")) {
                final String extendedServicePackage = conf.getString("extendedSpringContextPath");
                coreContext.scan(extendedServicePackage);
                LOG.info("Scanning additional service path = {}", extendedServicePackage);
            }

            //Have to set up the JMX stuff by hand

            //Create an MBeanServer and only use the one... But if we're in a container, locate an existing server
            // I'm not sure how well this will work, maybe it will fix the problem.
            GenericBeanDefinition mBeanServer = new GenericBeanDefinition();
            mBeanServer.setBeanClass(MBeanServerFactoryBean.class);
            MutablePropertyValues mBeanServerProps = new MutablePropertyValues();
            //This should tell spring to ENSURE that it'll use an existing server
            mBeanServerProps.add("locateExistingServerIfPossible", true);
            mBeanServer.setPropertyValues(mBeanServerProps);
            //Would have to give this an agent id?
            coreContext.registerBeanDefinition("reposeMBeanServer", mBeanServer);

            GenericBeanDefinition jmxAttributeSource = new GenericBeanDefinition();
            jmxAttributeSource.setBeanClass(AnnotationJmxAttributeSource.class);
            coreContext.registerBeanDefinition("jmxAttributeSource", jmxAttributeSource);

            GenericBeanDefinition jmxNamingStrategy = new GenericBeanDefinition();
            jmxNamingStrategy.setBeanClass(ReposeJmxNamingStrategy.class);
            jmxNamingStrategy.setAutowireMode(AbstractBeanDefinition.AUTOWIRE_CONSTRUCTOR);

            //This creates the primary JMX MBean exporter... There should be only one.
            GenericBeanDefinition mBeanExporter = new GenericBeanDefinition();
            mBeanExporter.setBeanClass(AnnotationMBeanExporter.class);
            MutablePropertyValues mBeanExporterProps = new MutablePropertyValues();
            mBeanExporterProps.add("autodetect", true);
            mBeanExporterProps.add("server", mBeanServer);
            mBeanExporterProps.add("namingStrategy", jmxNamingStrategy);
            mBeanExporter.setPropertyValues(mBeanExporterProps);
            coreContext.registerBeanDefinition("exporter", mBeanExporter);

            coreContext.refresh();

            //Here I can actually log the thing, and it'll go to the log instead of to the stdout
            ReposeBanner.print(LOG);

            //Make sure that once it's configured, it's registered to be shut down
            coreContext.registerShutdownHook();

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

        if (LOG.isTraceEnabled()) {
            for (PropertySource source : nodeContext.getEnvironment().getPropertySources()) {
                EnumerablePropertySource eps = (EnumerablePropertySource) source;
                LOG.trace("NODEContext - Property names for {}: {}", eps.getName(), eps.getPropertyNames());
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

}
