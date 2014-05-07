package com.rackspace.cloud.valve.controller.service.context.impl

import com.mockrunner.mock.web.MockServletContext
import com.rackspace.cloud.valve.controller.service.ControllerService
import com.rackspace.papi.commons.config.manager.UpdateListener
import com.rackspace.papi.container.config.ContainerConfiguration
import com.rackspace.papi.model.DestinationEndpoint
import com.rackspace.papi.model.DestinationList
import com.rackspace.papi.model.ReposeCluster
import com.rackspace.papi.model.SystemModel
import com.rackspace.papi.service.ServiceRegistry
import com.rackspace.papi.service.config.ConfigurationService
import com.rackspace.papi.service.naming.ServiceContext
import org.junit.experimental.runners.Enclosed
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Qualifier
import spock.lang.Specification

import javax.servlet.ServletContextEvent

import static org.mockito.Matchers.anyString
import static org.mockito.Mockito.mock
import static org.mockito.Mockito.any
import static org.mockito.Mockito.verify
import static org.mockito.Mockito.times
import static org.mockito.Mockito.when

class ReposeValveControllerContextTest extends Specification {
    ServletContextEvent sce
    ServiceRegistry registry
    ControllerService controllerService
    ConfigurationService configurationService

    def setup(){
        sce = mock(ServletContextEvent)
        registry = mock(ServiceRegistry)
        controllerService = mock(ControllerService)
        configurationService = mock(ConfigurationService)

    }

    def "Register - happy path"() {
        given:
        def reposeValveControllerContext = new ReposeValveControllerContext(
                controllerService, registry, configurationService)

        when:
        reposeValveControllerContext.register()

        then:
        verify(registry, times(1)).addService(any(ServiceContext))


    }

    def "Context Initialized - happy path"() {
        given:
        when(sce.getServletContext()).thenReturn(new MockServletContext())
        def reposeValveControllerContext = new ReposeValveControllerContext(
                controllerService, registry, configurationService)

        when:
        reposeValveControllerContext.contextInitialized(sce)

        then:
        verify(registry, times(1)).addService(any(ServiceContext))
        verify(controllerService, times(1)).setConfigDirectory(anyString())
        !reposeValveControllerContext.isInsecure

    }

    def "Context Destroyed - happy path"() {
        given:
        def reposeValveControllerContext = new ReposeValveControllerContext(
                controllerService, registry, configurationService)

        when:
        reposeValveControllerContext.contextDestroyed(sce)

        then:
        verify(controllerService, times(1)).updateManagedInstances(any(Map), any(Set))
    }

    def "Container configuration updated - no system model"(){
        when:
        def reposeValveControllerContext = new ReposeValveControllerContext(
                controllerService, registry, configurationService)

        then:
        !reposeValveControllerContext.containerConfigurationListener.initialized

        when:
        reposeValveControllerContext.containerConfigurationListener.configurationUpdated(new ContainerConfiguration())

        then:
        reposeValveControllerContext.containerConfigurationListener.initialized
        !reposeValveControllerContext.systemModelConfigurationListener.initialized
    }

    def "Container configuration updated - with system model"(){
        when:
        def reposeValveControllerContext = new ReposeValveControllerContext(
                controllerService, registry, configurationService)
        reposeValveControllerContext.systemModel = getValidSystemModel()

        then:
        !reposeValveControllerContext.containerConfigurationListener.initialized
        !reposeValveControllerContext.systemModelConfigurationListener.initialized

        when:
        reposeValveControllerContext.containerConfigurationListener.configurationUpdated(new ContainerConfiguration())

        then:
        reposeValveControllerContext.containerConfigurationListener.initialized
        reposeValveControllerContext.systemModelConfigurationListener.initialized

    }


    /**
     * @return a valid system model
     */
    private SystemModel getValidSystemModel() {
        ReposeCluster cluster = new ReposeCluster()
        SystemModel sysModel = new SystemModel()

        cluster.setId("cluster1")
        cluster.setNodes(new com.rackspace.papi.model.NodeList())
        cluster.getNodes().getNode() <<
                new com.rackspace.papi.model.Node(id: "node1", hostname: "localhost", httpPort: 8080, httpsPort: 8181)
        cluster.setDestinations(new DestinationList())
        cluster.getDestinations().getEndpoint() << new DestinationEndpoint(
                hostname: "localhost", port: 9090, default: true, id: "dest1", protocol: "http")

        sysModel.getReposeCluster().add(cluster)

        return sysModel
    }
}
