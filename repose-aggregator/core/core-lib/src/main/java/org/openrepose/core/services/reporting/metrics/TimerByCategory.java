package org.openrepose.core.services.reporting.metrics;

import com.yammer.metrics.core.TimerContext;

import java.util.concurrent.TimeUnit;

/**
 * Interface to allow different TimerByCategory implementations to be used interchangeably.
 * <p>
 * Any class which implements this interface is expected to be thread-safe.  The individual yammer Timer class is
 * thread-safe.
 * <p>
 * These objects should be created by the {@link org.openrepose.core.services.reporting.metrics.impl.MetricsServiceImpl}
 * factory class.
 */
public interface TimerByCategory {

    void update(String key, long duration, TimeUnit unit);
    TimerContext time(String key);
}
