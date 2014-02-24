package com.rackspace.papi.service.datastore.distributed.impl.distributed.cluster
import com.rackspace.papi.domain.ReposeInstanceInfo
import com.rackspace.papi.service.ServiceRegistry
import com.rackspace.papi.service.config.ConfigurationService
import com.rackspace.papi.service.healthcheck.HealthCheckReport
import com.rackspace.papi.service.healthcheck.HealthCheckService
import org.junit.Before
import org.junit.Test

import javax.servlet.ServletContext
import javax.servlet.ServletContextEvent

import static org.mockito.Matchers.any
import static org.mockito.Mockito.*

class DistributedDatastoreServiceClusterContextTest {

    DistributedDatastoreServiceClusterContext distributedDatastoreServiceClusterContext;
    HealthCheckService healthCheckService;
    ConfigurationService configurationService;
    DistributedDatastoreServiceClusterViewService datastoreServiceClusterViewService;
    ReposeInstanceInfo reposeInstanceInfo;
    ServiceRegistry serviceRegistry;
    ServletContextEvent sce;


    @Before
    void setUp() {

        healthCheckService = mock(HealthCheckService.class);
        when(healthCheckService.register(any(DistributedDatastoreServiceClusterContext.class))).thenReturn("UID")
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

        verify(healthCheckService, times(1)).register(any(DistributedDatastoreServiceClusterContext.class))
    }

    @Test
    void shouldHaveRegisteredInitialErrorReports(){

        ServletContext servletContext = mock(ServletContext.class)
        when(servletContext.getInitParameter(eq("datastoreServicePort"))).thenReturn("100001")
//        when(servletContext.setAttribute(eq("ddClusterViewService"), any(DistributedDatastoreServiceClusterViewService.class))).thenReturn(true)
        when(sce.getServletContext()).thenReturn(servletContext)
        distributedDatastoreServiceClusterContext.contextInitialized(sce)


        verify(healthCheckService, times(2)).reportIssue(any(String), any(String), any(HealthCheckReport.class))
    }

}
