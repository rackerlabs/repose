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
import org.openrepose.core.spring.ReposeJmxNamingStrategy;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;

import static org.mockito.Mockito.*;

public class MetricRegistryAspectTest {

    private final static String JMX_PREFIX = "PREFIX:";

    private MetricRegistry target;
    private AspectJProxyFactory factory;
    private MetricRegistryAspect aspect;
    private MetricRegistry proxy;
    private ReposeJmxNamingStrategy reposeJmxNamingStrategy;

    @Before
    public void setup() throws Exception {
        this.reposeJmxNamingStrategy = mock(ReposeJmxNamingStrategy.class);
        this.target = mock(MetricRegistry.class);
        this.factory = new AspectJProxyFactory(target);
        this.aspect = new MetricRegistryAspect(reposeJmxNamingStrategy);

        factory.setProxyTargetClass(true);

        this.factory.addAspect(aspect);
        this.proxy = factory.getProxy();

        when(reposeJmxNamingStrategy.getJmxPrefix()).thenReturn(JMX_PREFIX);
    }

    @Test
    public void targetMeterNameShouldNotBeAltered() {
        String meterName = "meter.name";

        target.meter(meterName);

        verify(target).meter(meterName);
    }

    @Test
    public void targetMeterNameStartingWithOpenReposePackageShouldNotBeAltered() {
        String meterName = "org.openrepose.meter.name";

        target.meter(meterName);

        verify(target).meter(meterName);
    }

    @Test
    public void proxyMeterNameShouldNotBeAltered() {
        String meterName = "meter.name";

        proxy.meter(meterName);

        verify(target).meter(meterName);
    }

    @Test
    public void proxyMeterNameStartingWithOpenReposePackageShouldBePrefixed() {
        String meterName = "org.openrepose.meter.name";

        proxy.meter(meterName);

        verify(target).meter(reposeJmxNamingStrategy.getJmxPrefix() + meterName);
    }
}
