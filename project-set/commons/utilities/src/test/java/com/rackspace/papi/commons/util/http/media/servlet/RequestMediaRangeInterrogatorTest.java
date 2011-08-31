package com.rackspace.papi.commons.util.http.media.servlet;

import java.util.List;
import com.rackspace.papi.commons.util.http.media.MediaRange;
import com.rackspace.papi.commons.util.http.media.MediaType;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;

@RunWith(Enclosed.class)
public class RequestMediaRangeInterrogatorTest {
    public static class WhenInterrogatingRequests {
        
        @Test
        public void shouldReturnMediaTypeFromVariant() {
            List<MediaRange> mediaRange = RequestMediaRangeInterrogator.interrogate("http://cloudservers/images.json", "");

            assertEquals(MediaType.APPLICATION_JSON, mediaRange.get(0).getMediaType());
        }

        @Test
        public void shouldReturnMediaTypeFromAcceptHeader() {
            List<MediaRange> mediaRange = RequestMediaRangeInterrogator.interrogate("http://servers.api.openstack.org/images", "application/xml");

            assertEquals(MediaType.APPLICATION_XML, mediaRange.get(0).getMediaType());
        }
    }
}
