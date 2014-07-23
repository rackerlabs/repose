package com.rackspace.papi.jmx
import com.rackspace.papi.service.config.manager.UpdateListener
import com.rackspace.papi.domain.Port
import com.rackspace.papi.domain.ServicePorts
import com.rackspace.papi.model.*
import com.rackspace.papi.service.config.ConfigurationService
import com.rackspace.papi.service.healthcheck.HealthCheckService
import com.rackspace.papi.service.healthcheck.HealthCheckServiceProxy
import com.rackspace.papi.service.healthcheck.Severity
import org.apache.log4j.Logger
import org.apache.log4j.SimpleLayout
import org.apache.log4j.WriterAppender
import org.mockito.ArgumentCaptor
import spock.lang.Shared
import spock.lang.Specification

import static org.mockito.Matchers.eq
import static org.mockito.Mockito.*

class FilterListProviderTest extends Specification {
    @Shared
    FilterListProvider filterListProvider

    @Shared
    HealthCheckService healthCheckService

    @Shared
    ConfigurationService configurationService

    @Shared
    ServicePorts ports = new ServicePorts()

    @Shared
    ByteArrayOutputStream log = new ByteArrayOutputStream()

    @Shared
    HealthCheckServiceProxy healthCheckProxy
    def setupSpec() {
        def logger = Logger.getLogger(FilterListProvider.class)

        logger.addAppender(new WriterAppender(new SimpleLayout(), log))

        healthCheckService = mock(HealthCheckService.class)
        healthCheckProxy = mock(HealthCheckServiceProxy)
        configurationService = mock(ConfigurationService.class)
        def configurationInformation = mock(ConfigurationInformation.class)

        when(healthCheckService.register()).thenReturn(healthCheckProxy)

        filterListProvider = new FilterListProvider(ports,
                configurationService,
                configurationInformation,
                healthCheckService)
    }

    def "if localhost can find self in system model on update, should resolve outstanding issues with health check service"() {
        given:
        def listenerObject
        def listenerCaptor = ArgumentCaptor.forClass(UpdateListener.class)

        doNothing().when(configurationService).subscribeTo(eq("system-model.cfg.xml"), listenerCaptor.capture(), eq(SystemModel.class))

        SystemModel systemModel = getValidSystemModel()
        ports.clear()
        ports.add(new Port("http", 8080))

        filterListProvider.afterPropertiesSet()

        listenerObject = listenerCaptor.getValue()

        when:
        listenerObject.configurationUpdated(systemModel)

        then:
        listenerObject.isInitialized()
        verify(healthCheckProxy).resolveIssue(eq(filterListProvider.SYSTEM_MODEL_CONFIG_HEALTH_REPORT))
    }

    def "if localhost cannot find self in system model on update, should log error and report to health check service"() {
        given:
        def listenerObject
        def listenerCaptor = ArgumentCaptor.forClass(UpdateListener.class)

        doNothing().when(configurationService).subscribeTo(eq("system-model.cfg.xml"), listenerCaptor.capture(), eq(SystemModel.class))

        SystemModel systemModel = getValidSystemModel()
        ports.clear()

        filterListProvider.afterPropertiesSet()

        listenerObject = listenerCaptor.getValue()

        when:
        listenerObject.configurationUpdated(systemModel)

        then:
        !listenerObject.isInitialized()
        new String(log.toByteArray()).contains("Unable to identify the local host in the system model")
        verify(healthCheckProxy).reportIssue(eq(filterListProvider.SYSTEM_MODEL_CONFIG_HEALTH_REPORT), any(String), eq(Severity.BROKEN))
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
