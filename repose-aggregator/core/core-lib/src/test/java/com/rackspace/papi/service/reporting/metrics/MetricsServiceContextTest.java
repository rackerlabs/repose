package com.rackspace.papi.service.reporting.metrics;

import com.rackspace.papi.service.ServiceRegistry;
import com.rackspace.papi.service.config.ConfigurationService;
import com.rackspace.papi.service.context.impl.MetricsServiceContext;
import com.rackspace.papi.service.healthcheck.HealthCheckReport;
import com.rackspace.papi.service.healthcheck.HealthCheckService;
import com.rackspace.papi.service.healthcheck.InputNullException;
import com.rackspace.papi.service.healthcheck.NotRegisteredException;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import javax.servlet.ServletContextEvent;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.*;

@RunWith(Enclosed.class)
public class MetricsServiceContextTest {
    public static class EnabledDisabled {
        protected MetricsServiceContext metricsServiceContext;

        protected ServiceRegistry serviceRegistry;
        protected ConfigurationService configurationService;
        protected MetricsService metricsService;
        protected HealthCheckService healthCheckService;
        protected ServletContextEvent sce;


        @Before
        public void setUp() throws InputNullException {
            serviceRegistry = mock(ServiceRegistry.class);
            configurationService = mock(ConfigurationService.class);
            metricsService = mock(MetricsService.class);
            healthCheckService = mock(HealthCheckService.class);
            when(healthCheckService.register(MetricsServiceContext.class)).thenReturn("UID");
            metricsServiceContext = new MetricsServiceContext(serviceRegistry, configurationService, metricsService, healthCheckService);
            sce = mock(ServletContextEvent.class);
        }

        @Test
        public void testMetricsServiceEnabled() {
            when(metricsService.isEnabled()).thenReturn(true);

            assertNotNull(metricsServiceContext.getService());
        }

        @Test
        public void testMetricsServiceDisabled() {
            when(metricsService.isEnabled()).thenReturn(false);

            assertNull(metricsServiceContext.getService());
        }

        @Test
        public void verifyRegisteredToHealthCheckService() throws InputNullException {

            verify(healthCheckService, times(1)).register(MetricsServiceContext.class);
        }

        @Test
        public void verifyIssueReported() throws InputNullException, NotRegisteredException {

            metricsServiceContext.contextInitialized(sce);
            verify(healthCheckService, times(1)).reportIssue(any(String.class), any(String.class), any(HealthCheckReport.class));
        }
    }
}
