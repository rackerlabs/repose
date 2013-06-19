package com.rackspace.papi.service.context.impl;

import com.rackspace.papi.commons.config.manager.UpdateListener;
import com.rackspace.papi.service.ServiceRegistry;
import com.rackspace.papi.service.config.ConfigurationService;
import com.rackspace.papi.service.context.ServiceContext;
import com.rackspace.papi.service.reporting.metrics.MetricsService;
import com.rackspace.papi.service.reporting.metrics.config.GraphiteServer;
import com.rackspace.papi.service.reporting.metrics.config.MetricsConfiguration;

import javax.servlet.ServletContextEvent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URL;

/**
 * Manages the {@link com.rackspace.papi.service.reporting.metrics.MetricsService} instance and subscribes to the
 * metrics.cfg.xml configuration file.
 */
@Component("metricsServiceContext")
public class MetricsServiceContext implements ServiceContext<MetricsService> {

   public static final String SERVICE_NAME = "MetricsService";

   private static final Logger LOG = LoggerFactory.getLogger( MetricsService.class );
   private static final String prefix = "Error with the MetricsService";

   private final MetricsService metricsService;
   private final ServiceRegistry registry;
   private final ConfigurationService configurationService;
   private final MetricsCfgListener metricsCfgListener;

   @Autowired
   public MetricsServiceContext( @Qualifier("serviceRegistry") ServiceRegistry registry,
                                 @Qualifier("configurationManager") ConfigurationService configurationService,
                                 @Qualifier("metricsService") MetricsService metricsService ) {

      this.registry = registry;
      this.configurationService = configurationService;
      this.metricsService = metricsService;
      metricsCfgListener = new MetricsCfgListener();
   }

   private void register() {
      if (registry != null) {
         registry.addService(this);
      }
   }

   @Override
   public String getServiceName() {
      return SERVICE_NAME;
   }

   @Override
   public MetricsService getService() {
      return metricsService;
   }

   @Override
   public void contextInitialized(ServletContextEvent sce) {

      URL xsdURL = getClass().getResource("/META-INF/schema/metrics/metrics.xsd");
      configurationService.subscribeTo("metrics.cfg.xml", xsdURL, metricsCfgListener, MetricsConfiguration.class);

      register();
   }

   @Override
   public void contextDestroyed(ServletContextEvent sce) {

      metricsService.destroy();
      configurationService.unsubscribeFrom( "metrics.cfg.xml", metricsCfgListener);
   }

   private class MetricsCfgListener implements UpdateListener<MetricsConfiguration> {

      private boolean initialized = false;

      @Override
      public void configurationUpdated( MetricsConfiguration metricsC ) {

          // we are reinitializing the graphite servers
          metricsService.shutdownGraphite();

          if ( metricsC.getGraphite() != null ) {

            try {

               for( GraphiteServer gs : metricsC.getGraphite().getServer() )  {

                   metricsService.addGraphiteServer( gs.getHost(),
                                                     gs.getPort().intValue(),
                                                     gs.getPeriod(),
                                                     gs.getPrefix() );
               }
            } catch (IOException e ) {

               LOG.debug( prefix, e );
            }
         }

         initialized = true;
      }

      @Override
      public boolean isInitialized() {
         return initialized;
      }
   }
}
