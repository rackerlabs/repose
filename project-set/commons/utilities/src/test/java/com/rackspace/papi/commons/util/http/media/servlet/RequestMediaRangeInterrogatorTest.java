package com.rackspace.papi.commons.util.http.media.servlet;

import com.rackspace.papi.commons.util.http.media.servlet.RequestMediaRangeInterrogator;
import com.rackspace.papi.commons.util.http.media.MediaRange;
import com.rackspace.papi.commons.util.http.media.MediaType;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;

/**
 * Created by IntelliJ IDEA.
 * User: joshualockwood
 * Date: Apr 18, 2011
 * Time: 3:24:38 PM
 */
@RunWith(Enclosed.class)
public class RequestMediaRangeInterrogatorTest {
    public static class WhenInterrogatingRequests {
        
        @Test
        public void shouldReturnMediaTypeFromVariant() {
            MediaRange mediaRange = RequestMediaRangeInterrogator.interrogate("http://cloudservers/images.json", "");

            assertEquals(MediaType.APPLICATION_JSON, mediaRange.getMediaType());            
        }

        @Test
        public void shouldReturnMediaTypeFromAcceptHeader() {
            MediaRange mediaRange = RequestMediaRangeInterrogator.interrogate("http://servers.api.openstack.org/images", "application/xml");

            assertEquals(MediaType.APPLICATION_XML, mediaRange.getMediaType());
        }
    }
}
