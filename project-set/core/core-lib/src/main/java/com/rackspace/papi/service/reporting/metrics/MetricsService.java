package com.rackspace.papi.service.reporting.metrics;

import com.rackspace.papi.commons.util.Destroyable;
import com.yammer.metrics.core.Counter;
import com.yammer.metrics.core.Meter;
import com.yammer.metrics.core.MetricsRegistry;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * Service to pass around service registry
 */
public interface MetricsService extends Destroyable{

   public Meter newMeter( Class klass, String name, String scope, String eventType, TimeUnit unit );

   public Counter newCounter( Class klass, String name, String scope );

   public void updateConfiguration( String host, int port, long period, String prefix ) throws IOException;

   public void shutdownGraphite();

   // TODO:  add additional Repose-specific metrics as required

   public void destroy();

}
