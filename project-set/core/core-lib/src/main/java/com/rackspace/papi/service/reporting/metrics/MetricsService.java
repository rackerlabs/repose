package com.rackspace.papi.service.reporting.metrics;

import com.rackspace.papi.commons.util.Destroyable;
import com.rackspace.papi.service.reporting.metrics.impl.MeterByCategorySum;
import com.yammer.metrics.core.Counter;
import com.yammer.metrics.core.Meter;
import com.yammer.metrics.core.Timer;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * Service to pass around service registry.
 * <p>
 * This service creates all necessary classes to track repose performance through JMX & Graphite.
 */
public interface MetricsService extends Destroyable{

    void setEnabled(boolean b);
    boolean isEnabled();
    Meter newMeter( Class klass, String name, String scope, String eventType, TimeUnit unit );
    MeterByCategory newMeterByCategory( Class klass, String scope, String eventType, TimeUnit unit );
    MeterByCategorySum newMeterByCategorySum( Class klass, String scope, String eventType, TimeUnit unit );
    Counter newCounter( Class klass, String name, String scope );
    Timer newTimer(Class klass, String name, String scope, TimeUnit duration, TimeUnit rate );
    TimerByCategory newTimerByCategory(Class klass, String scope, TimeUnit duration, TimeUnit rate );
    void addGraphiteServer( String host, int port, long period, String prefix ) throws IOException;
    void shutdownGraphite();
    void destroy();
}
