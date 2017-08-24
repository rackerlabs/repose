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

import com.codahale.metrics.MetricRegistry;
import org.junit.Before;
import org.junit.Test;
import org.openrepose.core.services.config.ConfigurationService;
import org.openrepose.core.services.healthcheck.HealthCheckService;

import javax.management.*;

import static org.hamcrest.Matchers.hasKey;
import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;

public class MetricsServiceImplTest {

    private MetricsServiceImpl metricsService;

    @Before
    public void setUp() {
        metricsService = new MetricsServiceImpl(
            mock(ConfigurationService.class),
            mock(HealthCheckService.class),
            new MetricsJmxObjectNameFactory());
    }

    @Test
    public void testGetRegistry() {
        MetricRegistry registry = metricsService.getRegistry();

        assertNotNull(registry);
    }

    @Test
    public void testServiceEnabledDisabled()
        throws
        MalformedObjectNameException,
        AttributeNotFoundException,
        MBeanException,
        ReflectionException,
        InstanceNotFoundException {

        metricsService.setEnabled(false);
        assertFalse(metricsService.isEnabled());

        metricsService.setEnabled(true);
        assertTrue(metricsService.isEnabled());
    }

    @Test
    public void createSummingMeterFactoryShouldBePassedTheServiceMetricRegistry() throws Exception {
        String namePrefix = MetricRegistry.name("test", "name", "prefix");
        String name = "foo";

        MetricRegistry metricRegistry = metricsService.getRegistry();
        AggregateMeterFactory summingMeterFactory = metricsService.createSummingMeterFactory(namePrefix);

        summingMeterFactory.createMeter(name);

        assertThat(metricRegistry.getMeters(), hasKey(MetricRegistry.name(namePrefix, name)));
    }
}
