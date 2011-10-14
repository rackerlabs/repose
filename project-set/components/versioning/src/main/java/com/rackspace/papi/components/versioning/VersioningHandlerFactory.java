package com.rackspace.papi.components.versioning;

import com.rackspace.papi.commons.config.manager.UpdateListener;
import org.slf4j.Logger;
import com.rackspace.papi.commons.util.StringUtilities;
import com.rackspace.papi.components.versioning.config.ServiceVersionMapping;
import com.rackspace.papi.components.versioning.config.ServiceVersionMappingList;
import com.rackspace.papi.components.versioning.domain.ConfigurationData;
import com.rackspace.papi.components.versioning.util.ContentTransformer;
import com.rackspace.papi.filter.SystemModelInterrogator;
import com.rackspace.papi.filter.logic.AbstractConfiguredFilterHandlerFactory;
import com.rackspace.papi.model.Host;
import com.rackspace.papi.model.PowerProxy;
import java.util.HashMap;
import java.util.Map;

public class VersioningHandlerFactory extends AbstractConfiguredFilterHandlerFactory<VersioningHandler> {

   private static final Logger LOG = org.slf4j.LoggerFactory.getLogger(VersioningHandlerFactory.class);
   private final Map<String, ServiceVersionMapping> configuredMappings = new HashMap<String, ServiceVersionMapping>();
   private final Map<String, Host> configuredHosts = new HashMap<String, Host>();
   private final ContentTransformer transformer;
   private Host localHost;
   private ServiceVersionMappingList config;

   public VersioningHandlerFactory() {
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
         localHost = new SystemModelInterrogator(configurationObject).getLocalHost();

         for (Host powerApiHost : configurationObject.getHost()) {
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
      final Map<String, Host> copiedHostDefinitions = new HashMap<String, Host>(configuredHosts);

      final ConfigurationData configData = new ConfigurationData(config.getServiceRoot().getHref(), localHost, copiedHostDefinitions, copiedVersioningMappings);

      return new VersioningHandler(configData, transformer);
   }
}
