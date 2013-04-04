package com.rackspace.papi.service.reporting.metrics;

import com.rackspace.papi.commons.util.Destroyable;
import com.yammer.metrics.core.MetricsRegistry;
/**
 * Service to pass around service registry
 */
public interface MetricsRegistryService extends Destroyable{
   
   MetricsRegistry getRegistry();
}
