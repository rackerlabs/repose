package com.rackspace.papi.commons.util.logging.apache.format.converters;

import static org.junit.Assert.*;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

@RunWith(Enclosed.class)
public class DateConversionFormatTest {

    public static final class WhenGettingPatterns {

        @Test
        public void shouldGetCorrectPattern() {
            String format = DateConversionFormat.getPattern(DateConversionFormat.ISO_8601.name());
            assertNotNull(format);
            assertEquals(DateConversionFormat.ISO_8601.getPattern(), format);
        }
        
        @Test
        public void shouldGetDefaultPattern() {
            String format = DateConversionFormat.getPattern("Doesn't Exist");
            assertNotNull(format);
            assertEquals(DateConversionFormat.RFC_1123.getPattern(), format);
        }
    }

}
