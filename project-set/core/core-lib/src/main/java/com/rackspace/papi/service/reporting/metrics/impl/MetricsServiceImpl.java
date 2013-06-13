package com.rackspace.papi.service.reporting.metrics.impl;

import com.rackspace.papi.service.reporting.metrics.MeterByCategory;
import com.rackspace.papi.service.reporting.metrics.MetricsService;
import com.rackspace.papi.spring.ReposeJmxNamingStrategy;
import com.yammer.metrics.core.*;
import com.yammer.metrics.reporting.JmxReporter;
import com.yammer.metrics.reporting.GraphiteReporter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * This factory class generates Yammer Metrics objects & exposes them through JMX & Graphite.  Any metric classes which
 * might be used by multiple components should have a factory method off this class.
 * <p>
 * To ensure no namespace collisions between clusters & nodes, the {@link com.rackspace.papi.spring.ReposeJmxNamingStrategy} object is used.  This object
 * also provides the ObjectName for any Spring-managed MBeans.
 * <p>
 * <h1>Custom MXBeans </h1>
 *
 * If you need to register a MXBean, please follow the example of the ConfigurationInformation class.  While its not an
 * MXBean (its an MBean) much of its construction is usable for MXBeans.  It contains the following aspects:
 * <ul>
 *   <li> Declares itself as a @ManagedResource.  Use the same objectName format.
 *   <li> Implements a MBean interface (MXBeans are named *MXBean).
 *   <li> Declares JMX viewable methods with @ManagedOperation.
 * </ul>
 * Additionally, this can also be helpful:
 * <ul>
 *  <li><a href="http://docs.oracle.com/javase/tutorial/jmx/mbeans/mxbeans.html">http://docs.oracle.com/javase/tutorial/jmx/mbeans/mxbeans.html</a>  In particular the part about the @ConstructorProperites
 * annotations I believe prevents you from having to mess with CompositeData when returning custom objects through
 * a MXBean.
 * </ul>
 * <p>
 * <h1>Functional Tests for instrumented filters</h1>
 * The functional tests contained in
 * repose/test/spock-functional-test/src/test/groovy/features/core/powerfilter/ResponseCodeJMXTest.groovy
 * provide an example on how you might verify your instrumentation.
 */
@Component( "metricsService" )
public class MetricsServiceImpl implements MetricsService {

    private MetricsRegistry metrics;
    private JmxReporter jmx;
    private List<GraphiteReporter> listGraphite = new ArrayList<GraphiteReporter>();
    private ReposeJmxNamingStrategy reposeStrat;

    @Autowired
    public MetricsServiceImpl( @Qualifier( "reposeJmxNamingStrategy" ) ReposeJmxNamingStrategy reposeStratP ) {

        metrics = new MetricsRegistry();

        jmx = new JmxReporter( metrics );
        jmx.start();

        reposeStrat = reposeStratP;
    }

    public void addGraphiteServer( String host, int port, long period, String prefix )
          throws IOException {

        GraphiteReporter graphite = new GraphiteReporter( metrics,
                                                          prefix,
                                                          MetricPredicate.ALL,
                                                          new GraphiteReporter.DefaultSocketProvider( host, port ),
                                                          Clock.defaultClock() );

        graphite.start( period, TimeUnit.SECONDS );

        synchronized ( listGraphite ) {
            listGraphite.add( graphite );
        }
    }

    public void shutdownGraphite() {

        synchronized ( listGraphite ) {
            for( GraphiteReporter graphite : listGraphite ) {

                graphite.shutdown();
            }
        }
    }

    @Override
    public Meter newMeter( Class klass, String name, String scope, String eventType, TimeUnit unit ) {

        return metrics.newMeter( makeMetricName( klass, name, scope ), eventType, unit );
    }

    @Override
    public Counter newCounter( Class klass, String name, String scope ) {

        return metrics.newCounter( makeMetricName( klass, name, scope ) );
    }


    @Override
    public MeterByCategory newMeterByCategory( Class klass, String scope, String eventType, TimeUnit unit ) {

        return new MeterByCategoryImpl( this, klass, scope, eventType, unit );
    }

    @Override
    public MeterByCategorySum newMeterByCategorySum( Class klass, String scope, String eventType, TimeUnit unit ) {

        return new MeterByCategorySum( this, klass, scope, eventType, unit );
    }

    @Override
    public void destroy() {

        metrics.shutdown();
        jmx.shutdown();

        shutdownGraphite();
    }

    private MetricName makeMetricName( Class klass, String name, String scope ) {

        return new MetricName( reposeStrat.getDomainPrefix() + klass.getPackage().getName(),
                               klass.getSimpleName(),
                               name, scope );
    }
}
