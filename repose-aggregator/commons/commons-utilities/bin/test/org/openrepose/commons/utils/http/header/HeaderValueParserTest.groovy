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
package org.openrepose.commons.utils.http.header

import org.junit.Test

import static org.hamcrest.Matchers.*
import static org.junit.Assert.assertThat

/**
 *
 * @author zinic
 */

public class HeaderValueParserTest {

    @Test
    public void shouldReturnEmptyParameterMapWhenNoParametersAreSpecified() {
        String headerValueString = "the value";
        HeaderValue headerValue = new HeaderValueParser(headerValueString).parse();
        HashMap<String, String> eMap = new HashMap<String, String>();

        assertThat(headerValue, allOf(hasProperty("parameters", equalTo(eMap)), hasProperty("value", equalTo("the value"))))
    }

    @Test
    public void shouldParseParameters() {
        String headerValueString = "the value; q=0.5; a=apple ; b= banana; c = clementine";
        HeaderValue headerValue = new HeaderValueParser(headerValueString).parse();
        HashMap<String, String> eMap = new HashMap<String, String>();
        eMap.put("a", "apple")
        eMap.put("b", "banana")
        eMap.put("c", "clementine")
        eMap.put("q", "0.5")

        assertThat(headerValue, allOf(hasProperty("parameters", equalTo(eMap)), hasProperty("qualityFactor", equalTo(Double.valueOf(0.5))),
                hasProperty("value", equalTo("the value"))))
    }

    @Test
    public void shouldAppendSemicolonIfNoEquals() {
        String headerValueString = "the; value";
        HeaderValue headerValue = new HeaderValueParser(headerValueString).parse();
        HashMap<String, String> eMap = new HashMap<String, String>();
        assertThat(headerValue, allOf(hasProperty("parameters", equalTo(eMap)), hasProperty("value", equalTo("the; value"))))
    }

    @Test
    public void concatBecauseOfEqualInValue() {
        String headerValueString = "the value; q=0.5; a=apple ; b= banana; c = clementine; z = lemon=lime";
        HeaderValue headerValue = new HeaderValueParser(headerValueString).parse();
        HashMap<String, String> eMap = new HashMap<String, String>();
        eMap.put("a", "apple")
        eMap.put("b", "banana")
        eMap.put("c", "clementine")
        eMap.put("z", "lemon=lime")
        eMap.put("q", "0.5")

        assertThat(headerValue, allOf(hasProperty("parameters", equalTo(eMap)), hasProperty("qualityFactor", equalTo(Double.valueOf(0.5))),
                hasProperty("value", equalTo("the value"))))
    }

    @Test(expected = MalformedHeaderValueException.class)
    public void throwMalformedHeaderValExceptionIfNeeded() {
        String headerValueString = ";=";
        HeaderValue headerValue = new HeaderValueParser(headerValueString).parse();
    }
}
