package com.rackspace.papi.components.versioning;

import com.rackspace.papi.commons.config.manager.UpdateListener;
import com.rackspace.papi.commons.util.StringUtilities;
import com.rackspace.papi.components.versioning.config.ServiceVersionMapping;
import com.rackspace.papi.components.versioning.config.ServiceVersionMappingList;
import com.rackspace.papi.components.versioning.domain.ConfigurationData;
import com.rackspace.papi.components.versioning.util.ContentTransformer;
import com.rackspace.papi.domain.Port;
import com.rackspace.papi.filter.SystemModelInterrogator;
import com.rackspace.papi.filter.logic.AbstractConfiguredFilterHandlerFactory;
import com.rackspace.papi.model.Destination;
import com.rackspace.papi.model.DomainNode;
import com.rackspace.papi.model.PowerProxy;
import com.rackspace.papi.model.ServiceDomain;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;

public class VersioningHandlerFactory extends AbstractConfiguredFilterHandlerFactory<VersioningHandler> {

   private static final Logger LOG = org.slf4j.LoggerFactory.getLogger(VersioningHandlerFactory.class);
   private final Map<String, ServiceVersionMapping> configuredMappings = new HashMap<String, ServiceVersionMapping>();
   private final Map<String, Destination> configuredHosts = new HashMap<String, Destination>();
   private final ContentTransformer transformer;
   private final List<Port> ports;
   private ServiceDomain localDomain;
   private DomainNode localHost;
   private ServiceVersionMappingList config;

   public VersioningHandlerFactory(List<Port> ports) {
      this.ports = ports;

      transformer = new ContentTransformer();
   }

   @Override
   protected Map<Class, UpdateListener<?>> getListeners() {
      return new HashMap<Class, UpdateListener<?>>() {

         {
            put(ServiceVersionMappingList.class, new VersioningConfigurationListener());
            put(PowerProxy.class, new SystemModelConfigurationListener());
         }
      };
   }

   private class SystemModelConfigurationListener implements UpdateListener<PowerProxy> {

      @Override
      public void configurationUpdated(PowerProxy configurationObject) {
         SystemModelInterrogator interrogator = new SystemModelInterrogator(configurationObject, ports);
         localDomain = interrogator.getLocalServiceDomain();
         localHost = interrogator.getLocalHost();
         List<Destination> destinations = new ArrayList<Destination>();

         destinations.addAll(localDomain.getDestinations().getEndpoint());
         destinations.addAll(localDomain.getDestinations().getTargetDomain());
         for (Destination powerApiHost : destinations) {
            configuredHosts.put(powerApiHost.getId(), powerApiHost);
         }
      }
   }

   private class VersioningConfigurationListener implements UpdateListener<ServiceVersionMappingList> {

      @Override
      public void configurationUpdated(ServiceVersionMappingList mappings) {
         if (mappings.getServiceRoot() == null || StringUtilities.isBlank(mappings.getServiceRoot().getHref())) {
            LOG.error("Service root not defined - bailing on config update");
            return;
         }

         configuredMappings.clear();

         for (ServiceVersionMapping mapping : mappings.getVersionMapping()) {
            configuredMappings.put(mapping.getId(), mapping);
         }

         config = mappings;
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
