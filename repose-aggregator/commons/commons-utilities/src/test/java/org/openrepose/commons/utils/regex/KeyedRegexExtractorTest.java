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
package org.openrepose.commons.utils.regex;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * @author zinic
 */
public class KeyedRegexExtractorTest {

    @Test
    public void shouldReturnFirstCaptureGroup() {
        final KeyedRegexExtractor<Object> extractor = new KeyedRegexExtractor<>();
        final Object expectedKey = new Object();

        final String pattern = "a([^z]+)z";
        extractor.addPattern(pattern, expectedKey);

        ExtractorResult result = extractor.extract("abcdz");
        assertEquals("Extractor should always return the first capture group when find(...) returns true", "bcd", result.getResult());
        assertEquals("Extractor should return matched key", expectedKey, result.getKey());
    }

    @Test
    public void shouldUseNullKeys() {
        final KeyedRegexExtractor<Object> extractor = new KeyedRegexExtractor<>();

        final String pattern = "a([^z]+)z";
        extractor.addPattern(pattern);

        ExtractorResult result = extractor.extract("abcdz");
        assertEquals("Extractor should always return the first capture group when find(...) returns true", "bcd", result.getResult());
        assertNull("Extractor should return null keys when patterns are added without them", result.getKey());
    }

    @Test
    public void shouldCaptureUserWithNegativeId() {
        final KeyedRegexExtractor<Object> extractor = new KeyedRegexExtractor<>();
        final Object expectedKey = new Object();

        final String pattern = ".*/servers/([-|\\w]+)/?.*";
        extractor.addPattern(pattern, expectedKey);


        ExtractorResult result = extractor.extract("http://n01.repose.org/servers/-384904");
        assertEquals("Extractor should always return the first capture group when find(...) returns true", "-384904", result.getResult());
        assertEquals("Extractor should return matched key", expectedKey, result.getKey());
    }
}
