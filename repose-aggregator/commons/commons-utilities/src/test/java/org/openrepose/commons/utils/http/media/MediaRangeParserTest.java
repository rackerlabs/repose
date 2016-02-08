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

import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import java.util.List;

import static org.junit.Assert.assertEquals;

@RunWith(Enclosed.class)
public class MediaRangeParserTest {

    public static class WhenGettingMediaRangesFromAcceptHeader {

        @Test
        public void shouldParseStandardMediaRangeWithoutParameters() {
            String acceptHeader = "application/xml";

            List<MediaType> mediaRanges = new MediaRangeParser(acceptHeader).parse();

            Assert.assertEquals(MimeType.APPLICATION_XML, mediaRanges.get(0).getMimeType());
        }

        @Test
        public void shouldParseStandardMediaRangeWithParameters() {
            String acceptHeader = "application/xml; v=1.0; s=7; q=1";

            List<MediaType> mediaRanges = new MediaRangeParser(acceptHeader).parse();

            assertEquals(MimeType.APPLICATION_XML, mediaRanges.get(0).getMimeType());
            assertEquals(3, mediaRanges.get(0).getParameters().size());
        }

        @Test
        public void shouldParseAcceptHeaderWithVendorSpecificMediaRange() {
            String acceptHeader = "application/vnd.openstack.compute-v1.1+json";

            List<MediaType> mediaRanges = new MediaRangeParser(acceptHeader).parse();

            assertEquals(MimeType.APPLICATION_JSON, mediaRanges.get(0).getMimeType());
            assertEquals(acceptHeader, mediaRanges.get(0).getValue());
        }

        @Test
        public void shouldParseAcceptHeaderWithMultipleMediaRanges() {
            String acceptHeader = "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8";

            List<MediaType> mediaRanges = new MediaRangeParser(acceptHeader).parse();

            assertEquals(4, mediaRanges.size());
        }
    }
}
