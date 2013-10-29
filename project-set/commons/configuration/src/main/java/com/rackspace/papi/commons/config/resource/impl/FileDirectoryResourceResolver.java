package com.rackspace.papi.commons.config.resource.impl;

import com.rackspace.papi.commons.config.resource.ConfigurationResource;
import com.rackspace.papi.commons.config.resource.ConfigurationResourceResolver;
import com.rackspace.papi.commons.config.resource.ResourceResolutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * @author malconis
 */
public class FileDirectoryResourceResolver implements ConfigurationResourceResolver {

    private final String configRoot;
    private static final Logger LOG = LoggerFactory.getLogger(FileDirectoryResourceResolver.class);

    public FileDirectoryResourceResolver(String configRoot) {

        this.configRoot = configRoot;
    }

    public String getConfigRoot() {
        return this.configRoot;
    }

    @Override
    public ConfigurationResource resolve(String resourceName) {

        File spec = null;
        URL configURL = isResolvable(resourceName);

        try {
            if (configURL == null) {
                spec = new File(configRoot, resourceName);
                configURL = spec.toURI().toURL();
            }
            return new BufferedURLConfigurationResource(configURL);

        } catch (MalformedURLException murle) {
            throw new ResourceResolutionException("Unable to build URL for resource. Resource: "
                    + resourceName + ". Reason: " + murle.getMessage(), murle);
        } catch (IllegalArgumentException ex) {
            throw new ResourceResolutionException("Unable to build URL for resource. Resource: " + resourceName + ". Reason: " + ex.getMessage(), ex);
        }
    }

    private URL isResolvable(String resourceName) {

        try {
            return new URL(resourceName);
        } catch (MalformedURLException murle) {
            LOG.trace("Unable to build URL for resource, it is a configuration file");
        }
        return null;
    }
}
