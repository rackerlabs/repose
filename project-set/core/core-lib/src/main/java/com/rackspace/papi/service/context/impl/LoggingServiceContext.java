package com.rackspace.papi.service.context.impl;

import com.rackspace.papi.commons.config.manager.UpdateListener;
import com.rackspace.papi.commons.config.parser.properties.PropertiesFileConfigurationParser;
import com.rackspace.papi.commons.util.StringUtilities;
import com.rackspace.papi.container.config.ContainerConfiguration;
import com.rackspace.papi.container.config.LoggingConfiguration;
import com.rackspace.papi.service.ServiceRegistry;
import com.rackspace.papi.service.config.ConfigurationService;
import com.rackspace.papi.service.context.ServiceContext;
import com.rackspace.papi.service.logging.LoggingService;
import java.net.URL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletContextEvent;
import java.util.Properties;

/**
 * @author fran
 */
public class LoggingServiceContext implements ServiceContext<LoggingService> {

   private static final Logger LOG = LoggerFactory.getLogger(LoggingServiceContext.class);
   public static final String SERVICE_NAME = "powerapi:/services/logging";
   private final LoggingService loggingService;
   private ConfigurationService configurationManager;
   private final ContainerConfigurationListener configurationListener;
   private final LoggingConfigurationListener loggingConfigurationListener;
   private String loggingConfigurationConfig = "";
   private final ServiceRegistry registry;

   public LoggingServiceContext(LoggingService loggingService, ServiceRegistry registry, ConfigurationService configurationManager) {
      this.loggingService = loggingService;
      this.configurationListener = new ContainerConfigurationListener();
      this.loggingConfigurationListener = new LoggingConfigurationListener();
      this.configurationManager = configurationManager;
      this.registry = registry;
   }

   public void register() {
      if (registry != null) {
         registry.addService(this);
      }
   }

   @Override
   public String getServiceName() {
      return SERVICE_NAME;
   }

   @Override
   public LoggingService getService() {
      return loggingService;
   }

   /**
    * Listens for updates to the container.cfg.xml file which holds the location of the log properties file.
    */
   private class ContainerConfigurationListener implements UpdateListener<ContainerConfiguration> {

      private boolean isInitialized = false;

      @Override
      public void configurationUpdated(ContainerConfiguration configurationObject) {

         if (configurationObject.getDeploymentConfig() != null) {
            final LoggingConfiguration loggingConfig = configurationObject.getDeploymentConfig().getLoggingConfiguration();

            if (loggingConfig != null && !StringUtilities.isBlank(loggingConfig.getHref())) {
               final String newLoggingConfig = loggingConfig.getHref();
               loggingConfigurationConfig = newLoggingConfig;
               updateLogConfigFileSubscription(loggingConfigurationConfig, newLoggingConfig);
            }
         }
         isInitialized = true;
      }

      @Override
      public boolean isInitialized() {
         return isInitialized;
      }
   }

   /**
    * Listens for updates to the log properties file.
    */
   private class LoggingConfigurationListener implements UpdateListener<Properties> {

      private boolean isInitialized = false;

      @Override
      public void configurationUpdated(Properties configurationObject) {
         loggingService.updateLoggingConfiguration(configurationObject);

         LOG.error("ERROR LEVEL LOG STATEMENT");
         LOG.warn("WARN LEVEL LOG STATEMENT");
         LOG.info("INFO LEVEL LOG STATEMENT");
         LOG.debug("DEBUG LEVEL LOG STATEMENT");
         LOG.trace("TRACE LEVEL LOG STATEMENT");
         isInitialized = true;
      }

      @Override
      public boolean isInitialized() {
         return isInitialized;
      }
   }

   private void updateLogConfigFileSubscription(String currentLoggingConfig, String loggingConfig) {

      configurationManager.unsubscribeFrom(currentLoggingConfig, loggingConfigurationListener);
      configurationManager.subscribeTo("", loggingConfig, loggingConfigurationListener, new PropertiesFileConfigurationParser());
   }

   @Override
   public void contextInitialized(ServletContextEvent servletContextEvent) {
      URL containerXsdURL = getClass().getResource("/META-INF/schema/container/container-configuration.xsd");

      configurationManager.subscribeTo("container.cfg.xml",containerXsdURL, configurationListener, ContainerConfiguration.class);
      register();
   }

   @Override
   public void contextDestroyed(ServletContextEvent servletContextEvent) {
      configurationManager.unsubscribeFrom("container.cfg.xml", configurationListener);
      configurationManager.unsubscribeFrom(loggingConfigurationConfig, loggingConfigurationListener);
   }
}
