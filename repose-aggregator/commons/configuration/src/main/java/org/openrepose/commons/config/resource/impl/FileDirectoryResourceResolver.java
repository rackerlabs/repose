/*
 * _=_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_=
 * Repose
 * _-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-
 * Copyright (C) 2010 - 2015 Rackspace US, Inc.
 * _-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * =_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_=_
 */
package org.openrepose.commons.config.resource.impl;

import org.openrepose.commons.config.resource.ConfigurationResource;
import org.openrepose.commons.config.resource.ConfigurationResourceResolver;
import org.openrepose.commons.config.resource.ResourceResolutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * @author malconis
 */
public class FileDirectoryResourceResolver implements ConfigurationResourceResolver {

    private static final Logger LOG = LoggerFactory.getLogger(FileDirectoryResourceResolver.class);
    private final String configRoot;

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
            LOG.trace("Unable to build URL for resource, it is a configuration file", murle);
        }
        return null;
    }
}
