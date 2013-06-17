package com.rackspace.papi.service.reporting.metrics;

/**
 * Interface to allow different MeterByCategory implementations (like
 * {@link com.rackspace.papi.service.reporting.metrics.impl.MeterByCategoryImpl} &
 * {@link com.rackspace.papi.service.reporting.metrics.impl.MeterByCategorySum}) to be used interchangeably.
 * <p>
 * Any class which implements this interface is expected to be thread-safe.  The individual yammer Meter class are
 * thread-safe.
 * <p>
 * These objects should be created by the {@link com.rackspace.papi.service.reporting.metrics.impl.MetricsServiceImpl}
 * factory class.
 */
public interface MeterByCategory {

    public void mark( String key );

    public void mark( String key, long n );
}
