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
import org.apache.commons.lang3.StringUtils;

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
    public ConfigurationResource resolve(String resourceName) {
        final String spec = resourceName.contains("://") ? resourceName : StringUtils.join(configurationRoot, File.separator, resourceName);

        try {
            return new BufferedURLConfigurationResource(new URL(spec));
        } catch (MalformedURLException murle) {
            throw new ResourceResolutionException("Unable to build URL for resource. Resource: "
                    + spec + ". Reason: " + murle.getMessage(), murle);
        }
    }
}
