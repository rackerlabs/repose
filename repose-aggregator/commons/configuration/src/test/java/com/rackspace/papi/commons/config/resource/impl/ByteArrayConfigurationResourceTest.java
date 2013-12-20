package com.rackspace.papi.commons.config.resource.impl;

import com.rackspace.papi.commons.config.resource.ConfigurationResource;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.io.InputStream;

import static org.junit.Assert.*;

@RunWith(Enclosed.class)
public class ByteArrayConfigurationResourceTest {

    public static class WhenUsingByteArrayConfigurationResource {

        private ConfigurationResource<ByteArrayConfigurationResource> byteArrayConfigurationResource;

        @Before
        public void setup() {
            byte[] bytes = {1, 2 ,3};
            String name = "my_byte_array";

            byteArrayConfigurationResource = new ByteArrayConfigurationResource(name, bytes);
        }

        @Test
        public void shouldReturnTrueForExists() throws IOException {
            assertTrue(byteArrayConfigurationResource.exists());
        }

        @Test
        public void shouldReturnName() {
            String name = "my_byte_array";

            assertEquals(name, byteArrayConfigurationResource.name());
        }

        @Test
        public void shouldReturnNewInputStream() throws IOException {
            InputStream inputStream = byteArrayConfigurationResource.newInputStream();

            assertNotNull(inputStream);
        }

        @Test
        public void shouldReturnFalseForUpdated() throws IOException {
            assertFalse(byteArrayConfigurationResource.updated());
        }
    }
}
