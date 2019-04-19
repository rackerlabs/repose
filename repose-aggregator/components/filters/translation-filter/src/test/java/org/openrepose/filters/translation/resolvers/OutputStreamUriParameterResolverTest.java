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
package org.openrepose.filters.translation.resolvers;

import net.sf.saxon.lib.OutputURIResolver;
import org.junit.Before;
import org.junit.Test;

import javax.xml.transform.Result;
import javax.xml.transform.TransformerException;
import javax.xml.transform.stream.StreamResult;
import java.io.OutputStream;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

public class OutputStreamUriParameterResolverTest {

    private OutputStreamUriParameterResolver resolver;
    private OutputURIResolver parent;
    private OutputStream output;
    private Result result;

    @Before
    public void setUp() throws TransformerException {
        parent = mock(OutputURIResolver.class);
        output = mock(OutputStream.class);
        resolver = new OutputStreamUriParameterResolver(parent);
        result = mock(Result.class);
        when(parent.resolve(anyString(), anyString())).thenReturn(result);

    }

    @Test
    public void shouldAddStream() throws TransformerException {
        String name = "out";
        resolver.addStream(output, name);
        String href = resolver.getHref(name);
        assertNotNull("Should return the href for our output stream", href);
        StreamResult result = (StreamResult) resolver.resolve(href, "");
        assertThat("Source stream path should not be empty", result.getSystemId(), not(isEmptyString()));
        assertThat("Should return our output stream", result.getOutputStream(), sameInstance(output));
    }

    @Test
    public void shouldCallParentResolver() throws TransformerException {
        String name = "someUri";
        String href = name;
        String base = "somebase";
        Result result = resolver.resolve(href, base);

        verify(parent).resolve(href, base);
        assertNotNull(result);

    }

    @Test(expected = RuntimeException.class)
    public void shouldThrowExceptionWhenCannotResolve() throws TransformerException {
        final String doesntExist = "reference:jio:doesn'tExist";
        resolver.resolve(doesntExist, "somebase");
    }
}
