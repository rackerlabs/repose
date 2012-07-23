package com.rackspace.papi.commons.config.resource.impl;

import com.rackspace.papi.commons.config.resource.ConfigurationResource;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import java.net.MalformedURLException;
import java.net.URL;

import static org.junit.Assert.assertEquals;

@RunWith(Enclosed.class)
public class BufferedURLConfigurationResourceTest {

    public static class WhenUsingBufferedURLConfigurationResource {

        @Test
        public void shouldReturnName() throws MalformedURLException {
            String urlString = "file:/META-INF/test/test.properties";
            
            URL url = new URL(urlString);

            ConfigurationResource configResource = new BufferedURLConfigurationResource(url);

            assertEquals(urlString, configResource.name());
        }
    }
}
