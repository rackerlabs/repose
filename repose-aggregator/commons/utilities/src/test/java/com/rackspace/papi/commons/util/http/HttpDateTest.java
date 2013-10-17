package com.rackspace.papi.commons.util.http;

import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import java.util.Calendar;
import java.util.TimeZone;

import static org.junit.Assert.assertEquals;

/**
 *
 * 
 */
@RunWith(Enclosed.class)
public class HttpDateTest {
    public static class WhenMarshallingToRFC1123 {
        @Test
        public void shouldOutputCorrectFormat() {
            final String expected = "Sun, 06 Nov 1994 08:49:37 GMT";

            final Calendar then = Calendar.getInstance();
            then.setTimeZone(TimeZone.getTimeZone("GMT"));
            then.set(1994, 10, 6, 8, 49, 37);

            assertEquals("Date format did not match expected", expected, new HttpDate(then.getTime()).toRFC1123());
        }
    }
}
