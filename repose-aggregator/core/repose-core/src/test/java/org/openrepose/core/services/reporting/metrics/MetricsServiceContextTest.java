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
package org.openrepose.core.services.reporting.metrics;

import org.junit.Before;
import org.junit.Test;
import org.openrepose.commons.config.resource.ConfigurationResource;
import org.openrepose.commons.config.resource.ConfigurationResourceResolver;
import org.openrepose.core.services.config.ConfigurationService;
import org.openrepose.core.services.healthcheck.HealthCheckService;
import org.openrepose.core.services.healthcheck.HealthCheckServiceProxy;
import org.openrepose.core.services.healthcheck.Severity;

import java.io.IOException;

import static org.mockito.Mockito.*;

/**
 * This is testing a few things that used to only exist int he "service context" thing that no longer exists
 * TODO: This should probably be refactored into the MetricsService Impl test.
 */
public class MetricsServiceContextTest {
    private MetricsServiceImpl metricsService;

    private ConfigurationService configurationService;
    private HealthCheckService healthCheckService;
    private HealthCheckServiceProxy healthCheckServiceProxy;

    @Before
    public void setUp() {
        configurationService = mock(ConfigurationService.class);
        healthCheckService = mock(HealthCheckService.class);
        healthCheckServiceProxy = mock(HealthCheckServiceProxy.class);
        when(healthCheckService.register()).thenReturn(healthCheckServiceProxy);
        metricsService = new MetricsServiceImpl(configurationService, healthCheckService);
    }

    @Test
    public void verifyRegisteredToHealthCheckService() {

        verify(healthCheckService, times(1)).register();
    }

    @Test
    public void verifyIssueReported() throws IOException {

        ConfigurationResourceResolver resourceResolver = mock(ConfigurationResourceResolver.class);
        ConfigurationResource configurationResource = mock(ConfigurationResource.class);
        when(configurationService.getResourceResolver()).thenReturn(resourceResolver);
        when(resourceResolver.resolve(MetricsServiceImpl.DEFAULT_CONFIG_NAME)).thenReturn(configurationResource);
        when(configurationService.getResourceResolver().resolve(MetricsServiceImpl.DEFAULT_CONFIG_NAME)).thenReturn(configurationResource);

        when(configurationResource.exists()).thenReturn(false);
        metricsService.init();
        verify(healthCheckServiceProxy, times(1)).reportIssue(any(String.class), any(String.class), any(Severity.class));
    }
}
