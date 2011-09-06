package com.rackspace.papi.commons.util.http.media;

import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;

/**
 * @author fran
 */
@RunWith(Enclosed.class)
public class MediaRangeTest {
    public static class WhenCheckingIfEqual {
        @Test
        public void shouldReturnTrueIfComparingEqualTypes() {
            MediaType mediaType = MediaType.fromMediaTypeString("application/vnd.rackspace.services.a-v1.0+xml");
            MediaRange oneMediaRange = new MediaRange(mediaType);
            MediaRange twoMediaRange = new MediaRange(mediaType);

            assertTrue(oneMediaRange.equals(twoMediaRange));
        }

        @Test
        public void shouldReturnFalseIfComparingADifferentType() {
            MediaType mediaType = MediaType.fromMediaTypeString("application/vnd.rackspace.services.a-v1.0+xml");
            MediaRange oneMediaRange = new MediaRange(mediaType);

            assertFalse(oneMediaRange.equals("another object"));
        }

//        @Test
//        public void shouldReturnXmlMediaTypeAndVendor() {
//            String mediaTypeString = "application/vnd.openstack.compute-v1.0+xml";
//
//            MediaType returnedMediaType = MediaType.UNKNOWN.fromMediaTypeString(mediaTypeString);
//
//            assertEquals(MediaType.APPLICATION_XML, returnedMediaType);
//            assertEquals(mediaTypeString, returnedMediaType.getVendorSpecificType());
//        }
//
//        @Test
//        public void shouldReturnXmlMediaTypeAndNullVendor() {
//            String mediaTypeString = "application/xml";
//
//            MediaType returnedMediaType = MediaType.UNKNOWN.fromMediaTypeString(mediaTypeString);
//
//            assertEquals(MediaType.APPLICATION_XML, returnedMediaType);
//            assertNull(returnedMediaType.getVendorSpecificType());
//        }
//
//        @Test
//        public void shouldReturnJsonMediaTypeAndVendor() {
//            String mediaTypeString = "application/vnd.openstack.compute-v1.0+json";
//
//            MediaType returnedMediaType = MediaType.UNKNOWN.fromMediaTypeString(mediaTypeString);
//
//            assertEquals(MediaType.APPLICATION_JSON, returnedMediaType);
//            assertEquals(mediaTypeString, returnedMediaType.getVendorSpecificType());
//        }
//
//        @Test
//        public void shouldReturnJsonMediaTypeAndNullVendor() {
//            String mediaTypeString = "application/json";
//
//            MediaType returnedMediaType = MediaType.UNKNOWN.fromMediaTypeString(mediaTypeString);
//
//            assertEquals(MediaType.APPLICATION_JSON, returnedMediaType);
//            assertNull(returnedMediaType.getVendorSpecificType());
//        }
    }
}
