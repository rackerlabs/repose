package com.rackspace.repose.service.ratelimit.cache.util;

import java.util.concurrent.TimeUnit;

/**
 *
 * @author jhopper
 */
public final class TimeUnitConverter {
   private static final int DAY_UNIT = 24;
   private static final int HOUR_UNIT = 60;
   private static final int MINUTE_UNIT = 60;
   private static final int SECOND_UNIT = 1000;

    private TimeUnitConverter() {
    }
    
    
    public static long toLong(com.rackspace.repose.service.limits.schema.TimeUnit timeUnit) {
        long time = 1;

        switch (timeUnit) {
            case DAY:
                time *= DAY_UNIT;
            case HOUR:
                time *= HOUR_UNIT;
            case MINUTE:
                time *= MINUTE_UNIT;
            case SECOND:
                time *= SECOND_UNIT;
        }

        return time;
    }

    public static TimeUnit fromSchemaTypeToConcurrent(com.rackspace.repose.service.limits.schema.TimeUnit unit) {
        switch (unit) {
            case SECOND:
                return TimeUnit.SECONDS;
            case MINUTE:
                return TimeUnit.MINUTES;
            case HOUR:
                return TimeUnit.HOURS;
            case DAY:
                return TimeUnit.DAYS;
            default:
                throw new IllegalArgumentException("Time unit: " + unit.toString() + " is not supported");
        }
    }
}
