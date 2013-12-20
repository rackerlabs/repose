package com.rackspace.papi.commons.config.parser.generic;

import com.rackspace.papi.commons.config.parser.common.AbstractConfigurationObjectParser;
import com.rackspace.papi.commons.config.resource.ConfigurationResource;

public class GenericResourceConfigurationParser extends AbstractConfigurationObjectParser<ConfigurationResource> {

    public GenericResourceConfigurationParser() {
        super(ConfigurationResource.class);
    }
    
    @Override
    public ConfigurationResource read(ConfigurationResource cr) {
        return cr;
    }
    
}
