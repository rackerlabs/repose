package com.rackspace.papi.commons.util.http.media;

import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(Enclosed.class)
public class MediaRangeParserTest {

    public static class WhenCheckingAcceptHeaderPattern {

        private static final String BASIC_CONTENT_TYPE = "application/xml";
        private static final String VENDOR_COMPLEX_CONTENT_TYPE = "application/vnd.rackspace.services.a+xml";
        private static final String FULL_COMPLEX_CONTENT_TYPE = "application/vnd.rackspace.services.a-v1.0+xml";
        private static final String ALL_MEDIA_TYPES = "*/*";
        private static final String ALL_MEDIA_SUBTYPES = "application/*";
        private static final String SUPER_COMPLICATED_MEDIA_TYPE = "application/vnd.rackspace.services.a+xml; x=v1.0; q=.8, application/json; q=.5";

        @Test
        public void should1() {
            assertTrue(
                    MediaRangeParser.ACCEPTS_HEADER_REGEX.matcher(BASIC_CONTENT_TYPE).matches());
        }

        @Test
        public void should1_2() {
            assertFalse(
                    MediaRangeParser.ACCEPTS_HEADER_REGEX.matcher("").matches());
        }

        @Test
        public void should2() {
            assertTrue(
                    MediaRangeParser.ACCEPTS_HEADER_REGEX.matcher(FULL_COMPLEX_CONTENT_TYPE).matches());
        }

        @Test
        public void should3() {
            assertTrue(
                    MediaRangeParser.ACCEPTS_HEADER_REGEX.matcher(VENDOR_COMPLEX_CONTENT_TYPE).matches());
        }

        @Test
        public void should4() {
            assertTrue(
                    MediaRangeParser.ACCEPTS_HEADER_REGEX.matcher(ALL_MEDIA_TYPES).matches());
        }

        @Test
        public void should5() {
            assertTrue(
                    MediaRangeParser.ACCEPTS_HEADER_REGEX.matcher(ALL_MEDIA_SUBTYPES).matches());
        }

        @Test
        public void should6() {
            assertTrue(
                    MediaRangeParser.ACCEPTS_HEADER_REGEX.matcher(SUPER_COMPLICATED_MEDIA_TYPE).matches());
        }
    }

    public static class WhenGettingMediaRangesFromAcceptHeader {

        @Test
        public void shouldParseStandardMediaRangeWithoutParameters() {
            String acceptHeader = "application/xml";

            List<MediaRange> mediaRanges = MediaRangeParser.getMediaRangesFromAcceptHeader(acceptHeader);

            Assert.assertEquals(MediaType.APPLICATION_XML, mediaRanges.get(0).getMediaType());
        }

        @Test
        public void shouldParseStandardMediaRangeWithParameters() {
            String acceptHeader = "application/xml; v=1.0; s=7; q=1";

            List<MediaRange> mediaRanges = MediaRangeParser.getMediaRangesFromAcceptHeader(acceptHeader);

            assertEquals(MediaType.APPLICATION_XML, mediaRanges.get(0).getMediaType());
            assertEquals(3, mediaRanges.get(0).getParameters().size());
        }

        @Test
        public void shouldParseAcceptHeaderWithVendorSpecificMediaRange() {
            String acceptHeader = "application/vnd.openstack.compute-v1.1+json";

            List<MediaRange> mediaRanges = MediaRangeParser.getMediaRangesFromAcceptHeader(acceptHeader);

            assertEquals(MediaType.APPLICATION_JSON, mediaRanges.get(0).getMediaType());
            assertEquals(acceptHeader, mediaRanges.get(0).getVendorSpecificMediaType());
        }

        @Test
        public void shouldParseAcceptHeaderWithMultipleMediaRanges() {
            String acceptHeader = "application/vnd.openstack.compute-v1.1+xml, application/json; q=1.0, application/atom";

            List<MediaRange> mediaRanges = MediaRangeParser.getMediaRangesFromAcceptHeader(acceptHeader);

            assertEquals(3, mediaRanges.size());
        }
    }
}
