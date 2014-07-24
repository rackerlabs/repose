package com.rackspace.papi.service.config.manager;

import org.openrepose.core.service.config.manager.UpdateListener;
import org.openrepose.core.service.config.parser.ConfigurationParser;
import org.openrepose.core.service.config.resource.ConfigurationResource;

public interface ConfigurationUpdateManager {

     <T> void registerListener(UpdateListener<T> listener, ConfigurationResource resource, ConfigurationParser<T> parser, String filterName);

     <T> void unregisterListener(UpdateListener<T> listener, ConfigurationResource resource);
     
}
