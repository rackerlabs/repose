package org.openrepose.core.jmx

import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.core.LoggerContext
import org.apache.logging.log4j.test.appender.ListAppender
import org.mockito.ArgumentCaptor
import org.openrepose.commons.config.manager.UpdateListener
import org.openrepose.core.domain.Port
import org.openrepose.core.domain.ServicePorts
import org.openrepose.core.services.config.ConfigurationService
import org.openrepose.core.systemmodel.*
import org.openrepose.services.healthcheck.HealthCheckService
import org.openrepose.services.healthcheck.HealthCheckServiceProxy
import org.openrepose.services.healthcheck.Severity
import spock.lang.Shared
import spock.lang.Specification

import static org.mockito.Matchers.any
import static org.mockito.Matchers.eq
import static org.mockito.Mockito.*

class ConfigurationInformationTest extends Specification {
    @Shared
    ConfigurationService configurationService

    @Shared
    HealthCheckService healthCheckService

    @Shared
    HealthCheckServiceProxy healthCheckServiceProxy

    @Shared
    ServicePorts ports = new ServicePorts()

    def setupSpec() {
        configurationService = mock(ConfigurationService.class)
        healthCheckService = mock(HealthCheckService.class)
        healthCheckServiceProxy = mock(HealthCheckServiceProxy)

        when(healthCheckService.register()).thenReturn(healthCheckServiceProxy)

    }

    def "if localhost can find self in system model on update, should resolve outstanding issues with health check service"() {
        given:
        def configurationInformation = new ConfigurationInformation(configurationService, "cluster1", "node1", healthCheckService)

        def listenerObject
        def listenerCaptor = ArgumentCaptor.forClass(UpdateListener.class)

        doNothing().when(configurationService).subscribeTo(eq("system-model.cfg.xml"), listenerCaptor.capture(), eq(SystemModel.class))

        SystemModel systemModel = getValidSystemModel()
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
        def configurationInformation = new ConfigurationInformation(configurationService, "cluster1", "nopes", healthCheckService)

        def listenerObject
        def listenerCaptor = ArgumentCaptor.forClass(UpdateListener.class)
        LoggerContext ctx = (LoggerContext) LogManager.getContext(false)
        ListAppender app = ((ListAppender)(ctx.getConfiguration().getAppender("List0"))).clear()

        doNothing().when(configurationService).subscribeTo(eq("system-model.cfg.xml"), listenerCaptor.capture(), eq(SystemModel.class))

        SystemModel systemModel = getValidSystemModel()

        configurationInformation.contextInitialized(null)

        listenerObject = listenerCaptor.getValue()

        when:
        listenerObject.configurationUpdated(systemModel)

        then:
        !listenerObject.isInitialized()
        app.getEvents().find { it.getMessage().getFormattedMessage().contains("Unable to identify the local host in the system model") }
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
