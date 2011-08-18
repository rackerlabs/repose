package com.rackspace.papi.components.ratelimit.util;

import java.util.concurrent.TimeUnit;

/**
 *
 * @author jhopper
 */
public final class TimeUnitConverter {

    private TimeUnitConverter() {
    }
    
    
    public static long toLong(com.rackspace.papi.components.limits.schema.TimeUnit timeUnit) {
        long time = 1;

        switch (timeUnit) {
            case DAY:
                time *= 24;
            case HOUR:
                time *= 60;
            case MINUTE:
                time *= 60;
            case SECOND:
                time *= 1000;
        }

        return time;
    }

    public static TimeUnit fromSchemaTypeToConcurrent(com.rackspace.papi.components.limits.schema.TimeUnit unit) {
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
