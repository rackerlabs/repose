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
package org.openrepose.commons.utils.http.media;

import org.junit.Test;

import static org.junit.Assert.*;

public class MimeTypeTest {
    @Test
    public void shouldReturnUnknownMediaType() {
        String mediaTypeString = "application/what'sUpDoc";

        MimeType returnedMediaType = MimeType.getMatchingMimeType(mediaTypeString);

        assertEquals(MimeType.UNKNOWN, returnedMediaType);
    }

    @Test
    public void shouldReturnGuessedMediaType() {

        String mediaTypeString = "text/xml";

        MimeType returnedMediaType = MimeType.guessMediaTypeFromString(mediaTypeString);

        assertEquals(returnedMediaType.getName(), mediaTypeString);
    }

    @Test
    public void shouldReturnMatchingApplicationXmlMediaType() {
        String mediaTypeString = "application/xml";

        MimeType returnedMediaType = MimeType.getBestFitMimeType(mediaTypeString);

        assertEquals(MimeType.APPLICATION_XML, returnedMediaType);
    }

    @Test
    public void shouldReturnMatchingTextXmlMediaType() {
        String mediaTypeString = "text/xml";

        MimeType returnedMediaType = MimeType.getBestFitMimeType(mediaTypeString);

        assertEquals(MimeType.TEXT_XML, returnedMediaType);
    }

    @Test
    public void shouldReturnGuessedApplicationXmlMediaType() {
        String mediaTypeString = "application/xml+atom";

        MimeType returnedMediaType = MimeType.getBestFitMimeType(mediaTypeString);

        assertEquals(MimeType.APPLICATION_XML, returnedMediaType);
    }

    @Test
    public void shouldReturnFalseIfMediaTypesDoNotMatch() {
        assertFalse(MimeType.APPLICATION_JSON.matches(MimeType.APPLICATION_XML));
    }

    @Test
    public void shouldReturnTrueIfMediaTypesMatch() {
        assertTrue(MimeType.TEXT_PLAIN.matches(MimeType.TEXT_PLAIN));
    }

    @Test
    public void shouldReturnTrueOnWildcardMatchCall() {
        assertTrue(MimeType.WILDCARD.matches(MimeType.TEXT_XML));
    }

    @Test
    public void shouldReturnTrueOnWildcardMatchParameter() {
        assertTrue(MimeType.TEXT_XML.matches(MimeType.WILDCARD));
    }
}
