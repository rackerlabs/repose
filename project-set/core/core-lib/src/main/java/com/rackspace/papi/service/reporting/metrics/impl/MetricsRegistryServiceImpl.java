package com.rackspace.papi.service.reporting.metrics.impl;

import com.rackspace.papi.service.reporting.metrics.MetricsRegistryService;
import com.yammer.metrics.core.MetricsRegistry;
import org.springframework.stereotype.Component;

@Component("metricsRegistry")
public class MetricsRegistryServiceImpl implements MetricsRegistryService{
   
   
   private final MetricsRegistry metrics;
   
   
   public MetricsRegistryServiceImpl(){
      metrics = new MetricsRegistry();
   }
   
   @Override
   public MetricsRegistry getRegistry() {
      return metrics;
   }

   @Override
   public void destroy() {
      metrics.shutdown();
   }
   
}
