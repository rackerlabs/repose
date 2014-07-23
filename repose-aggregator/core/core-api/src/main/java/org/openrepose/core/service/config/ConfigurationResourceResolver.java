package org.openrepose.core.service.config;

import org.openrepose.core.service.config.resource.ConfigurationResource;
import org.openrepose.core.service.config.resource.ResourceResolutionException;

public interface ConfigurationResourceResolver {
    ConfigurationResource resolve(String resourceName) throws ResourceResolutionException;
}
