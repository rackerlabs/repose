package com.rackspace.papi.service.datastore.distributed.impl.distributed.cluster

import com.rackspace.papi.commons.config.resource.ConfigurationResource
import com.rackspace.papi.commons.config.resource.ConfigurationResourceResolver
import com.rackspace.papi.domain.ReposeInstanceInfo
import com.rackspace.papi.service.ServiceRegistry
import com.rackspace.papi.service.config.ConfigurationService
import com.rackspace.papi.service.context.impl.MetricsServiceContext
import com.rackspace.papi.service.healthcheck.HealthCheckService
import com.rackspace.papi.service.healthcheck.HealthCheckServiceProxy
import com.rackspace.papi.service.healthcheck.Severity
import org.junit.Before
import org.junit.Test

import javax.servlet.ServletContext
import javax.servlet.ServletContextEvent

import static org.mockito.Matchers.any
import static org.mockito.Matchers.eq
import static org.mockito.Mockito.*

class DistributedDatastoreServiceClusterContextTest {

    DistributedDatastoreServiceClusterContext distributedDatastoreServiceClusterContext;
    HealthCheckService healthCheckService;
    HealthCheckServiceProxy healthCheckServiceProxy
    ConfigurationService configurationService;
    DistributedDatastoreServiceClusterViewService datastoreServiceClusterViewService;
    ReposeInstanceInfo reposeInstanceInfo;
    ServiceRegistry serviceRegistry;
    ServletContextEvent sce;


    @Before
    void setUp() {

        healthCheckService = mock(HealthCheckService.class);
        healthCheckServiceProxy = mock(HealthCheckServiceProxy)
        when(healthCheckService.register()).thenReturn(healthCheckServiceProxy)
        configurationService = mock(ConfigurationService.class)
        datastoreServiceClusterViewService = mock(DistributedDatastoreServiceClusterViewService.class)
        reposeInstanceInfo = mock(ReposeInstanceInfo.class)
        serviceRegistry = mock(ServiceRegistry.class)
        distributedDatastoreServiceClusterContext = new DistributedDatastoreServiceClusterContext(configurationService,
        datastoreServiceClusterViewService, reposeInstanceInfo, serviceRegistry, healthCheckService);
        sce = mock(ServletContextEvent.class)
    }


    @Test
    void shouldHaveRegisteredToHealthCheckService(){

        verify(healthCheckService, times(1)).register()
    }

    @Test
    void shouldHaveRegisteredInitialErrorReports(){

        ConfigurationResourceResolver resourceResolver = mock(ConfigurationResourceResolver.class);
        ConfigurationResource configurationResource = mock(ConfigurationResource.class);
        when(configurationService.getResourceResolver()).thenReturn(resourceResolver);
        when(resourceResolver.resolve(MetricsServiceContext.DEFAULT_CONFIG_NAME)).thenReturn(configurationResource);
        when(configurationService.getResourceResolver().resolve(DistributedDatastoreServiceClusterContext.DEFAULT_CONFIG)).thenReturn(configurationResource);
        when(configurationResource.exists()).thenReturn(false);

        ServletContext servletContext = mock(ServletContext.class)
        when(servletContext.getInitParameter(eq("datastoreServicePort"))).thenReturn("100001")
        when(sce.getServletContext()).thenReturn(servletContext)
        distributedDatastoreServiceClusterContext.contextInitialized(sce)


        verify(healthCheckServiceProxy, times(2)).reportIssue(any(String), any(String), any(Severity))
    }

}
