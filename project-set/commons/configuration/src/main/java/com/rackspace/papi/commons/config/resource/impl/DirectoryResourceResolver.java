package com.rackspace.papi.commons.config.resource.impl;

import com.rackspace.papi.commons.config.resource.ConfigurationResource;
import com.rackspace.papi.commons.config.resource.ConfigurationResourceResolver;
import com.rackspace.papi.commons.config.resource.ResourceResolutionException;
import com.rackspace.papi.commons.util.StringUtilities;
import java.io.File;

import java.net.MalformedURLException;
import java.net.URL;

public class DirectoryResourceResolver implements ConfigurationResourceResolver {

    private final String configurationRoot;

    public DirectoryResourceResolver(String configurationRoot) {
        this.configurationRoot = preppendFileURISpec(configurationRoot);
    }
    
    final String preppendFileURISpec(String urn) {
        return !urn.startsWith("file://") ? "file://" + urn : urn;
    }

    public String getConfigurationRoot() {
        return configurationRoot;
    }

    @Override
    public ConfigurationResource resolve(String resourceName) throws ResourceResolutionException {
        final String spec = resourceName.contains("://")? resourceName: StringUtilities.join(configurationRoot, File.separator, resourceName);
        
        try {
            return new BufferedURLConfigurationResource(new URL(spec));
        } catch (MalformedURLException murle) {
            throw new ResourceResolutionException("Unable to build URL for resource. Resource: " 
                    + spec + ". Reason: " + murle.getMessage(), murle);
        }
    }
}
