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
package org.openrepose.filters.ratelimiting.util.combine;

import org.junit.Before;
import org.junit.Test;

import javax.xml.transform.TransformerException;
import java.io.InputStream;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Created by IntelliJ IDEA.
 * User: joshualockwood
 * Date: May 9, 2011
 * Time: 4:16:16 PM
 */
public class InputStreamUriParameterTest {
    private InputStreamUriParameter inputStreamUriParameter;

    @Before
    public void setup() {
        InputStream inputStreamReference = mock(InputStream.class);
        when(inputStreamReference.toString()).thenReturn("streamRef");

        inputStreamUriParameter = new InputStreamUriParameter(inputStreamReference);
    }

    @Test
    public void shouldReturnNewStreamSourceIfIsMatchingHref() throws TransformerException {
        String validHref = "reference:jio:streamRef";

        assertNotNull(inputStreamUriParameter.resolve(validHref, null));
    }

    @Test(expected = CombinedLimitsTransformerException.class)
    public void shouldThrowExceptionIfIsNotMatchingHref() throws TransformerException {
        String validHref = "reference:jio:invalidStreamRef";

        assertNotNull(inputStreamUriParameter.resolve(validHref, null));
    }
}
