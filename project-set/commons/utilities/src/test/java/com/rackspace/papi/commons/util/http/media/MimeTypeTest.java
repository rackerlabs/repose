package com.rackspace.papi.commons.util.http.media;

import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;

/**
 * @author fran
 */
@RunWith(Enclosed.class)
public class MimeTypeTest {
    public static class WhenUsingGettingMediaTypeFromMediaTypeString {
        @Test
        public void shouldReturnUnknownMediaType() {
            String mediaTypeString = "application/what'sUpDoc";

            MimeType returnedMediaType = MimeType.getMatchingMimeType(mediaTypeString);

            assertEquals(MimeType.UNKNOWN, returnedMediaType);
        }        
    }
}
