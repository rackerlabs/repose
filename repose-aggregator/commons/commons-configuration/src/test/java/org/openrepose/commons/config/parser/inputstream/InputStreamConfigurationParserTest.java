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
package org.openrepose.commons.config.parser.inputstream;

import org.junit.Before;
import org.junit.Test;
import org.openrepose.commons.config.resource.ConfigurationResource;
import org.openrepose.commons.config.resource.ResourceResolutionException;

import java.io.IOException;
import java.io.InputStream;

import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

public class InputStreamConfigurationParserTest {

    private InputStreamConfigurationParser instance;
    private ConfigurationResource cr;
    private InputStream stream;
    private ConfigurationResource badCr;

    @Before
    public void setUp() throws IOException {
        instance = new InputStreamConfigurationParser();

        // good case
        cr = mock(ConfigurationResource.class);
        stream = mock(InputStream.class);
        when(cr.newInputStream()).thenReturn(stream);

        // bad case
        badCr = mock(ConfigurationResource.class);
        when(badCr.newInputStream()).thenThrow(new IOException());
    }

    @Test
    public void shouldGetInputStream() throws IOException {
        InputStream actual = instance.read(cr);
        verify(cr, times(1)).newInputStream();
        assertThat("Should get input stream", actual, sameInstance(stream));
    }

    @Test(expected = ResourceResolutionException.class)
    public void shouldThrowResourceResolutionException() {
        instance.read(badCr);
    }

    /**
     * Test of read method, of class InputStreamConfigurationParser.
     */
    @Test
    public void testRead() {
        InputStream result = instance.read(cr);
        assertNotNull(result);
        // TODO review the generated test code and remove the default call to fail.

    }
}
