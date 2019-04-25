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
package org.openrepose.commons.config.parser.properties;

import org.junit.Before;
import org.junit.Test;
import org.openrepose.commons.config.resource.ConfigurationResource;
import org.openrepose.commons.config.resource.ResourceResolutionException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PropertiesFileConfigurationParserTest {

    private PropertiesFileConfigurationParser instance;
    private ConfigurationResource cr;
    private ConfigurationResource badCr;
    private Properties props;

    @Before
    public void setUp() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        instance = new PropertiesFileConfigurationParser();

        props = new Properties();
        props.setProperty("key", "value");
        props.setProperty("key2", "some other value");
        props.store(out, "TEST");
        cr = mock(ConfigurationResource.class);
        when(cr.newInputStream()).thenReturn(new ByteArrayInputStream(out.toByteArray()));
        badCr = mock(ConfigurationResource.class);
        when(badCr.newInputStream()).thenThrow(new IOException());
    }

    @Test
    public void shouldReturnValidPropertiesFile() {
        Properties actual = instance.read(cr);
        assertEquals("Should get properties file", props, actual);
    }

    @Test(expected = ResourceResolutionException.class)
    public void testRead() {
        instance.read(badCr);
    }

}
