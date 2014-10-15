package org.openrepose.core.services.reporting.metrics;

/**
 * Interface to allow different MeterByCategory implementations (like
 * {@link org.openrepose.core.services.reporting.metrics.impl.MeterByCategoryImpl} &
 * {@link org.openrepose.core.services.reporting.metrics.impl.MeterByCategorySum}) to be used interchangeably.
 * <p>
 * Any class which implements this interface is expected to be thread-safe.  The individual yammer Meter class are
 * thread-safe.
 * <p>
 * These objects should be created by the {@link org.openrepose.core.services.reporting.metrics.impl.MetricsServiceImpl}
 * factory class.
 */
public interface MeterByCategory {

    void mark( String key );
    void mark( String key, long n );
}
