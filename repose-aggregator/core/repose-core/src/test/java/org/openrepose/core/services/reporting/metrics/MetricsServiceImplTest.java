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

import com.codahale.metrics.Counter;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.openrepose.core.services.config.ConfigurationService;
import org.openrepose.core.services.healthcheck.HealthCheckService;
import org.openrepose.core.spring.ReposeJmxNamingStrategy;

import javax.management.*;
import java.lang.management.ManagementFactory;
import java.util.Hashtable;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.Matchers.greaterThan;
import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(Enclosed.class)
public class MetricsServiceImplTest {

    public static class Register {

        protected final String JMX_PREFIX = "MOCK-PREFIX-";
        protected MetricsServiceImpl metricsService;
        protected ReposeJmxNamingStrategy jmxNamingStrategy;

        @Before
        public void setUp() {
            jmxNamingStrategy = mock(ReposeJmxNamingStrategy.class);
            when(jmxNamingStrategy.getJmxPrefix()).thenReturn(JMX_PREFIX);
            metricsService = new MetricsServiceImpl(mock(ConfigurationService.class), mock(HealthCheckService.class), jmxNamingStrategy);
        }

        protected Object getAttribute(Class klass, String name, String scope, String att)
                throws
                MalformedObjectNameException,
                AttributeNotFoundException,
                MBeanException,
                ReflectionException,
                InstanceNotFoundException {

            Hashtable<String, String> hash = new Hashtable<>();
            hash.put("name", "\"" + name + "\"");
            hash.put("scope", "\"" + scope + "\"");
            hash.put("type", "\"" + klass.getSimpleName() + "\"");

            // Lets you see all registered MBean ObjectNames
            //Set<ObjectName> set = ManagementFactory.getPlatformMBeanServer().queryNames(null, null);

            ObjectName on = new ObjectName("\"" + JMX_PREFIX + klass.getPackage().getName() + "\"", hash);

            return ManagementFactory.getPlatformMBeanServer().getAttribute(on, att);
        }

        @Test
        public void testServiceMeter()
                throws
                MalformedObjectNameException,
                AttributeNotFoundException,
                MBeanException,
                ReflectionException,
                InstanceNotFoundException {

            Meter m = metricsService.getRegistry().meter(this.getClass().getName() + ".meter1.scope1.hits");

            m.mark();
            m.mark();
            m.mark();

            long l = (Long) getAttribute(this.getClass(), "meter1", "scope1", "Count");

            assertEquals((long) 3, l);
        }

        @Test
        public void testServiceCounter()
                throws
                MalformedObjectNameException,
                AttributeNotFoundException,
                MBeanException,
                ReflectionException,
                InstanceNotFoundException {

            Counter c = metricsService.getRegistry().counter(this.getClass().getName() + ".counter1.scope1");

            c.inc();
            c.inc();
            c.inc();
            c.inc();
            c.dec();

            long l = (Long) getAttribute(this.getClass(), "counter1", "scope1", "Count");

            assertEquals((long) 3, l);
        }

        @Test
        public void testServiceTimer() throws
                MalformedObjectNameException,
                AttributeNotFoundException,
                MBeanException,
                ReflectionException,
                InstanceNotFoundException {

            Timer t = metricsService.getRegistry().timer(this.getClass().getName() + ".name1.scope1");

            Timer.Context tc = t.time();
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ie) {
                // We don't care.
            }
            tc.stop();

            assertEquals(1L, ((Long) getAttribute(this.getClass(), "name1", "scope1", "Count")).longValue());
            assertThat((Double) getAttribute(this.getClass(), "name1", "scope1", "Mean"), greaterThan(0.0));

            t.update(1000L, TimeUnit.MILLISECONDS);

            assertEquals(2L, ((Long) getAttribute(this.getClass(), "name1", "scope1", "Count")).longValue());
            assertThat((Double) getAttribute(this.getClass(), "name1", "scope1", "Mean"), greaterThan(0.0));
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
    }
}
