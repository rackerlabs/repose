package com.rackspace.papi.service.reporting.metrics.impl;

import com.rackspace.papi.service.reporting.metrics.MetricsService;
import com.rackspace.papi.spring.ReposeJmxNamingStrategy;
import com.yammer.metrics.core.*;
import com.yammer.metrics.reporting.JmxReporter;
import com.yammer.metrics.reporting.GraphiteReporter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 *
 * MetricServiceImpl
 *
 * This factory class generates Yammer Metrics objects & exposes them through JMX & Graphite.  Any metric classes which
 * might be used by multiple components should have a factory method off this class.
 *
 * To ensure no namespace collisions between clusters & nodes, the reposeJmxNamingStrategy object is used.  This object
 * also provides the ObjectName for any Spring-managed MBeans.
 *
 * Custom MXBeans
 * --------------
 * If you need to register a MXBean, please follow the example of the ConfigurationInformation class.  While its not an
 * MXBean (its an MBean) much of its construction is usable for MXBeans.  It contains the following aspects:
 *
 *   - Declares itself as a @ManagedResource.  Use the same objectName format.
 *   - Implements a MBean interface (MXBeans are named *MXBean).
 *   - Declares JMX viewable methods with @ManagedOperation.
 *
 * Additionally, this can also be helpful:
 *
 *   - http://docs.oracle.com/javase/tutorial/jmx/mbeans/mxbeans.html  In particular the part about the @ConstructorProperites
 *     annotations I believe prevents you from having to mess with CompositeData when returning custom objects through
 *     a MXBean.
 *
 **/
@Component("metricsService")
public class MetricsServiceImpl implements MetricsService {

    private MetricsRegistry metrics;
    private JmxReporter jmx;
    private GraphiteReporter graphite;
    private ReposeJmxNamingStrategy reposeStrat;

    @Autowired
    public MetricsServiceImpl( @Qualifier( "reposeJmxNamingStrategy" ) ReposeJmxNamingStrategy reposeStratP ){

        metrics = new MetricsRegistry();

        jmx = new JmxReporter( metrics );
        jmx.start();

        reposeStrat = reposeStratP;
    }

    public synchronized void updateConfiguration( String host, int port, long period, String prefix ) throws IOException {
       shutdownGraphite();


       graphite = new GraphiteReporter( metrics,
                prefix,
                MetricPredicate.ALL,
                new GraphiteReporter.DefaultSocketProvider(host, port),
                Clock.defaultClock() );

        graphite.start( period, TimeUnit.SECONDS );
    }

   public synchronized void shutdownGraphite() {

      if ( graphite != null ) {

         graphite.shutdown();
         graphite = null;
      }
   }

    @Override
    public Meter newMeter(Class klass, String name, String scope, String eventType, TimeUnit unit ) {

        return metrics.newMeter( makeMetricName( klass, name, scope ), eventType, unit );
    }

    @Override
    public Counter newCounter(Class klass, String name, String scope ) {

        return metrics.newCounter( makeMetricName( klass, name, scope ) );
    }

    @Override
   public void destroy() {

      metrics.shutdown();
      jmx.shutdown();

       shutdownGraphite();
    }

   private MetricName makeMetricName( Class klass, String name, String scope ) {

       return new MetricName( reposeStrat.getDomainPrefix() + klass.getPackage().getName(), klass.getSimpleName(), name, scope);
   }
}
