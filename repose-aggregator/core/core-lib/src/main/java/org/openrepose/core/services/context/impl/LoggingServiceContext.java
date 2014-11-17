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
public class LoggingServiceContext implements ServiceContext<LoggingService>, ApplicationContextAware {

   private static final Logger LOG = LoggerFactory.getLogger(LoggingServiceContext.class);
   public static final String SERVICE_NAME = "powerapi:/services/logging";
   private final LoggingService loggingService;
   private ConfigurationService configurationManager;
   private final ContainerConfigurationListener configurationListener;
   private File loggingConfigurationFile = null;
   private final ServiceRegistry registry;
   private ApplicationContext applicationContext;

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

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
       this.applicationContext = applicationContext;
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
               final File newLoggingConfigFile = new File(loggingConfig.getHref());
               if(loggingConfigurationFile == null ||
                        !newLoggingConfigFile.getAbsolutePath().equals(loggingConfigurationFile.getAbsolutePath())) {
                  loggingConfigurationFile = newLoggingConfigFile;
                  LoggingServiceContext.this.loggingConfigurationFile = newLoggingConfigFile;
                  loggingService.updateLoggingConfiguration(LoggingServiceContext.this.loggingConfigurationFile);

                  LOG.error("ERROR LEVEL LOG STATEMENT");
                  LOG.warn("WARN  LEVEL LOG STATEMENT");
                  LOG.info("INFO  LEVEL LOG STATEMENT");
                  LOG.debug("DEBUG LEVEL LOG STATEMENT");
                  LOG.trace("TRACE LEVEL LOG STATEMENT");
                }
            }
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
