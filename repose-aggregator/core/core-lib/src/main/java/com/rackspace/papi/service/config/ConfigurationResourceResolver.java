package com.rackspace.papi.service.config;

import com.rackspace.papi.commons.config.resource.ConfigurationResource;
import com.rackspace.papi.commons.config.resource.ResourceResolutionException;

public interface ConfigurationResourceResolver {
    ConfigurationResource resolve(String resourceName) throws ResourceResolutionException;
}
