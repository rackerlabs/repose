package org.openrepose.commons.config.parser.generic;

import org.openrepose.commons.config.parser.common.AbstractConfigurationObjectParser;
import org.openrepose.commons.config.resource.ConfigurationResource;

public class GenericResourceConfigurationParser extends AbstractConfigurationObjectParser<ConfigurationResource> {

    public GenericResourceConfigurationParser() {
        super(ConfigurationResource.class);
    }
    
    @Override
    public ConfigurationResource read(ConfigurationResource cr) {
        return cr;
    }
    
}
