package com.rackspace.papi.jmx

import com.rackspace.papi.commons.config.manager.UpdateListener
import com.rackspace.papi.domain.Port
import com.rackspace.papi.domain.ServicePorts
import com.rackspace.papi.model.*
import com.rackspace.papi.service.config.ConfigurationService
import com.rackspace.papi.service.healthcheck.HealthCheckReport
import com.rackspace.papi.service.healthcheck.HealthCheckService
import com.rackspace.papi.service.healthcheck.HealthCheckServiceProxy
import com.rackspace.papi.service.healthcheck.Severity
import org.apache.log4j.Logger
import org.apache.log4j.SimpleLayout
import org.apache.log4j.WriterAppender
import org.mockito.ArgumentCaptor
import spock.lang.Shared
import spock.lang.Specification

import static org.mockito.Matchers.any
import static org.mockito.Matchers.eq
import static org.mockito.Mockito.*

class ConfigurationInformationTest extends Specification {
    @Shared
    ConfigurationInformation configurationInformation

    @Shared
    ConfigurationService configurationService

    @Shared
    HealthCheckService healthCheckService

    @Shared
    HealthCheckServiceProxy healthCheckServiceProxy

    @Shared
    ServicePorts ports = new ServicePorts()

    @Shared
    ByteArrayOutputStream log = new ByteArrayOutputStream()

    def setupSpec() {
        def logger = Logger.getLogger(ConfigurationInformation.class)

        logger.addAppender(new WriterAppender(new SimpleLayout(), log))

        configurationService = mock(ConfigurationService.class)
        healthCheckService = mock(HealthCheckService.class)
        healthCheckServiceProxy = mock(HealthCheckServiceProxy)

        when(healthCheckService.register()).thenReturn(healthCheckServiceProxy)

        configurationInformation = new ConfigurationInformation(configurationService, ports, healthCheckService)
    }

    def "if localhost can find self in system model on update, should resolve outstanding issues with health check service"() {
        given:
        def listenerObject
        def listenerCaptor = ArgumentCaptor.forClass(UpdateListener.class)

        doNothing().when(configurationService).subscribeTo(eq("system-model.cfg.xml"), listenerCaptor.capture(), eq(SystemModel.class))

        SystemModel systemModel = getValidSystemModel()
        ports.clear()
        ports.add(new Port("http", 8080))

        configurationInformation.contextInitialized(null)

        listenerObject = listenerCaptor.getValue()

        when:
        listenerObject.configurationUpdated(systemModel)

        then:
        listenerObject.isInitialized()
        verify(healthCheckServiceProxy).resolveIssue(eq(ConfigurationInformation.SYSTEM_MODEL_CONFIG_HEALTH_REPORT))
    }

    def "if localhost cannot find self in system model on update, should log error and report to health check service"() {
        given:
        def listenerObject
        def listenerCaptor = ArgumentCaptor.forClass(UpdateListener.class)

        doNothing().when(configurationService).subscribeTo(eq("system-model.cfg.xml"), listenerCaptor.capture(), eq(SystemModel.class))

        SystemModel systemModel = getValidSystemModel()
        ports.clear()

        configurationInformation.contextInitialized(null)

        listenerObject = listenerCaptor.getValue()

        when:
        listenerObject.configurationUpdated(systemModel)

        then:
        !listenerObject.isInitialized()
        new String(log.toByteArray()).contains("Unable to identify the local host in the system model")
        verify(healthCheckServiceProxy).reportIssue(eq(ConfigurationInformation.SYSTEM_MODEL_CONFIG_HEALTH_REPORT), any(String),
                any(Severity))
    }

    /**
     * @return a valid system model
     */
    private static SystemModel getValidSystemModel() {
        Node node = new Node()
        DestinationEndpoint dest = new DestinationEndpoint()
        ReposeCluster cluster = new ReposeCluster()
        SystemModel sysModel = new SystemModel()

        node.setId("node1")
        node.setHostname("localhost")
        node.setHttpPort(8080)

        dest.setHostname("localhost")
        dest.setPort(9090)
        dest.setDefault(true)
        dest.setId("dest1")
        dest.setProtocol("http")

        cluster.setId("cluster1")
        cluster.setNodes(new NodeList())
        cluster.getNodes().getNode().add(node)
        cluster.setDestinations(new DestinationList())
        cluster.getDestinations().getEndpoint().add(dest)

        sysModel.getReposeCluster().add(cluster)

        sysModel
    }
}
