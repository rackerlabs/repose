package com.rackspace.papi.commons.config.parser.jaxb;

import com.rackspace.papi.commons.config.resource.ConfigurationResource;
import com.rackspace.papi.commons.util.pooling.ResourceContext;
import com.rackspace.papi.commons.util.pooling.ResourceContextException;
import javax.xml.bind.Unmarshaller;

public class UnmarshallerResourceContext implements ResourceContext<Unmarshaller, Object> {

    private final ConfigurationResource cfgResource;

    public UnmarshallerResourceContext(ConfigurationResource cfgResource) {
        this.cfgResource = cfgResource;
    }
    
    @Override
    public Object perform(Unmarshaller resource) throws ResourceContextException {
        try {
            return resource.unmarshal(cfgResource.newInputStream());
        } catch(Exception ex) {
            throw new ResourceContextException("Failed to unmarshall input stream. Reason: " + ex.getMessage(), ex);
        }
    }
}
