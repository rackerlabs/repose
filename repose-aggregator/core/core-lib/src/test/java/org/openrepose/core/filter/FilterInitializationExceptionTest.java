package org.openrepose.core.filter;

import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.openrepose.powerfilter.FilterInitializationException;

import static org.junit.Assert.assertEquals;

/**
 * @author fran
 */
@RunWith(Enclosed.class)
public class FilterInitializationExceptionTest {
    public static class WhenUsingFilterInitializationException {
        @Test
        public void shouldProcessCustomMessage() {
            String expectedExceptionMessage = "Oops!  Something unexpected happened.";
            FilterInitializationException filterInitializationException = new FilterInitializationException(expectedExceptionMessage, new Throwable("unexpected"));

            assertEquals(expectedExceptionMessage, filterInitializationException.getMessage());
        }
    }
}
