package com.rackspace.papi.service.config.parser.generic;

import com.rackspace.papi.service.config.parser.common.AbstractConfigurationObjectParser;
import org.openrepose.core.service.config.resource.ConfigurationResource;

public class GenericResourceConfigurationParser extends AbstractConfigurationObjectParser<ConfigurationResource> {

    public GenericResourceConfigurationParser() {
        super(ConfigurationResource.class);
    }
    
    @Override
    public ConfigurationResource read(ConfigurationResource cr) {
        return cr;
    }
    
}
