package com.rackspace.papi.commons.util.http.media;

import com.rackspace.papi.commons.util.http.media.MediaType;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;

/**
 * @author fran
 */
@RunWith(Enclosed.class)
public class MediaTypeTest {
    public static class WhenUsingGettingMediaTypeFromMediaTypeString {
        @Test
        public void shouldReturnUnknownMediaType() {
            String mediaTypeString = "application/what'sUpDoc";

            MediaType returnedMediaType = MediaType.fromMediaTypeString(mediaTypeString);

            assertEquals(MediaType.UNKNOWN, returnedMediaType);
        }        
    }
}
