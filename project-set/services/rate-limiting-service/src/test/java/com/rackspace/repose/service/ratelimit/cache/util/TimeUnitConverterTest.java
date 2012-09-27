package com.rackspace.repose.service.ratelimit.cache.util;


import com.rackspace.repose.service.limits.schema.TimeUnit;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;

/**
 *
 * @author jhopper
 */
@RunWith(Enclosed.class)
public class TimeUnitConverterTest {
    public static class WhenConvertingTimeUnitsToLongRepresentations {

        @Test
        public void shouldConvertSeconds() {
            assertEquals(1000, TimeUnitConverter.toLong(TimeUnit.SECOND));
        }

        @Test
        public void shouldConvertMinutes() {
            assertEquals(60000, TimeUnitConverter.toLong(TimeUnit.MINUTE));
        }

        @Test
        public void shouldConvertHours() {
            assertEquals(3600000, TimeUnitConverter.toLong(TimeUnit.HOUR));
        }

        @Test
        public void shouldConvertDays() {
            assertEquals(86400000, TimeUnitConverter.toLong(TimeUnit.DAY));
        }
    }
}
