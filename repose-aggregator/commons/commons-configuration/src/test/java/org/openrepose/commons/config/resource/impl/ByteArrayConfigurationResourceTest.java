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

import org.junit.Before;
import org.junit.Test;
import org.openrepose.commons.config.resource.ConfigurationResource;

import java.io.IOException;
import java.io.InputStream;

import static org.junit.Assert.*;

public class ByteArrayConfigurationResourceTest {

    private ConfigurationResource byteArrayConfigurationResource;

    @Before
    public void setup() {
        byte[] bytes = {1, 2, 3};
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
