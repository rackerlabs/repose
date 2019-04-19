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
package org.openrepose.commons.utils.logging.apache;

import org.junit.Test;

import java.util.regex.Matcher;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.openrepose.commons.utils.logging.apache.LogConstants.PATTERN;

public class LogArgumentGroupExtractorTest {

    @Test
    public void shouldExtractEscapedPercent() {
        final String template = "%%";
        final LogArgumentGroupExtractor expected = LogArgumentGroupExtractor.instance("", "", "", "", "%");
        final Matcher m = PATTERN.matcher(template);

        m.find();

        LogArgumentGroupExtractor extractor = new LogArgumentGroupExtractor(m);

        assertEquals(expected, extractor);
    }

    @Test
    public void shouldExtractVariables() {
        final String template = "%{SOMEVAR}i";
        final LogArgumentGroupExtractor expected = LogArgumentGroupExtractor.instance("", "", "SOMEVAR", "", "i");
        final Matcher m = PATTERN.matcher(template);

        m.find();

        LogArgumentGroupExtractor extractor = new LogArgumentGroupExtractor(m);

        assertEquals(expected, extractor);
    }

    @Test
    public void shouldExtractStatusCodes() {
        final String template = "%100,200,300{SOMEVAR}i";
        final LogArgumentGroupExtractor expected = LogArgumentGroupExtractor.instance("", "100,200,300", "SOMEVAR", "", "i");
        final Matcher m = PATTERN.matcher(template);

        m.find();

        LogArgumentGroupExtractor extractor = new LogArgumentGroupExtractor(m);

        assertEquals(expected, extractor);
    }

    @Test
    public void shouldExtractNegatedStatusCodes() {
        final String template = "%!100,200,300{SOMEVAR}i";
        final LogArgumentGroupExtractor expected = LogArgumentGroupExtractor.instance("", "!100,200,300", "SOMEVAR", "", "i");
        final Matcher m = PATTERN.matcher(template);

        m.find();

        LogArgumentGroupExtractor extractor = new LogArgumentGroupExtractor(m);

        assertEquals(expected, extractor);
    }

    @Test
    public void shouldExtractLifeCycleModifiers() {
        final String template = "%>!100,200,300{SOMEVAR}i";
        final LogArgumentGroupExtractor expected = LogArgumentGroupExtractor.instance(">", "!100,200,300", "SOMEVAR", "", "i");
        final Matcher m = PATTERN.matcher(template);

        m.find();

        LogArgumentGroupExtractor extractor = new LogArgumentGroupExtractor(m);

        assertEquals(expected, extractor);
        assertEquals(">", extractor.getLifeCycleModifier());
    }

    @Test
    public void shouldExtractFormats() {
        final String template = "%>!100,200,300{SOMEVAR format1,format2}i";
        final LogArgumentGroupExtractor expected = LogArgumentGroupExtractor.instance(">", "!100,200,300", "SOMEVAR", "format1,format2", "i");
        final Matcher m = PATTERN.matcher(template);

        m.find();

        LogArgumentGroupExtractor extractor = new LogArgumentGroupExtractor(m);

        assertEquals(expected, extractor);
        assertEquals(2, extractor.getArguments().size());
        assertEquals(">", extractor.getLifeCycleModifier());
    }

    @Test
    public void shouldExtractFormats2() {
        final String template = "%>!100,200,300{SOMEVAR format1 format2}i";
        final LogArgumentGroupExtractor expected = LogArgumentGroupExtractor.instance(">", "!100,200,300", "SOMEVAR", "format1,format2", "i");
        final Matcher m = PATTERN.matcher(template);

        m.find();

        LogArgumentGroupExtractor extractor = new LogArgumentGroupExtractor(m);

        assertEquals(expected, extractor);
        assertEquals(2, extractor.getArguments().size());
        assertEquals(">", extractor.getLifeCycleModifier());
    }

    @Test
    public void shouldHaveSameHashCode1() {
        final LogArgumentGroupExtractor e1 = LogArgumentGroupExtractor.instance("", "!100,200,300", "SOMEVAR", "", "i");
        final LogArgumentGroupExtractor e2 = LogArgumentGroupExtractor.instance(null, "!100,200,300", "SOMEVAR", "", "i");

        assertEquals(e1.hashCode(), e2.hashCode());
    }

    @Test
    public void shouldHaveSameHashCode2() {
        final LogArgumentGroupExtractor e1 = LogArgumentGroupExtractor.instance("1", "", "SOMEVAR", "", "i");
        final LogArgumentGroupExtractor e2 = LogArgumentGroupExtractor.instance("1", null, "SOMEVAR", "", "i");

        assertEquals(e1.hashCode(), e2.hashCode());
    }

    @Test
    public void shouldHaveSameHashCode3() {
        final LogArgumentGroupExtractor e1 = LogArgumentGroupExtractor.instance("", "!100,200,300", "", "", "i");
        final LogArgumentGroupExtractor e2 = LogArgumentGroupExtractor.instance(null, "!100,200,300", null, "", "i");

        assertEquals(e1.hashCode(), e2.hashCode());
    }

    @Test
    public void shouldHaveSameHashCode4() {
        final LogArgumentGroupExtractor e1 = LogArgumentGroupExtractor.instance("", "!100,200,300", "SOMEVAR", "", "");
        final LogArgumentGroupExtractor e2 = LogArgumentGroupExtractor.instance(null, "!100,200,300", "SOMEVAR", "", null);

        assertEquals(e1.hashCode(), e2.hashCode());
    }

    @Test
    public void shouldBeEqual1() {
        final LogArgumentGroupExtractor e1 = LogArgumentGroupExtractor.instance("", "!100,200,300", "SOMEVAR", "", "i");
        final LogArgumentGroupExtractor e2 = LogArgumentGroupExtractor.instance("", "!100,200,300", "SOMEVAR", "", "i");

        assertEquals(e1, e2);
    }

    @Test
    public void shouldNotBeEqualWhenComparingNullToEmpty1() {
        final LogArgumentGroupExtractor e1 = LogArgumentGroupExtractor.instance("", "!100,200,300", "SOMEVAR", "", "i");
        final LogArgumentGroupExtractor e2 = LogArgumentGroupExtractor.instance(null, "!100,200,300", "SOMEVAR", "", "i");

        assertFalse(e1.equals(e2));
    }

    @Test
    public void shouldNotBeEqual1() {
        final LogArgumentGroupExtractor e1 = LogArgumentGroupExtractor.instance("", "!100,200,300", "SOMEVAR", "", "i");

        assertFalse(e1.equals(new Object()));
    }
}
