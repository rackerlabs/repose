package org.openrepose.commons.config.manager;

import org.openrepose.commons.config.parser.common.ConfigurationParser;
import org.openrepose.commons.config.resource.ConfigurationResource;
import org.openrepose.commons.utils.Destroyable;

public interface ConfigurationUpdateManager extends Destroyable {

     <T> void registerListener(UpdateListener<T> listener, ConfigurationResource resource, ConfigurationParser<T> parser, String filterName);

     <T> void unregisterListener(UpdateListener<T> listener, ConfigurationResource resource);
     
}
