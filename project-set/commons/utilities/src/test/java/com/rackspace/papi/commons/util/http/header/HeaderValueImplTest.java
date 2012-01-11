package com.rackspace.papi.commons.util.http.header;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;

/**
 *
 * @author zinic
 */
@RunWith(Enclosed.class)
public class HeaderValueImplTest {

    public static class WhenOutputtingHeaderValueAsString {

        @Test
        public void shouldOutputValueParameters() {
            final Pattern expectedPattern = Pattern.compile("[^;]+;(param1=1;?)?(param2=2;?)?(param3=3;?)?");

            final Map<String, String> parameters = new HashMap<String, String>();
            parameters.put("param1", "1");
            parameters.put("param2", "2");
            parameters.put("param3", "3");

            final HeaderValueImpl headerValue = new HeaderValueImpl("value", parameters);

            assertTrue("Header value: " + headerValue.toString() + " must match expected pattern", expectedPattern.matcher(headerValue.toString()).matches());
        }

        @Test
        public void shouldOutputValueWithNoParameters() {
            final HeaderValueImpl headerValue = new HeaderValueImpl("value", Collections.EMPTY_MAP);

            assertEquals("Header value should only contain value when no parameters are present.", "value", headerValue.toString());
        }
    }
}
