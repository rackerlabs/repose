package com.rackspace.papi.service.context.impl;

import com.rackspace.papi.service.ServiceRegistry;
import com.rackspace.papi.service.config.ConfigurationService;
import com.rackspace.papi.service.metrics.MetricsService;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(Enclosed.class)
public class MetricsServiceContextTest {
    public static class EnabledDisabled {
        protected MetricsServiceContext metricsServiceContext;

        protected ServiceRegistry serviceRegistry;
        protected ConfigurationService configurationService;
        protected MetricsService metricsService;

        @Before
        public void setUp() {
            serviceRegistry = mock(ServiceRegistry.class);
            configurationService = mock(ConfigurationService.class);
            metricsService = mock(MetricsService.class);
            metricsServiceContext = new MetricsServiceContext(serviceRegistry, configurationService, metricsService);
        }

        @Test
        public void testMetricsServiceEnabled() {
            when(metricsService.isEnabled()).thenReturn(true);

            Assert.assertNotNull(metricsServiceContext.getService());
        }

        @Test
        public void testMetricsServiceDisabled() {
            when(metricsService.isEnabled()).thenReturn(false);

            Assert.assertNull(metricsServiceContext.getService());
        }
    }
}
