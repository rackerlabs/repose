package com.rackspace.papi.service.context.impl
import com.google.common.base.Optional
import com.rackspace.papi.commons.config.manager.UpdateListener
import com.rackspace.papi.commons.util.proxy.RequestProxyService
import com.rackspace.papi.filter.SystemModelInterrogator
import com.rackspace.papi.model.ReposeCluster
import com.rackspace.papi.model.SystemModel
import com.rackspace.papi.service.config.ConfigurationService
import com.rackspace.papi.service.healthcheck.HealthCheckReport
import com.rackspace.papi.service.healthcheck.HealthCheckService
import com.rackspace.papi.service.proxy.httpcomponent.RequestProxyServiceImpl
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
    def RequestProxyServiceImpl requestProxyService

    @Shared
    def SystemModelInterrogator systemModelInterrogator

    @Shared
    def ConfigurationService configurationService

    @Shared
    def HealthCheckService healthCheckService

    @Shared
    def ByteArrayOutputStream log = new ByteArrayOutputStream()

    def setup() {
        def logger = Logger.getLogger(RequestProxyServiceImpl.class)

        logger.addAppender(new WriterAppender(new SimpleLayout(), log))

        systemModelInterrogator = mock(SystemModelInterrogator.class)
        configurationService = mock(ConfigurationService.class)
        healthCheckService = mock(HealthCheckService.class)

        when(healthCheckService.register(any(Class.class))).thenReturn("test_uid")

        requestProxyService = new RequestProxyServiceImpl(configurationService, systemModelInterrogator, healthCheckService)
    }

    def "if localhost can find self in system model on update, should resolve outstanding issues with health check service"() {
        given:
        def listenerObject
        def listenerCaptor = ArgumentCaptor.forClass(UpdateListener.class)
        def localCluster = mock(ReposeCluster.class)

        when(localCluster.isRewriteHostHeader()).thenReturn(false)
        when(systemModelInterrogator.getLocalCluster(any(SystemModel.class))).thenReturn(Optional.of(localCluster))
        doNothing().when(configurationService).subscribeTo(eq("system-model.cfg.xml"), listenerCaptor.capture(), eq(SystemModel.class))

        requestProxyService.afterPropertiesSet()

        listenerObject = listenerCaptor.getValue()


        when:
        listenerObject.configurationUpdated(null)


        then:
        listenerObject.isInitialized()
        verify(healthCheckService).solveIssue(any(String.class), eq(RequestProxyServiceContext.SYSTEM_MODEL_CONFIG_HEALTH_REPORT))
    }

    def "if localhost cannot find self in system model on update, should log error and report to health check service"() {
        given:
        def listenerObject
        def listenerCaptor = ArgumentCaptor.forClass(UpdateListener.class)

        when(systemModelInterrogator.getLocalCluster(any(SystemModel.class))).thenReturn(Optional.absent())
        doNothing().when(configurationService).subscribeTo(eq("system-model.cfg.xml"), listenerCaptor.capture(), eq(SystemModel.class))

        requestProxyService.afterPropertiesSet()

        listenerObject = listenerCaptor.getValue()


        when:
        listenerObject.configurationUpdated(null)


        then:
        !listenerObject.isInitialized()
        new String(log.toByteArray()).contains("Unable to identify the local host in the system model")
        verify(healthCheckService).reportIssue(any(String.class), eq(RequestProxyServiceImpl.SYSTEM_MODEL_CONFIG_HEALTH_REPORT),
                any(HealthCheckReport.class))
    }
}
