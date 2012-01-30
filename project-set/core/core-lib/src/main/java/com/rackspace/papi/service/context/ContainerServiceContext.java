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

      @Override
      public void configurationUpdated(ContainerConfiguration configurationObject) {
         DeploymentConfiguration deployConfig = configurationObject.getDeploymentConfig();
         int currentPort = ServletContextHelper.getServerPort(servletContext);

         if (deployConfig != null && deployConfig.getPort() != null) {
            Integer port = deployConfig.getPort();
            containerConfigurationService = new ContainerConfigurationServiceImpl(port);
            servletContext.setAttribute(InitParameter.PORT.getParameterName(), port);
            LOG.info("Setting " + InitParameter.PORT.getParameterName() + ": " + currentPort + " --> " + port);
         } else {
            LOG.warn("Service port not specified in container.cfg.xml, using: " + currentPort);
         }
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
