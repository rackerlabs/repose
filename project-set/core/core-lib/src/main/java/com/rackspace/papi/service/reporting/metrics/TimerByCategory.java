package com.rackspace.papi.service.reporting.metrics;

/**
 * Interface to allow different TimerByCategory implementations to be used interchangeably.
 * <p>
 * Any class which implements this interface is expected to be thread-safe.  The individual yammer Timer class is
 * thread-safe.
 * <p>
 * These objects should be created by the {@link com.rackspace.papi.service.reporting.metrics.impl.MetricsServiceImpl}
 * factory class.
 */
public interface TimerByCategory {

    //TODO

}
