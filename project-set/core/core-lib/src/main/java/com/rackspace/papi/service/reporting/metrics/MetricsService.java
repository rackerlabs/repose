package com.rackspace.papi.service.reporting.metrics;

import com.rackspace.papi.commons.util.Destroyable;
import com.rackspace.papi.service.reporting.metrics.impl.MeterByCategorySum;
import com.yammer.metrics.core.Counter;
import com.yammer.metrics.core.Meter;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * Service to pass around service registry.
 * <p>
 * This service creates all necessary classes to track repose performance through JMX & Graphite.
 */
public interface MetricsService extends Destroyable{

    public Meter newMeter( Class klass, String name, String scope, String eventType, TimeUnit unit );

    public MeterByCategory newMeterByCategory( Class klass, String scope, String eventType, TimeUnit unit );

    public MeterByCategorySum newMeterByCategorySum( Class klass, String scope, String eventType, TimeUnit unit );

    public Counter newCounter( Class klass, String name, String scope );

    public void addGraphiteServer( String host, int port, long period, String prefix ) throws IOException;

    public void shutdownGraphite();

    public void destroy();

}
