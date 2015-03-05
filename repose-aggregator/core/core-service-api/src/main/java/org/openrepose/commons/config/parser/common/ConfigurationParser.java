package org.openrepose.commons.config.parser.common;

import org.openrepose.commons.config.resource.ConfigurationResource;

public interface ConfigurationParser<T> {

    T read(ConfigurationResource cr);

    Class<T> configurationClass();
}
