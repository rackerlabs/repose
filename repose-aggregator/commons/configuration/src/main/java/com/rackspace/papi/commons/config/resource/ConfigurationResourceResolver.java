package com.rackspace.papi.commons.config.resource;

public interface ConfigurationResourceResolver {
    
    ConfigurationResource resolve(String resourceName) throws ResourceResolutionException;
}
