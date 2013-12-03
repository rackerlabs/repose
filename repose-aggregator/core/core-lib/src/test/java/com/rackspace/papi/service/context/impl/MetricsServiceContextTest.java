package com.rackspace.papi.service.context.impl;

import com.rackspace.papi.service.ServiceRegistry;
import com.rackspace.papi.service.config.ConfigurationService;
import com.rackspace.papi.service.metrics.MetricsService;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.mockito.Mockito;

@RunWith(Enclosed.class)
public class MetricsServiceContextTest {
    public static class EnabledDisabled {
        protected MetricsServiceContext metricsServiceContext;

        protected ServiceRegistry serviceRegistry;
        protected ConfigurationService configurationService;
        protected MetricsService metricsService;

        @Before
        public void setUp() {
            serviceRegistry = Mockito.mock(ServiceRegistry.class);
            configurationService = Mockito.mock(ConfigurationService.class);
            metricsService = Mockito.mock(MetricsService.class);
            metricsServiceContext = new MetricsServiceContext(serviceRegistry, configurationService, metricsService);
        }

        @Test
        public void testMetricsServiceEnabled() {
            Mockito.when(metricsService.isEnabled()).thenReturn(true);

            Assert.assertNotNull(metricsServiceContext.getService());
        }

        @Test
        public void testMetricsServiceDisabled() {
            Mockito.when(metricsService.isEnabled()).thenReturn(false);

            Assert.assertNull(metricsServiceContext.getService());
        }
    }
}
