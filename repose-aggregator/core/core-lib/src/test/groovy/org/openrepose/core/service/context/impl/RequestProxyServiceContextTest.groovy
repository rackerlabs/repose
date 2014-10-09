package org.openrepose.core.service.context.impl

import com.google.common.base.Optional
import org.openrepose.commons.config.manager.UpdateListener
import org.openrepose.commons.utils.proxy.RequestProxyService
import org.openrepose.core.filter.SystemModelInterrogator
import com.rackspace.papi.model.ReposeCluster
import com.rackspace.papi.model.SystemModel
import org.openrepose.core.service.ServiceRegistry
import org.openrepose.core.service.config.ConfigurationService
import org.openrepose.services.healthcheck.HealthCheckService
import org.openrepose.services.healthcheck.HealthCheckServiceProxy
import org.openrepose.services.healthcheck.Severity
import org.apache.log4j.Logger
import org.apache.log4j.SimpleLayout
import org.apache.log4j.WriterAppender
import org.mockito.ArgumentCaptor
import spock.lang.Shared
import spock.lang.Specification

import static org.mockito.Matchers.any
import static org.mockito.Matchers.eq
import static org.mockito.Mockito.*

class RequestProxyServiceContextTest extends Specification {
    @Shared
    def RequestProxyServiceContext requestProxyServiceContext

    @Shared
    def SystemModelInterrogator systemModelInterrogator

    @Shared
    def ConfigurationService configurationService

    @Shared
    def HealthCheckService healthCheckService

    @Shared
    def HealthCheckServiceProxy healthCheckServiceProxy

    @Shared
    def ByteArrayOutputStream log = new ByteArrayOutputStream()

    def setup() {
        def logger = Logger.getLogger(RequestProxyServiceContext.class)

        logger.addAppender(new WriterAppender(new SimpleLayout(), log))

        def requestProxyService = mock(RequestProxyService.class)
        def serviceRegistry = mock(ServiceRegistry.class)
        systemModelInterrogator = mock(SystemModelInterrogator.class)
        configurationService = mock(ConfigurationService.class)
        healthCheckService = mock(HealthCheckService.class)
        healthCheckServiceProxy = mock(HealthCheckServiceProxy)

        when(healthCheckService.register()).thenReturn(healthCheckServiceProxy)

        requestProxyServiceContext = new RequestProxyServiceContext(requestProxyService, serviceRegistry,
                configurationService, systemModelInterrogator, healthCheckService)
    }

    def "if localhost can find self in system model on update, should resolve outstanding issues with health check service"() {
        given:
        def listenerObject
        def listenerCaptor = ArgumentCaptor.forClass(UpdateListener.class)
        def localCluster = mock(ReposeCluster.class)

        when(localCluster.isRewriteHostHeader()).thenReturn(false)
        when(systemModelInterrogator.getLocalCluster(any(SystemModel.class))).thenReturn(Optional.of(localCluster))
        doNothing().when(configurationService).subscribeTo(eq("system-model.cfg.xml"), listenerCaptor.capture(), eq(SystemModel.class))

        requestProxyServiceContext.contextInitialized(null)

        listenerObject = listenerCaptor.getValue()


        when:
        listenerObject.configurationUpdated(null)


        then:
        listenerObject.isInitialized()
        verify(healthCheckServiceProxy).resolveIssue(eq(RequestProxyServiceContext.SYSTEM_MODEL_CONFIG_HEALTH_REPORT))
    }

    def "if localhost cannot find self in system model on update, should log error and report to health check service"() {
        given:
        def listenerObject
        def listenerCaptor = ArgumentCaptor.forClass(UpdateListener.class)

        when(systemModelInterrogator.getLocalCluster(any(SystemModel.class))).thenReturn(Optional.absent())
        doNothing().when(configurationService).subscribeTo(eq("system-model.cfg.xml"), listenerCaptor.capture(), eq(SystemModel.class))

        requestProxyServiceContext.contextInitialized(null)

        listenerObject = listenerCaptor.getValue()


        when:
        listenerObject.configurationUpdated(null)


        then:
        !listenerObject.isInitialized()
        new String(log.toByteArray()).contains("Unable to identify the local host in the system model")
        verify(healthCheckServiceProxy).reportIssue(eq(RequestProxyServiceContext.SYSTEM_MODEL_CONFIG_HEALTH_REPORT), any(String.class),
                any(Severity))
    }
}
