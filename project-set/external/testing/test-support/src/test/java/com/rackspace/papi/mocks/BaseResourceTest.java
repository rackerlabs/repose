package com.rackspace.papi.mocks;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;

/**
 *
 * @author zinic
 */
@RunWith(Enclosed.class)
public class BaseResourceTest {

    public static class WhenGettingCalendars {

        private BaseResource resource;
        
        @Before
        public void standUp() throws Exception {
            resource = new BaseResource();
        }

        @Test
        public void shouldReturnValidXmlGregorianCalendar() {
            assertNotNull("Base resource must return valid XML Gregorian Calendar instances.", resource.getCalendar());
        }
    }
}
