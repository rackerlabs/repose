package com.rackspace.papi.service.config.manager;

import com.rackspace.papi.service.config.parser.common.ConfigurationParser;
import com.rackspace.papi.service.config.resource.ConfigurationResource;

public interface ConfigurationUpdateManager {

     <T> void registerListener(UpdateListener<T> listener, ConfigurationResource resource, ConfigurationParser<T> parser, String filterName);

     <T> void unregisterListener(UpdateListener<T> listener, ConfigurationResource resource);
     
}
