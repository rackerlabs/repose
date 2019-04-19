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

import org.junit.Test;
import org.openrepose.commons.config.resource.ConfigurationResource;
import org.openrepose.commons.config.resource.ConfigurationResourceResolver;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class DirectoryResourceResolverTest {

    @Test
    public void shouldResolveWithAnyValidURIScheme() {
        ConfigurationResourceResolver configResolver = new DirectoryResourceResolver("/whatevah");

        ConfigurationResource configResource = configResolver.resolve("whatevah");

        assertNotNull(configResource);
    }

    @Test
    public void shouldPrependFileUriSpecToConfigurationRoots() {
        final DirectoryResourceResolver resolver = new DirectoryResourceResolver("/etc/powerapi");

        assertEquals("Should append file uri spec to configuration root", "file:///etc/powerapi", resolver.getConfigurationRoot());
    }

    @Test
    public void shouldNotDoublePrependFileUriSpec() {
        final DirectoryResourceResolver resolver = new DirectoryResourceResolver("file:///etc/powerapi");

        assertEquals("Should append file uri spec to configuration root", "file:///etc/powerapi", resolver.getConfigurationRoot());
    }
}
