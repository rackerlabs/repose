package com.rackspace.papi.commons.config.parser.common;

import com.rackspace.papi.commons.config.resource.ConfigurationResource;

public interface ConfigurationParser<T> {

    T read(ConfigurationResource cr);

    Class<T> configurationClass();
}
