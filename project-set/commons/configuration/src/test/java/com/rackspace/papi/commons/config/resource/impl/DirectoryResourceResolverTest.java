package com.rackspace.papi.commons.config.resource.impl;

import com.rackspace.papi.commons.config.resource.ConfigurationResource;
import com.rackspace.papi.commons.config.resource.ConfigurationResourceResolver;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@RunWith(Enclosed.class)
public class DirectoryResourceResolverTest {

    public static class WhenUsingDirectoryResourceResolverTest {

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
}
