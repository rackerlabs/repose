package com.rackspace.papi.service.reporting.metrics.impl;

import com.rackspace.papi.commons.config.manager.UpdateListener;
import com.rackspace.papi.service.config.ConfigurationService;
import com.rackspace.papi.service.healthcheck.HealthCheckService;
import com.rackspace.papi.service.healthcheck.HealthCheckServiceProxy;
import com.rackspace.papi.service.healthcheck.Severity;
import com.rackspace.papi.service.reporting.metrics.MeterByCategory;
import com.rackspace.papi.service.reporting.metrics.MetricsService;
import com.rackspace.papi.service.reporting.metrics.TimerByCategory;
import com.rackspace.papi.service.reporting.metrics.config.GraphiteServer;
import com.rackspace.papi.service.reporting.metrics.config.MetricsConfiguration;
import com.rackspace.papi.spring.ReposeJmxNamingStrategy;
import com.yammer.metrics.core.*;
import com.yammer.metrics.reporting.GraphiteReporter;
import com.yammer.metrics.reporting.JmxReporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Named;
import java.io.IOException;
import java.net.URL;
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
 * repose/repose-aggregator/functional-tests/spock-functional-test/src/test/groovy/features/core/powerfilter/ResponseCodeJMXTest.groovy
 * provide an example on how you might verify your instrumentation.
 */
@Named
public class MetricsServiceImpl implements MetricsService {

    public static final String DEFAULT_CONFIG_NAME = "metrics.cfg.xml";
    private static final Logger LOG = LoggerFactory.getLogger(MetricsService.class);
    private static final String metricsServiceConfigReport = "MetricsServiceReport";
    private final ConfigurationService configurationService;
    private final MetricsCfgListener metricsCfgListener;
    private final HealthCheckService healthCheckService;
    private MetricsRegistry metrics;
    private JmxReporter jmx;
    private List<GraphiteReporter> listGraphite = new ArrayList<GraphiteReporter>();
    private ReposeJmxNamingStrategy reposeStrat;
    private boolean enabled;
    private HealthCheckServiceProxy healthCheckServiceProxy;

    @Inject
    public MetricsServiceImpl(ReposeJmxNamingStrategy reposeStratP, ConfigurationService configurationService,
                              HealthCheckService healthCheckService) {
        this.metrics = new MetricsRegistry();
        this.jmx = new JmxReporter( metrics );
        jmx.start();
        this.reposeStrat = reposeStratP;
        this.enabled = true;
        this.configurationService = configurationService;
        metricsCfgListener = new MetricsCfgListener();
        this.healthCheckService = healthCheckService;
    }

    @PostConstruct
    public void afterPropertiesSet() {
        healthCheckServiceProxy = healthCheckService.register();
        reportIssue();
        URL xsdURL = getClass().getResource("/META-INF/schema/metrics/metrics.xsd");
        configurationService.subscribeTo(DEFAULT_CONFIG_NAME, xsdURL, metricsCfgListener, MetricsConfiguration.class);

        // The Metrics config is optional so in the case where the configuration listener doesn't mark it iniitalized
        // and the file doesn't exist, this means that the Metrics service will load its own default configuration
        // and the initial health check error should be cleared.
        try {
            if (!metricsCfgListener.isInitialized() &&
                !configurationService.getResourceResolver().resolve("metrics.cfg.xml").exists()) {
                solveIssue();
            }
        } catch (IOException io) {
            LOG.error("Error attempting to search for " + DEFAULT_CONFIG_NAME);
        }
    }

    @PreDestroy
    public void destroy() {
        healthCheckServiceProxy.deregister();
        metrics.shutdown();
        jmx.shutdown();
        shutdownGraphite();
        configurationService.unsubscribeFrom(DEFAULT_CONFIG_NAME, metricsCfgListener);
    }

    private void reportIssue() {
        LOG.debug("Reporting issue to Health Checker Service: " + metricsServiceConfigReport);
        healthCheckServiceProxy.reportIssue(metricsServiceConfigReport, "Metrics Service Configuration Error", Severity.BROKEN);
    }

    private void solveIssue() {
            LOG.debug("Resolving issue: " + metricsServiceConfigReport);
            healthCheckServiceProxy.resolveIssue(metricsServiceConfigReport);
    }

    private class MetricsCfgListener implements UpdateListener<MetricsConfiguration> {

        private boolean initialized = false;

        @Override
        public void configurationUpdated(MetricsConfiguration metricsC) {

            // we are reinitializing the graphite servers
            shutdownGraphite();

            if (metricsC.getGraphite() != null) {

                try {

                    for (GraphiteServer gs : metricsC.getGraphite().getServer()) {

                        addGraphiteServer(gs.getHost(),
                                          gs.getPort().intValue(),
                                          gs.getPeriod(),
                                          gs.getPrefix());
                    }
                } catch (IOException e) {

                    LOG.debug("Error with the MetricsService", e);
                }
            }

            solveIssue();
            setEnabled(metricsC.isEnabled());
            initialized = true;
        }

        @Override
        public boolean isInitialized() {
            return initialized;
        }
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
    public void setEnabled(boolean b) {
        this.enabled = b;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
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
    public Timer newTimer( Class klass, String name, String scope, TimeUnit duration, TimeUnit rate ) {
        return metrics.newTimer( makeMetricName( klass, name, scope ), duration, rate );
    }

    @Override
    public TimerByCategory newTimerByCategory( Class klass, String scope, TimeUnit duration, TimeUnit rate ) {
        return new TimerByCategoryImpl( this, klass, scope, duration, rate );
    }

    @Override
    public MeterByCategory newMeterByCategory( Class klass, String scope, String eventType, TimeUnit unit ) {
        return new MeterByCategoryImpl( this, klass, scope, eventType, unit );
    }

    @Override
    public MeterByCategorySum newMeterByCategorySum( Class klass, String scope, String eventType, TimeUnit unit ) {
        return new MeterByCategorySum( this, klass, scope, eventType, unit );
    }

    private MetricName makeMetricName( Class klass, String name, String scope ) {
        return new MetricName( reposeStrat.getDomainPrefix() + klass.getPackage().getName(),
                               klass.getSimpleName(),
                               name, scope );
    }
}
