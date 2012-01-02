package com.rackspace.papi.commons.util.http.media.servlet;

import java.util.List;
import com.rackspace.papi.commons.util.http.media.MediaType;
import com.rackspace.papi.commons.util.http.media.MimeType;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;

@RunWith(Enclosed.class)
public class RequestMediaRangeInterrogatorTest {
    public static class WhenInterrogatingRequests {
        
        @Test
        public void shouldReturnMediaTypeFromVariant() {
            List<MediaType> mediaRange = RequestMediaRangeInterrogator.interrogate("http://cloudservers/images.json", "");

            assertEquals(MimeType.APPLICATION_JSON, mediaRange.get(0).getMimeType());
        }

        @Test
        public void shouldReturnMediaTypeFromAcceptHeader() {
            List<MediaType> mediaRange = RequestMediaRangeInterrogator.interrogate("http://servers.api.openstack.org/images", "application/xml");

            assertEquals(MimeType.APPLICATION_XML, mediaRange.get(0).getMimeType());
        }
    }
}
