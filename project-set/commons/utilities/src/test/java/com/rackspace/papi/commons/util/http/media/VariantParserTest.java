package com.rackspace.papi.commons.util.http.media;

import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;

/**
 * @author fran
 */
@RunWith(Enclosed.class)
public class VariantParserTest {
    private final static String STANDARD_URI = "https://servers.api.openstack.org/images";
    private final static String VERSIONED_URI = "https://servers.api.openstack.org/v1.0/images";
    private final static String MEDIA_TYPE_URI = "https://servers.api.openstack.org/images.xml";
    private final static String VERSIONED_MEDIA_TYPE_URI = "https://servers.api.openstack.org/v1.0/images.xml";

    public static class WhenCheckingVariantPattern {
        @Test
        public void shouldNotMatchBadURI() {
            assertFalse(
                    VariantParser.VARIANT_REGEX
                            .matcher("tzs:/baduri|hehe")
                            .matches());
        }

        @Test
        public void shouldMatchStandardURI() {
            assertTrue(
                    VariantParser.VARIANT_REGEX
                            .matcher(STANDARD_URI)
                            .matches());
        }

        @Test
        public void shouldMatchVersionedURI() {
            assertTrue(
                    VariantParser.VARIANT_REGEX
                            .matcher(VERSIONED_URI)
                            .matches());
        }

        @Test
        public void shouldMatchMediaTypeURI() {
            assertTrue(
                    VariantParser.VARIANT_REGEX
                            .matcher(MEDIA_TYPE_URI)
                            .matches());
        }

        @Test
        public void shouldMatchVersionedMediaTypeURI() {
            assertTrue(
                    VariantParser.VARIANT_REGEX
                            .matcher(VERSIONED_MEDIA_TYPE_URI)
                            .matches());
        }
    }

    public static class WhenGettingMediaTypeFromVariant {
        private static final String URI_WITH_JSON_MEDIA_TYPE = "http://a/variant.json";
        private static final String URI_WITH_PARAMS = "http://a/variant.xml?passTest=true&testType=.json";
        private static final String URI_WITH_MULTIPLE_MEDIA_TYPE = "http://a/variant.json/variant.xml";

        public void shouldReturnNullForVariantWithoutExtension() {
            assertNull("Variants without extensions should return null", VariantParser.getMediaTypeFromVariant(""));
        }

        @Test
        public void shouldReturnMediaType() {
            MimeType mediaType = VariantParser.getMediaTypeFromVariant(URI_WITH_JSON_MEDIA_TYPE);

            assertEquals(MimeType.APPLICATION_JSON, mediaType);
        }

        @Test
        public void shouldMatchLastVariant() {
            assertEquals(MimeType.APPLICATION_XML, VariantParser.getMediaTypeFromVariant(URI_WITH_MULTIPLE_MEDIA_TYPE));
        }

        @Test
        public void shouldIgnoreQueryParameters() {
            assertEquals(MimeType.APPLICATION_XML, VariantParser.getMediaTypeFromVariant(URI_WITH_PARAMS));
        }

        @Test
        public void shouldReturnNullWhenNoVariantContentTypeIsSpecified() {
            assertNull(VariantParser.getMediaTypeFromVariant(STANDARD_URI));
        }
    }
}
