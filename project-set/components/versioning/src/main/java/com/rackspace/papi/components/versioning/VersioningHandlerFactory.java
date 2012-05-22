package com.rackspace.papi.components.versioning;

import com.rackspace.papi.commons.config.manager.UpdateListener;
import com.rackspace.papi.components.versioning.config.ServiceVersionMapping;
import com.rackspace.papi.components.versioning.config.ServiceVersionMappingList;
import com.rackspace.papi.components.versioning.domain.ConfigurationData;
import com.rackspace.papi.components.versioning.util.ContentTransformer;
import com.rackspace.papi.domain.ServicePorts;
import com.rackspace.papi.filter.SystemModelInterrogator;
import com.rackspace.papi.filter.logic.AbstractConfiguredFilterHandlerFactory;
import com.rackspace.papi.model.Destination;
import com.rackspace.papi.model.Node;
import com.rackspace.papi.model.ReposeCluster;
import com.rackspace.papi.model.SystemModel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class VersioningHandlerFactory extends AbstractConfiguredFilterHandlerFactory<VersioningHandler> {

   private final Map<String, ServiceVersionMapping> configuredMappings = new HashMap<String, ServiceVersionMapping>();
   private final Map<String, Destination> configuredHosts = new HashMap<String, Destination>();
   private final ContentTransformer transformer;
   private final ServicePorts ports;
   private ReposeCluster localDomain;
   private Node localHost;

   public VersioningHandlerFactory(ServicePorts ports) {
      this.ports = ports;

      transformer = new ContentTransformer();
   }

   @Override
   protected Map<Class, UpdateListener<?>> getListeners() {
      return new HashMap<Class, UpdateListener<?>>() {

         {
            put(ServiceVersionMappingList.class, new VersioningConfigurationListener());
            put(SystemModel.class, new SystemModelConfigurationListener());
         }
      };
   }

   private class SystemModelConfigurationListener implements UpdateListener<SystemModel> {

      @Override
      public void configurationUpdated(SystemModel configurationObject) {
         SystemModelInterrogator interrogator = new SystemModelInterrogator(ports);
         localDomain = interrogator.getLocalServiceDomain(configurationObject);
         localHost = interrogator.getLocalHost(configurationObject);
         List<Destination> destinations = new ArrayList<Destination>();

         destinations.addAll(localDomain.getDestinations().getEndpoint());
         destinations.addAll(localDomain.getDestinations().getTarget());
         for (Destination powerApiHost : destinations) {
            configuredHosts.put(powerApiHost.getId(), powerApiHost);
         }
      }
   }

   private class VersioningConfigurationListener implements UpdateListener<ServiceVersionMappingList> {

      @Override
      public void configurationUpdated(ServiceVersionMappingList mappings) {
         configuredMappings.clear();

         for (ServiceVersionMapping mapping : mappings.getVersionMapping()) {
            configuredMappings.put(mapping.getId(), mapping);
         }

      }
   }

   @Override
   protected VersioningHandler buildHandler() {
      final Map<String, ServiceVersionMapping> copiedVersioningMappings = new HashMap<String, ServiceVersionMapping>(configuredMappings);
      final Map<String, Destination> copiedHostDefinitions = new HashMap<String, Destination>(configuredHosts);

      final ConfigurationData configData = new ConfigurationData(localDomain, localHost, copiedHostDefinitions, copiedVersioningMappings);

      return new VersioningHandler(configData, transformer);
   }
}
