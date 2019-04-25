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

import org.junit.Before;
import org.junit.Test;

import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.URIResolver;
import javax.xml.transform.stream.StreamSource;
import java.io.InputStream;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class InputStreamUriParameterResolverTest {

    private InputStreamUriParameterResolver parent;
    private InputStreamUriParameterResolver resolver;
    private InputStream input;

    @Before
    public void setUp() {
        parent = mock(InputStreamUriParameterResolver.class);
        resolver = new InputStreamUriParameterResolver(parent);
        input = mock(InputStream.class);
    }

    @Test
    public void shouldAddStream() throws TransformerException {
        String name = "data";
        String href = resolver.getHref(name);
        String actualHref = resolver.addStream(input, name);

        assertEquals("HREFs should be equal", href, actualHref);
        StreamSource source = (StreamSource) resolver.resolve(href, "base");
        assertThat("Source stream path should not be empty", source.getSystemId(), not(isEmptyString()));
        assertThat("Streams should be the same", source.getInputStream(), sameInstance(input));
    }

    @Test
    public void shouldRemoveStreamByName() throws TransformerException {
        String name = "data";
        String href = resolver.getHref(name);
        String actualHref = resolver.addStream(input, name);

        assertEquals("HREFs should be equal", href, actualHref);
        StreamSource source = (StreamSource) resolver.resolve(href, "base");
        assertThat("Source stream path should not be empty", source.getSystemId(), not(isEmptyString()));
        assertThat("Streams should be the same", source.getInputStream(), sameInstance(input));
        resolver.removeStream(href);
        source = (StreamSource) resolver.resolve(href, "base");

        assertNull(source);
    }

    @Test
    public void shouldRemoveStream() throws TransformerException {
        String href = resolver.getHref(input);
        resolver.addStream(input);

        StreamSource source = (StreamSource) resolver.resolve(href, "base");
        assertThat("Source stream path should not be empty", source.getSystemId(), not(isEmptyString()));
        assertThat("Streams should be the same", source.getInputStream(), sameInstance(input));
        resolver.removeStream(input);
        source = (StreamSource) resolver.resolve(href, "base");

        assertNull(source);
    }

    @Test
    public void shouldCallParentResolver() throws TransformerException {
        String href = "otherdata";
        String base = "base";
        resolver.resolve(href, base);
        verify(parent).resolve(href, base);
    }

    @Test
    public void shouldCallAdditionalResolver() throws TransformerException {
        String href = "otherdata";
        String base = "base";
        URIResolver additional = mock(URIResolver.class);
        resolver.addResolver(additional);

        resolver.resolve(href, base);
        verify(parent).resolve(href, base);
        verify(additional).resolve(href, base);
    }

    @Test
    public void shouldReturnSourceOfAdditionalResolver() throws TransformerException {
        String href = "otherdata";
        String base = "base";
        URIResolver additional = mock(URIResolver.class);
        Source source = mock(Source.class);
        when(additional.resolve(anyString(), anyString())).thenReturn(source);
        resolver.addResolver(additional);

        Source actual = resolver.resolve(href, base);
        assertThat("Should return our additional source", actual, sameInstance(source));
    }
}
