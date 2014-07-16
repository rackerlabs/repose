package com.rackspace.papi.service.reporting.metrics;

import com.rackspace.papi.commons.config.resource.ConfigurationResource;
import com.rackspace.papi.commons.config.resource.ConfigurationResourceResolver;
import com.rackspace.papi.service.ServiceRegistry;
import com.rackspace.papi.service.config.ConfigurationService;
import com.rackspace.papi.service.context.impl.MetricsServiceContext;
import com.rackspace.papi.service.healthcheck.HealthCheckService;
import com.rackspace.papi.service.healthcheck.HealthCheckServiceProxy;
import com.rackspace.papi.service.healthcheck.Severity;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import javax.servlet.ServletContextEvent;
import java.io.IOException;

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
        protected HealthCheckServiceProxy healthCheckServiceProxy;
        protected ServletContextEvent sce;


        @Before
        public void setUp() {
            serviceRegistry = mock(ServiceRegistry.class);
            configurationService = mock(ConfigurationService.class);
            metricsService = mock(MetricsService.class);
            healthCheckService = mock(HealthCheckService.class);
            healthCheckServiceProxy = mock(HealthCheckServiceProxy.class);
            when(healthCheckService.register()).thenReturn(healthCheckServiceProxy);
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
        public void verifyRegisteredToHealthCheckService() {

            verify(healthCheckService, times(1)).register();
        }

        @Test
        public void verifyIssueReported() throws IOException {

            ConfigurationResourceResolver resourceResolver = mock(ConfigurationResourceResolver.class);
            ConfigurationResource configurationResource = mock(ConfigurationResource.class);
            when(configurationService.getResourceResolver()).thenReturn(resourceResolver);
            when(resourceResolver.resolve(MetricsServiceContext.DEFAULT_CONFIG_NAME)).thenReturn(configurationResource);
            when(configurationService.getResourceResolver().resolve(MetricsServiceContext.DEFAULT_CONFIG_NAME)).thenReturn(configurationResource);

            when(configurationResource.exists()).thenReturn(false);
            metricsServiceContext.contextInitialized(sce);
            verify(healthCheckServiceProxy, times(1)).reportIssue(any(String.class), any(String.class), any(Severity.class));
        }
    }
}
