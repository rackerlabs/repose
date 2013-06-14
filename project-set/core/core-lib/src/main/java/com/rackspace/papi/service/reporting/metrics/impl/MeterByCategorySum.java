package com.rackspace.papi.service.reporting.metrics.impl;

import com.rackspace.papi.service.reporting.metrics.MetricsService;
import com.yammer.metrics.core.Meter;

import java.util.concurrent.TimeUnit;

/**
 * Implements a collection of Meters which share the same JMX type & scope.  These Meters are usually related in some
 * fashion.
 * <p>
 * By calling the mark() methods, a Meter object is automatically registered under the key as name and can be marked
 * by later calls.
 * <p>
 * Additionally, an additional Meter registered under the name ACROSS ALL tracks the summary of all Meters in this
 * object.
 * <p>
 * This is created by the {@link com.rackspace.papi.service.reporting.metrics.impl.MetricsServiceImpl} factory class.
 * <p>
 * This class is thread-safe.
 */
public class MeterByCategorySum extends MeterByCategoryImpl {

    public static String ALL = "ACROSS ALL";

    private Meter meter;

    MeterByCategorySum( MetricsService metricsServiceP, Class klassP, String scopeP, String eventTypeP,
                        TimeUnit unitP ) {
        super( metricsServiceP, klassP, scopeP, eventTypeP, unitP );

        meter = metricsServiceP.newMeter( klassP, ALL, scopeP, eventTypeP, unitP );
    }

    @Override
    public void mark( String key ) {

        checkKey( key );
        super.mark( key );
        meter.mark();
    }

    @Override
    public void mark( String key, long n ) {

        checkKey( key );
        super.mark( key, n );
        meter.mark( n );
    }

    private void checkKey( String key ) {
        if ( key.equals( ALL ) ) {

            throw new IllegalArgumentException(
                  getClass().getName() + ": The key value '" + key + "' is a reserved key to track stats across all Meters." );
        }
    }
}