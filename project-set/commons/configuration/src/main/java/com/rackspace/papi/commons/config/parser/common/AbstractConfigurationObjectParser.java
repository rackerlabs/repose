package com.rackspace.papi.commons.config.parser.common;

public abstract class AbstractConfigurationObjectParser<T> implements ConfigurationParser<T> {

    private final Class<T> configurationClass;

    public AbstractConfigurationObjectParser(Class<T> configurationClass) {
        this.configurationClass = configurationClass;
    }

    @Override
    public final Class<T> configurationClass() {
        return configurationClass;
    }
}
