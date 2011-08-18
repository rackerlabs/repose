package com.rackspace.papi.commons.config.parser;

import com.rackspace.papi.commons.config.resource.ConfigurationResource;

public interface ConfigurationObjectParser<T> {

    T read(ConfigurationResource cr);

    Class<T> configurationClass();
}
