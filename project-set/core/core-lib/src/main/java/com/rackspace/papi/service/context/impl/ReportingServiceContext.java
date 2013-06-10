package com.rackspace.papi.service.context.impl;

import com.rackspace.papi.commons.config.manager.UpdateListener;
import com.rackspace.papi.container.config.ContainerConfiguration;
import com.rackspace.papi.model.*;
import com.rackspace.papi.service.ServiceRegistry;
import com.rackspace.papi.service.config.ConfigurationService;
import com.rackspace.papi.service.context.ServiceContext;
import com.rackspace.papi.service.reporting.ReportingService;
import java.net.URL;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import javax.servlet.ServletContextEvent;
import java.util.ArrayList;
import java.util.List;

@Component("reportingServiceContext")
public class ReportingServiceContext implements ServiceContext<ReportingService> {

   public static final String SERVICE_NAME = "powerapi:/services/reporting";
   private static final int DEFAULT_JMX_RESET_TIME_SECONDS = 15;
   private final ContainerConfigurationListener containerConfigurationListener;
   private final SystemModelListener systemModelListener;
   private final ConfigurationService configurationManager;
   private final ServiceRegistry registry;
   private final ReportingService reportingService;
   private final Object jmxResetTimeKey = new Object();
   private final List<String> destinationIds = new ArrayList<String>();
   private int jmxResetTime = DEFAULT_JMX_RESET_TIME_SECONDS;

   @Autowired
   public ReportingServiceContext(@Qualifier("serviceRegistry") ServiceRegistry registry,
           @Qualifier("configurationManager") ConfigurationService configurationManager,
           @Qualifier("reportingService") ReportingService reportingService) {
      this.containerConfigurationListener = new ContainerConfigurationListener();
      this.systemModelListener = new SystemModelListener();
      this.registry = registry;
      this.configurationManager = configurationManager;
      this.reportingService = reportingService;
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
   public ReportingService getService() {
      return reportingService;
   }

   @Override
   public void contextInitialized(ServletContextEvent servletContextEvent) {
      URL xsdURL = getClass().getResource("/META-INF/schema/system-model/system-model.xsd");
      URL containerXsdURL = getClass().getResource("/META-INF/schema/container/container-configuration.xsd");
      configurationManager.subscribeTo("system-model.cfg.xml",xsdURL, systemModelListener, SystemModel.class);
      configurationManager.subscribeTo("container.cfg.xml",containerXsdURL, containerConfigurationListener, ContainerConfiguration.class);
      register();
   }

   @Override
   public void contextDestroyed(ServletContextEvent servletContextEvent) {
      reportingService.shutdown();
      configurationManager.unsubscribeFrom("system-model.cfg.xml", systemModelListener);
      configurationManager.unsubscribeFrom("container.cfg.xml", containerConfigurationListener);
   }

   /**
    * Listens for updates to the container.cfg.xml file which holds the jmx-reset-time.
    */
   private class ContainerConfigurationListener implements UpdateListener<ContainerConfiguration> {

      private boolean isInitialized = false;

      @Override
      public void configurationUpdated(ContainerConfiguration configurationObject) {

          if (configurationObject.getDeploymentConfig() != null) {

              synchronized (jmxResetTimeKey) {
                  jmxResetTime = configurationObject.getDeploymentConfig().getJmxResetTime();
              }

              reportingService.updateConfiguration(destinationIds, jmxResetTime);
          }
          isInitialized = true;

      }

      @Override
      public boolean isInitialized() {
         return isInitialized;
      }
   }

   /**
    * Listens for updates to the system-model.cfg.xml file which holds the destination ids.
    */
   private class SystemModelListener implements UpdateListener<SystemModel> {

      private boolean isInitialized = false;

      @Override
      public void configurationUpdated(SystemModel systemModel) {

         final List<String> endpointIds = new ArrayList<String>();

         for (ReposeCluster reposeCluster : systemModel.getReposeCluster()) {

            final DestinationList destinations = reposeCluster.getDestinations();

            for (DestinationEndpoint endpoint : destinations.getEndpoint()) {
               endpointIds.add(endpoint.getId());
            }

            for (DestinationCluster destinationCluster : destinations.getTarget()) {
               endpointIds.add(destinationCluster.getId());
            }
         }


         synchronized (destinationIds) {
            destinationIds.clear();
            destinationIds.addAll(endpointIds);
         }

         reportingService.updateConfiguration(destinationIds, jmxResetTime);
         isInitialized = true;
      }

      @Override
      public boolean isInitialized() {
         return isInitialized;
      }
   }
}
