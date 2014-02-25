package com.rackspace.papi.service.context.impl

import com.rackspace.papi.commons.config.resource.ConfigurationResource
import com.rackspace.papi.commons.config.resource.ConfigurationResourceResolver
import com.rackspace.papi.service.ServiceRegistry
import com.rackspace.papi.service.config.ConfigurationService
import com.rackspace.papi.service.healthcheck.HealthCheckReport
import com.rackspace.papi.service.healthcheck.HealthCheckService
import com.rackspace.papi.service.httpclient.HttpClientService
import org.junit.Before
import org.junit.Test

import javax.servlet.ServletContextEvent

import static org.mockito.Matchers.any
import static org.mockito.Mockito.*

class HttpConnectionPoolServiceContextTest {

    HttpConnectionPoolServiceContext httpConnectionPoolServiceContext
    HealthCheckService healthCheckService
    ServiceRegistry serviceRegistry;
    ConfigurationService configurationService;
    HttpClientService httpClientService;
    ServletContextEvent sce


    @Before
    void setUp() {

        configurationService = mock(ConfigurationService.class)
        serviceRegistry = mock(ServiceRegistry.class)
        httpClientService = mock(HttpClientService.class)
        healthCheckService = mock(HealthCheckService.class)
        when(healthCheckService.register(any(HttpConnectionPoolServiceContext.class))).thenReturn("UID")
        httpConnectionPoolServiceContext = new HttpConnectionPoolServiceContext(serviceRegistry, configurationService, httpClientService, healthCheckService)
        sce = mock(ServletContextEvent.class)

    }

    @Test
    void verifyRegistrationToHealthCheckService() {

        verify(healthCheckService, times(1)).register(any(HttpConnectionPoolServiceContext.class))

    }

    @Test
    void verifyInitialReportOfConfig(){

        ConfigurationResourceResolver resourceResolver = mock(ConfigurationResourceResolver.class);
        ConfigurationResource configurationResource = mock(ConfigurationResource.class);
        when(configurationService.getResourceResolver()).thenReturn(resourceResolver);
        when(resourceResolver.resolve(HttpConnectionPoolServiceContext.DEFAULT_CONFIG_NAME)).thenReturn(configurationResource);
        when(configurationService.getResourceResolver().resolve(MetricsServiceContext.DEFAULT_CONFIG_NAME)).thenReturn(configurationResource);

        httpConnectionPoolServiceContext.contextInitialized(sce)
        verify(healthCheckService, times(1)).reportIssue(any(String), any(String), any(HealthCheckReport.class))
    }
}
