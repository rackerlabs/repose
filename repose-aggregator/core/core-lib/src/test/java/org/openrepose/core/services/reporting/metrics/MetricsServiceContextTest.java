package org.openrepose.core.services.reporting.metrics;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.openrepose.commons.config.resource.ConfigurationResource;
import org.openrepose.commons.config.resource.ConfigurationResourceResolver;
import org.openrepose.core.services.ServiceRegistry;
import org.openrepose.core.services.config.ConfigurationService;
import org.openrepose.core.services.reporting.metrics.impl.MetricsServiceImpl;
import org.openrepose.core.spring.ReposeJmxNamingStrategy;
import org.openrepose.services.healthcheck.HealthCheckService;
import org.openrepose.services.healthcheck.HealthCheckServiceProxy;
import org.openrepose.services.healthcheck.Severity;

import javax.servlet.ServletContextEvent;
import java.io.IOException;

import static org.mockito.Mockito.*;

/**
 * This is testing a few things that used to only exist int he "service context" thing that no longer exists
 * TODO: This should probably be refactored into the MetricsService Impl test.
 */
@RunWith(Enclosed.class)
public class MetricsServiceContextTest {
    public static class EnabledDisabled {
        protected MetricsServiceImpl metricsService;

        protected ServiceRegistry serviceRegistry;
        protected ConfigurationService configurationService;
        protected HealthCheckService healthCheckService;
        protected HealthCheckServiceProxy healthCheckServiceProxy;
        protected ServletContextEvent sce;


        @Before
        public void setUp() {
            serviceRegistry = mock(ServiceRegistry.class);
            configurationService = mock(ConfigurationService.class);
            healthCheckService = mock(HealthCheckService.class);
            healthCheckServiceProxy = mock(HealthCheckServiceProxy.class);
            when(healthCheckService.register()).thenReturn(healthCheckServiceProxy);
            metricsService = new MetricsServiceImpl(configurationService, healthCheckService, mock(ReposeJmxNamingStrategy.class));
            sce = mock(ServletContextEvent.class);
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
            when(resourceResolver.resolve(MetricsServiceImpl.DEFAULT_CONFIG_NAME)).thenReturn(configurationResource);
            when(configurationService.getResourceResolver().resolve(MetricsServiceImpl.DEFAULT_CONFIG_NAME)).thenReturn(configurationResource);

            when(configurationResource.exists()).thenReturn(false);
            metricsService.init();
            verify(healthCheckServiceProxy, times(1)).reportIssue(any(String.class), any(String.class), any(Severity.class));
        }
    }
}
