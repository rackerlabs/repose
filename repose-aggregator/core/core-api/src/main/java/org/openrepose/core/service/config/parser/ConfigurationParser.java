package org.openrepose.core.service.config.parser;

import org.openrepose.core.service.config.resource.ConfigurationResource;

public interface ConfigurationParser<T> {

    T read(ConfigurationResource cr);

    Class<T> configurationClass();
}
