package com.rackspace.papi.commons.util.logging.apache;

import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;

/**
 * Created by IntelliJ IDEA.
 * User: joshualockwood
 * Date: 7/25/11
 * Time: 2:56 PM
 */
@RunWith(Enclosed.class)
public class LogFormatArgumentTest {
    public static class WhenCallingToString {
        @Test
        public void shouldShowArgumentSymbol() {
            assertEquals("%", LogFormatArgument.PERCENT.toString());
        }
    }

    public static class WhenCallingFromString {
        @Test
        public void should() {
            assertEquals(LogFormatArgument.PERCENT, LogFormatArgument.fromString("%"));
        }
    }
}
