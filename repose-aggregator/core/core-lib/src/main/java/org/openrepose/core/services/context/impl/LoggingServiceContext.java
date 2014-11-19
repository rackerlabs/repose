package org.openrepose.core.services.context.impl;

import org.openrepose.commons.config.manager.UpdateListener;
import org.openrepose.commons.utils.StringUtilities;
import org.openrepose.core.container.config.ContainerConfiguration;
import org.openrepose.core.container.config.LoggingConfiguration;
import org.openrepose.core.services.ServiceRegistry;
import org.openrepose.core.services.config.ConfigurationService;
import org.openrepose.core.services.context.ServiceContext;
import org.openrepose.core.services.logging.LoggingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import javax.servlet.ServletContextEvent;
import java.io.File;
import java.net.URL;

/**
 * @author fran
 */
public class LoggingServiceContext implements ServiceContext<LoggingService> {
   public static final String SERVICE_NAME = "powerapi:/services/logging";
   private final LoggingService loggingService;
   private ConfigurationService configurationManager;
   private final ContainerConfigurationListener configurationListener;
   private final ServiceRegistry registry;

   public LoggingServiceContext(LoggingService loggingService, ServiceRegistry registry, ConfigurationService configurationManager) {
      this.loggingService = loggingService;
      this.configurationListener = new ContainerConfigurationListener();
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
            loggingService.updateLoggingConfiguration(configurationObject.getDeploymentConfig().getLoggingConfiguration().getHref());
         }
         isInitialized = true;
      }

      @Override
      public boolean isInitialized() {
         return isInitialized;
      }
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
   }
}
