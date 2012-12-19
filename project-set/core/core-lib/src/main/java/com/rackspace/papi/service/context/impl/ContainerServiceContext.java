package com.rackspace.papi.service.context.impl;

import com.rackspace.papi.commons.config.manager.UpdateListener;
import com.rackspace.papi.container.config.ContainerConfiguration;
import com.rackspace.papi.container.config.DeploymentConfiguration;
import com.rackspace.papi.domain.Port;
import com.rackspace.papi.domain.ServicePorts;
import com.rackspace.papi.service.ServiceRegistry;
import com.rackspace.papi.service.config.ConfigurationService;
import com.rackspace.papi.service.context.ServiceContext;
import com.rackspace.papi.service.context.ServletContextHelper;
import com.rackspace.papi.service.context.container.ContainerConfigurationService;
import com.rackspace.papi.service.context.container.ContainerConfigurationServiceImpl;
import com.rackspace.papi.servlet.InitParameter;
import java.net.URL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;

@Component("containerServiceContext")
public class ContainerServiceContext implements ServiceContext<ContainerConfigurationService> {

   private static final Logger LOG = LoggerFactory.getLogger(ContainerServiceContext.class);
   public static final String SERVICE_NAME = "powerapi:/services/container";
   private final ContainerConfigurationListener configurationListener;
   private ContainerConfigurationService containerConfigurationService;
   private ConfigurationService configurationManager;
   private ServletContext servletContext;
   private ServicePorts servicePorts;
   private final ServiceRegistry registry;

   @Autowired
   public ContainerServiceContext(
           @Qualifier("containerConfigurationService") ContainerConfigurationService containerConfigurationService,
           @Qualifier("servicePorts") ServicePorts servicePorts,
           @Qualifier("serviceRegistry") ServiceRegistry registry,
           @Qualifier("configurationManager") ConfigurationService configurationManager) {
      this.containerConfigurationService = containerConfigurationService;
      this.configurationListener = new ContainerConfigurationListener();
      this.servicePorts = servicePorts;
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
   public ContainerConfigurationService getService() {
      return containerConfigurationService;
   }
   
   /**
     * Listens for updates to the container.cfg.xml file which holds the location of the log properties file.
     */
   private class ContainerConfigurationListener implements UpdateListener<ContainerConfiguration> {

      private ServicePorts determinePorts(DeploymentConfiguration deployConfig) {
         ServicePorts ports = new ServicePorts();

         if (deployConfig != null) {
            if (deployConfig.getHttpPort() != null) {
               ports.add(new Port("http", deployConfig.getHttpPort()));
            } else {
               LOG.error("Http service port not specified in container.cfg.xml");
            }

            if (deployConfig.getHttpsPort() != null) {
               ports.add(new Port("https", deployConfig.getHttpsPort()));
            } else {
               LOG.info("Https service port not specified in container.cfg.xml");
            }
         }

         return ports;
      }

      @Override
      public void configurationUpdated(ContainerConfiguration configurationObject) {
         DeploymentConfiguration deployConfig = configurationObject.getDeploymentConfig();
         ServicePorts currentPorts = ServletContextHelper.getInstance().getServerPorts(servletContext);
         ServicePorts ports = determinePorts(deployConfig);
         String via = deployConfig.getVia();
         int maxResponseContentSize=deployConfig.getContentBodyReadLimit().intValue();
         
         if (currentPorts.isEmpty()) {
            // No port has been set into the servlet context

            if (!ports.isEmpty()) {
               containerConfigurationService = new ContainerConfigurationServiceImpl(ports,via,maxResponseContentSize);
               servicePorts.clear();
               servicePorts.addAll(ports);
               LOG.info("Setting " + InitParameter.PORT.getParameterName() + " to " + ports);
            } else {
               // current port and port specified in container.cfg.xml are -1 (not set)
               LOG.error("Cannot determine " + InitParameter.PORT.getParameterName() + ". Port must be specified in container.cfg.xml or on the command line.");
            }
         } else {
            if (!currentPorts.equals(ports)) {
               // Port changed and is different from port already available in servlet context.
               LOG.warn("****** " + InitParameter.PORT.getParameterName() + " changed from " + currentPorts + " --> " + ports
                       + ".  Restart is required for this change.");
            } else {
               containerConfigurationService.setVia(via);
               containerConfigurationService.setContentBodyReadLimit(maxResponseContentSize);
               servicePorts.clear();
               servicePorts.addAll(ports);
            }
         }
         
      }
   }

   @Override
   public void contextInitialized(ServletContextEvent servletContextEvent) {
      this.servletContext = servletContextEvent.getServletContext();
      URL xsdURL = getClass().getResource("/META-INF/schema/container/container-configuration.xsd");
      configurationManager.subscribeTo("container.cfg.xml",xsdURL, configurationListener, ContainerConfiguration.class);
      register();
   }

   @Override
   public void contextDestroyed(ServletContextEvent servletContextEvent) {
      if (configurationManager != null) {
         configurationManager.unsubscribeFrom("container.cfg.xml", configurationListener);
      }
   }
}
