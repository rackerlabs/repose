package org.openrepose.services.ratelimit.cache.util;

import java.util.concurrent.TimeUnit;

/**
 * @author jhopper
 */
public final class TimeUnitConverter {
    private TimeUnitConverter() {}

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
