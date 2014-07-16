package com.rackspace.papi.service.context.impl

import com.rackspace.papi.commons.config.manager.UpdateListener
import com.rackspace.papi.commons.config.resource.ConfigurationResource
import com.rackspace.papi.commons.config.resource.ConfigurationResourceResolver
import com.rackspace.papi.service.ServiceRegistry
import com.rackspace.papi.service.config.ConfigurationService
import com.rackspace.papi.service.context.ServiceContext
import com.rackspace.papi.service.healthcheck.HealthCheckService
import com.rackspace.papi.service.healthcheck.HealthCheckServiceProxy
import com.rackspace.papi.service.healthcheck.Severity
import com.rackspace.papi.service.httpclient.HttpClientService
import com.rackspace.papi.service.httpclient.config.HttpConnectionPoolConfig
import org.junit.Before
import org.junit.Test

import javax.servlet.ServletContextEvent

import static org.mockito.Matchers.any
import static org.mockito.Matchers.anyString
import static org.mockito.Mockito.*

class HttpConnectionPoolServiceContextTest {

    HttpConnectionPoolServiceContext httpConnectionPoolServiceContext
    HealthCheckService healthCheckService
    HealthCheckServiceProxy healthCheckServiceProxy
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
        healthCheckServiceProxy = mock(HealthCheckServiceProxy)
        when(healthCheckService.register()).thenReturn(healthCheckServiceProxy)
        httpConnectionPoolServiceContext = new HttpConnectionPoolServiceContext(serviceRegistry, configurationService, httpClientService, healthCheckService)
        sce = mock(ServletContextEvent.class)

    }

    @Test
    void verifyRegistrationToHealthCheckService() {

        verify(healthCheckService, times(1)).register()

    }

    @Test
    void verifyInitialReportOfConfig(){

        ConfigurationResourceResolver resourceResolver = mock(ConfigurationResourceResolver.class);
        ConfigurationResource configurationResource = mock(ConfigurationResource.class);
        when(configurationService.getResourceResolver()).thenReturn(resourceResolver);
        when(resourceResolver.resolve(HttpConnectionPoolServiceContext.DEFAULT_CONFIG_NAME)).thenReturn(configurationResource);
        when(configurationService.getResourceResolver().resolve(MetricsServiceContext.DEFAULT_CONFIG_NAME)).thenReturn(configurationResource);

        httpConnectionPoolServiceContext.contextInitialized(sce);
        verify(healthCheckServiceProxy, times(1)).reportIssue(any(String), any(String), any(Severity.class));
        verify(serviceRegistry, times(1)).addService(any(ServiceContext.class));
    }

    @Test
    void verifyContextDestroyed(){
        httpConnectionPoolServiceContext.contextDestroyed(sce)
        verify(httpClientService, times(1)).shutdown()
        verify(configurationService, times(1)).unsubscribeFrom(any(String),any(UpdateListener.class))

    }

    @Test
    void verifyHealthCheckServiceReportingNullPointer(){

        ConfigurationResourceResolver resourceResolver = mock(ConfigurationResourceResolver.class);
        ConfigurationResource configurationResource = mock(ConfigurationResource.class);
        when(configurationService.getResourceResolver()).thenReturn(resourceResolver);
        when(resourceResolver.resolve(HttpConnectionPoolServiceContext.DEFAULT_CONFIG_NAME)).thenReturn(configurationResource);
        when(configurationService.getResourceResolver().resolve(MetricsServiceContext.DEFAULT_CONFIG_NAME)).thenReturn(configurationResource);

        httpConnectionPoolServiceContext.contextInitialized(sce);
        verify(healthCheckServiceProxy, times(1)).reportIssue(any(String), any(String), any(Severity));
        verify(serviceRegistry, times(1)).addService(any(ServiceContext.class));

    }

    @Test
    void verifyConfigurationUpdated(){

        ConfigurationResourceResolver resourceResolver = mock(ConfigurationResourceResolver.class);
        ConfigurationResource configurationResource = mock(ConfigurationResource.class);
        when(configurationService.getResourceResolver()).thenReturn(resourceResolver);
        when(resourceResolver.resolve(HttpConnectionPoolServiceContext.DEFAULT_CONFIG_NAME)).thenReturn(configurationResource);
        when(configurationService.getResourceResolver().resolve(MetricsServiceContext.DEFAULT_CONFIG_NAME)).thenReturn(configurationResource);

        httpConnectionPoolServiceContext.configurationListener.configurationUpdated(mock(HttpConnectionPoolConfig))

        assert(httpConnectionPoolServiceContext.configurationListener.initialized)
        verify(httpClientService, times(1)).configure(any(HttpConnectionPoolConfig.class));
        verify(healthCheckServiceProxy, times(1)).resolveIssue(anyString());
    }
}
