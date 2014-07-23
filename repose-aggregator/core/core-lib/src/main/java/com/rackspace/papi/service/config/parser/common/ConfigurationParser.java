package com.rackspace.papi.service.config.parser.common;

import com.rackspace.papi.service.config.resource.ConfigurationResource;

public interface ConfigurationParser<T> {

    T read(ConfigurationResource cr);

    Class<T> configurationClass();
}
