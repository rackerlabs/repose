package com.rackspace.papi.commons.util.http;

import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;

/**
 * Created by IntelliJ IDEA.
 * User: joshualockwood
 * Date: Apr 21, 2011
 * Time: 11:57:39 AM
 */
@RunWith(Enclosed.class)
public class HttpStatusCodeTest {
    public static class WhenGettingCodeIntValues {
        @Test
        public void shouldReturnExpected() {
            Integer expected, actual;

            expected = 200;
            actual = HttpStatusCode.OK.intValue();

            assertEquals(expected, actual);
        }
    }

    public static class WhenGettingCodeValuesFromInts {
        @Test
        public void shouldReturnExpected() {
            HttpStatusCode expected, actual;

            expected = HttpStatusCode.OK;
            actual = HttpStatusCode.fromInt(200);

            assertEquals(expected, actual);
        }

        @Test
        public void shouldGracefullyHandleUnknownValues() {
            assertEquals(HttpStatusCode.UNSUPPORTED_RESPONSE_CODE,
                    HttpStatusCode.fromInt(10101));
        }
    }
}
