package com.rackspace.papi.service.config;

import com.rackspace.papi.commons.config.manager.ConfigurationUpdateManager;
import com.rackspace.papi.commons.config.manager.UpdateListener;
import com.rackspace.papi.commons.config.parser.common.ConfigurationParser;
import com.rackspace.papi.commons.config.resource.ConfigurationResourceResolver;
import com.rackspace.papi.commons.util.Destroyable;

public interface ConfigurationService extends Destroyable {

   void setResourceResolver(ConfigurationResourceResolver resourceResolver);
   void setUpdateManager(ConfigurationUpdateManager updateManager);
   <T> void subscribeTo(String configurationName, UpdateListener<T> listener, Class<T> configurationClass);
   <T> void subscribeTo(String configurationName, UpdateListener<T> listener, ConfigurationParser<T> customParser);
   void unsubscribeFrom(String configurationName, UpdateListener plistener);
}
