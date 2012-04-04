package com.rackspace.papi.service.context;

import com.rackspace.papi.commons.config.manager.UpdateListener;

import com.rackspace.papi.container.config.ContainerConfiguration;
import com.rackspace.papi.container.config.DeploymentConfiguration;

import com.rackspace.papi.service.ServiceContext;
import com.rackspace.papi.service.config.ConfigurationService;
import com.rackspace.papi.service.context.container.ContainerConfigurationService;
import com.rackspace.papi.service.context.container.ContainerConfigurationServiceImpl;
import com.rackspace.papi.service.context.jndi.ServletContextHelper;
import com.rackspace.papi.servlet.InitParameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;

public class ContainerServiceContext implements ServiceContext<ContainerConfigurationService> {

   private static final Logger LOG = LoggerFactory.getLogger(ContainerServiceContext.class);
   public static final String SERVICE_NAME = "powerapi:/services/container";
   private final ContainerConfigurationListener configurationListener;
   private ContainerConfigurationService containerConfigurationService;
   private ConfigurationService configurationManager;
   private ServletContext servletContext;

   public ContainerServiceContext() {
      this.containerConfigurationService = new ContainerConfigurationServiceImpl();
      this.configurationListener = new ContainerConfigurationListener();
   }

   @Override
   public String getServiceName() {
      return SERVICE_NAME;
   }

   @Override
   public ContainerConfigurationService getService() {
      return containerConfigurationService;
   }

   /**
    * Listens for updates to the container.cfg.xml file which holds the location of the log properties
    * file.
    */
   private class ContainerConfigurationListener implements UpdateListener<ContainerConfiguration> {

      private int determinePort(DeploymentConfiguration deployConfig) {
         int port = -1;

         if (deployConfig != null && deployConfig.getPort() != null) {
            port = deployConfig.getPort();
         } else {
            LOG.error("Service port not specified in container.cfg.xml");
         }
         
         return port;
      }

      private void setTimeoutParameters(DeploymentConfiguration deployConfig) {
         Integer connectionTimeout = deployConfig.getConnectionTimeout();
         Integer readTimeout = deployConfig.getReadTimeout();

         servletContext.setAttribute(InitParameter.CONNECTION_TIMEOUT.getParameterName(), connectionTimeout);
         LOG.info("Setting " + InitParameter.CONNECTION_TIMEOUT.getParameterName() + " to " + connectionTimeout);

         servletContext.setAttribute(InitParameter.READ_TIMEOUT.getParameterName(), readTimeout);
         LOG.info("Setting " + InitParameter.READ_TIMEOUT.getParameterName() + " to " + readTimeout);
      }
      
      @Override
      public void configurationUpdated(ContainerConfiguration configurationObject) {
         DeploymentConfiguration deployConfig = configurationObject.getDeploymentConfig();
         int currentPort = ServletContextHelper.getServerPort(servletContext);
         int port = determinePort(deployConfig);

         if (currentPort == -1) {
            // No port has been set into the servlet context

            if (port > 0) {
               containerConfigurationService = new ContainerConfigurationServiceImpl(port);
               servletContext.setAttribute(InitParameter.PORT.getParameterName(), port);
               LOG.info("Setting " + InitParameter.PORT.getParameterName() + " to " + port);
            } else {
               // current port and port specified in container.cfg.xml are -1 (not set)
               LOG.error("Cannot determine " + InitParameter.PORT.getParameterName() + ". Port must be specified in container.cfg.xml or on the command line.");
            }
         } else {
            if (port > 0 && currentPort != port) {
               // Port changed and is different from port already available in servlet context.
               LOG.warn("****** " + InitParameter.PORT.getParameterName() + " changed from " + currentPort + " --> " + port 
                       + ".  Restart is required for this change.");
            }
         }

         setTimeoutParameters(deployConfig);
      }
   }

   @Override
   public void contextInitialized(ServletContextEvent servletContextEvent) {

      servletContext = servletContextEvent.getServletContext();
      configurationManager = ServletContextHelper.getPowerApiContext(servletContext).configurationService();

      configurationManager.subscribeTo("container.cfg.xml", configurationListener, ContainerConfiguration.class);
   }

   @Override
   public void contextDestroyed(ServletContextEvent servletContextEvent) {
      if (configurationManager != null) {
         configurationManager.unsubscribeFrom("container.cfg.xml", configurationListener);
      }
   }
}
