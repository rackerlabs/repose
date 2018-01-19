/*
 * _=_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_=
 * Repose
 * _-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-
 * Copyright (C) 2010 - 2015 Rackspace US, Inc.
 * _-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * =_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_=_
 */
package org.openrepose.core.services.opentracing

import io.opentracing.NoopTracer
import io.opentracing.NoopTracerFactory
import io.opentracing.mock.MockTracer
import io.opentracing.util.GlobalTracer
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.openrepose.commons.config.manager.UpdateListener
import org.openrepose.commons.config.resource.ConfigurationResource
import org.openrepose.commons.config.resource.ConfigurationResourceResolver
import org.openrepose.core.service.opentracing.config.OpenTracingConfig
import org.openrepose.core.service.opentracing.config.TracerType
import org.openrepose.core.services.config.ConfigurationService
import org.openrepose.core.services.healthcheck.HealthCheckService
import org.openrepose.core.services.healthcheck.HealthCheckServiceProxy
import org.openrepose.core.services.healthcheck.Severity

import java.lang.reflect.Field

import static org.hamcrest.Matchers.nullValue
import static org.mockito.Matchers.any
import static org.mockito.Matchers.anyString
import static org.hamcrest.Matchers.instanceOf
import static org.junit.Assert.*
import static org.mockito.Mockito.*


class OpenTracingServiceImplTest{

    OpenTracingServiceImpl openTracingService
    HealthCheckService healthCheckService
    HealthCheckServiceProxy healthCheckServiceProxy
    ConfigurationService configurationService

    @Before
    void setUp() {
        configurationService = mock(ConfigurationService.class)
        healthCheckService = mock(HealthCheckService.class)
        healthCheckServiceProxy = mock(HealthCheckServiceProxy)
        when(healthCheckService.register()).thenReturn(healthCheckServiceProxy)

        openTracingService = new OpenTracingServiceImpl(configurationService, healthCheckService)


        // tears
        Field field = GlobalTracer.class.getDeclaredField("tracer");
        field.setAccessible(true);
        field.set(null, NoopTracerFactory.create())

    }

    @After
    void tearDown() {

        // tears
        Field field = GlobalTracer.class.getDeclaredField("tracer");
        field.setAccessible(true);
        field.set(null, NoopTracerFactory.create())

    }

    @Test
    void testRegistrationToHealthCheckService() {
        verify(healthCheckService, times(1)).register()
    }

    @Test
    void testInitialReportOfConfig() {
        ConfigurationResourceResolver resourceResolver = mock(ConfigurationResourceResolver.class);
        ConfigurationResource configurationResource = mock(ConfigurationResource.class);
        when(configurationService.getResourceResolver()).thenReturn(resourceResolver);
        when(resourceResolver.resolve(OpenTracingServiceImpl.DEFAULT_CONFIG_NAME)).thenReturn(configurationResource);
        when(configurationService.getResourceResolver().resolve(OpenTracingServiceImpl.DEFAULT_CONFIG_NAME)).thenReturn(configurationResource);

        openTracingService.init()
        verify(healthCheckServiceProxy, times(1)).reportIssue(any(String), any(String), any(Severity.class));
    }


    @Test
    void testInitialReportOfConfigFailsIO() {
        ConfigurationResourceResolver resourceResolver = mock(ConfigurationResourceResolver.class);
        ConfigurationResource configurationResource = mock(ConfigurationResource.class);
        when(configurationService.getResourceResolver()).thenReturn(resourceResolver);
        when(resourceResolver.resolve(OpenTracingServiceImpl.DEFAULT_CONFIG_NAME)).thenReturn(configurationResource);
        when(configurationService.getResourceResolver().resolve(OpenTracingServiceImpl.DEFAULT_CONFIG_NAME)).thenThrow(IOException.class);

        openTracingService.init()
        verify(healthCheckServiceProxy, times(1)).reportIssue(any(String), any(String), any(Severity.class));
        verify(healthCheckServiceProxy, times(0)).resolveIssue(any(String));
    }

    @Test
    void testConfigurationUpdated() {

        ConfigurationResourceResolver resourceResolver = mock(ConfigurationResourceResolver.class);
        ConfigurationResource configurationResource = mock(ConfigurationResource.class);
        when(configurationResource.exists()).thenReturn(true)
        when(configurationService.getResourceResolver()).thenReturn(resourceResolver);
        when(resourceResolver.resolve(OpenTracingServiceImpl.DEFAULT_CONFIG_NAME)).thenReturn(configurationResource);
        when(configurationService.getResourceResolver().resolve(OpenTracingServiceImpl.DEFAULT_CONFIG_NAME)).thenReturn(configurationResource);

        OpenTracingConfig openTracingConfig = mock(OpenTracingConfig.class)
        when(openTracingConfig.getTracer()).thenReturn(TracerType.ZIPKIN)
        when(openTracingConfig.isEnabled()).thenReturn(true)
        when(openTracingConfig.getName()).thenReturn("fake-tracer")

        openTracingService.configurationListener.configurationUpdated(openTracingConfig)

        assert (openTracingService.configurationListener.initialized)
        verify(healthCheckServiceProxy, times(1)).resolveIssue(anyString());
    }

    @Test
    void testConfigurationUpdatedAndResolved() {

        ConfigurationResourceResolver resourceResolver = mock(ConfigurationResourceResolver.class);
        ConfigurationResource configurationResource = mock(ConfigurationResource.class);
        when(configurationResource.exists()).thenReturn(true)
        when(configurationService.getResourceResolver()).thenReturn(resourceResolver);
        when(resourceResolver.resolve(OpenTracingServiceImpl.DEFAULT_CONFIG_NAME)).thenReturn(configurationResource);
        when(configurationService.getResourceResolver().resolve(OpenTracingServiceImpl.DEFAULT_CONFIG_NAME)).thenReturn(configurationResource);

        OpenTracingConfig openTracingConfig = mock(OpenTracingConfig.class)
        when(openTracingConfig.getTracer()).thenReturn(TracerType.ZIPKIN)
        when(openTracingConfig.isEnabled()).thenReturn(true)
        when(openTracingConfig.getName()).thenReturn("fake-tracer")

        openTracingService.configurationListener.configurationUpdated(openTracingConfig)

        openTracingService.init()

        assert (openTracingService.configurationListener.initialized)
        verify(healthCheckServiceProxy, times(1)).resolveIssue(anyString());
    }


    @Test
    void testConfigurationUpdatedButNotResolved() {

        ConfigurationResourceResolver resourceResolver = mock(ConfigurationResourceResolver.class);
        ConfigurationResource configurationResource = mock(ConfigurationResource.class);
        when(configurationResource.exists()).thenReturn(false)
        when(configurationService.getResourceResolver()).thenReturn(resourceResolver);
        when(resourceResolver.resolve(OpenTracingServiceImpl.DEFAULT_CONFIG_NAME)).thenReturn(configurationResource);
        when(configurationService.getResourceResolver().resolve(OpenTracingServiceImpl.DEFAULT_CONFIG_NAME)).thenReturn(configurationResource);

        OpenTracingConfig openTracingConfig = mock(OpenTracingConfig.class)
        when(openTracingConfig.getTracer()).thenReturn(TracerType.ZIPKIN)
        when(openTracingConfig.isEnabled()).thenReturn(true)
        when(openTracingConfig.getName()).thenReturn("fake-tracer")

        openTracingService.configurationListener.configurationUpdated(openTracingConfig)

        openTracingService.init()

        assert (openTracingService.configurationListener.initialized)
        verify(healthCheckServiceProxy, times(1)).resolveIssue(anyString());
    }

    @Test
    void testConfigurationNotUpdatedAndNotResolved() {

        ConfigurationResourceResolver resourceResolver = mock(ConfigurationResourceResolver.class);
        ConfigurationResource configurationResource = mock(ConfigurationResource.class);
        when(configurationResource.exists()).thenReturn(false)
        when(configurationService.getResourceResolver()).thenReturn(resourceResolver);
        when(resourceResolver.resolve(OpenTracingServiceImpl.DEFAULT_CONFIG_NAME)).thenReturn(configurationResource);
        when(configurationService.getResourceResolver().resolve(OpenTracingServiceImpl.DEFAULT_CONFIG_NAME)).thenReturn(configurationResource);

        OpenTracingConfig openTracingConfig = mock(OpenTracingConfig.class)
        when(openTracingConfig.getTracer()).thenReturn(TracerType.ZIPKIN)
        when(openTracingConfig.isEnabled()).thenReturn(true)
        when(openTracingConfig.getName()).thenReturn("fake-tracer")

        openTracingService.init()

        verify(healthCheckServiceProxy, times(1)).resolveIssue(anyString());
    }

    @Test
    void testDestroyUnsubscribesFromConfigService() {
        openTracingService.destroy()
        verify(configurationService, times(1)).unsubscribeFrom(anyString(), any(UpdateListener.class))
    }

    @Test
    void testIsEnabled() {

        OpenTracingConfig openTracingConfig = mock(OpenTracingConfig.class)
        when(openTracingConfig.getTracer()).thenReturn(TracerType.ZIPKIN)
        when(openTracingConfig.isEnabled()).thenReturn(true)
        when(openTracingConfig.getName()).thenReturn("fake-tracer")

        openTracingService.configure(openTracingConfig)

        assert openTracingService.isEnabled() == true
    }

    @Test(expected = IllegalStateException.class)
    void verifyGetGlobalTracerUninitialized() {
        openTracingService = new OpenTracingServiceImpl(configurationService, healthCheckService)
        openTracingService.getGlobalTracer()
    }

    @Test
    void verifyGetGlobalTracerRegistered() {

        ConfigurationResourceResolver resourceResolver = mock(ConfigurationResourceResolver.class);
        ConfigurationResource configurationResource = mock(ConfigurationResource.class);
        when(configurationResource.exists()).thenReturn(true)
        when(configurationService.getResourceResolver()).thenReturn(resourceResolver);
        when(resourceResolver.resolve(OpenTracingServiceImpl.DEFAULT_CONFIG_NAME)).thenReturn(configurationResource);
        when(configurationService.getResourceResolver().resolve(OpenTracingServiceImpl.DEFAULT_CONFIG_NAME)).thenReturn(configurationResource);

        OpenTracingConfig openTracingConfig = mock(OpenTracingConfig.class)
        when(openTracingConfig.getTracer()).thenReturn(TracerType.ZIPKIN)
        when(openTracingConfig.isEnabled()).thenReturn(true)
        when(openTracingConfig.getName()).thenReturn("fake-tracer")

        openTracingService.configurationListener.configurationUpdated(openTracingConfig)

        GlobalTracer.register(new MockTracer())

        assertThat(openTracingService.getGlobalTracer(),instanceOf(GlobalTracer.class))
        assertTrue(openTracingService.isEnabled())
        assertEquals(openTracingService.getServiceName(), "fake-tracer")

    }

    @Test
    void testConfigure() {

        ConfigurationResourceResolver resourceResolver = mock(ConfigurationResourceResolver.class);
        ConfigurationResource configurationResource = mock(ConfigurationResource.class);
        when(configurationResource.exists()).thenReturn(true)
        when(configurationService.getResourceResolver()).thenReturn(resourceResolver);
        when(resourceResolver.resolve(OpenTracingServiceImpl.DEFAULT_CONFIG_NAME)).thenReturn(configurationResource);
        when(configurationService.getResourceResolver().resolve(OpenTracingServiceImpl.DEFAULT_CONFIG_NAME)).thenReturn(configurationResource);

        OpenTracingConfig openTracingConfig = mock(OpenTracingConfig.class)
        when(openTracingConfig.getTracer()).thenReturn(TracerType.JAEGER)
        when(openTracingConfig.getTracerHost()).thenReturn("localhost")
        when(openTracingConfig.getTracerPort()).thenReturn(80)
        when(openTracingConfig.isEnabled()).thenReturn(true)
        when(openTracingConfig.getName()).thenReturn("fake-tracer")
        when(openTracingConfig.getMaxBufferSize()).thenReturn(10000)
        when(openTracingConfig.getFlushIntervalMs()).thenReturn(10000)

        openTracingService.configure(openTracingConfig)

    }
}
