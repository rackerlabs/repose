package org.openrepose.commons.config.resource;

public interface ConfigurationResourceResolver {
    
    ConfigurationResource resolve(String resourceName) throws ResourceResolutionException;
}
