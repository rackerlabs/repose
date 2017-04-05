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

import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Matchers;

import static com.codahale.metrics.MetricRegistry.name;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;
import static org.openrepose.core.services.reporting.metrics.SummingMeterFactory.ACROSS_ALL;
import static org.openrepose.core.services.reporting.metrics.SummingMeterFactory.SummingMeterSupplier;

public class SummingMeterFactoryImplTest {

    private final static String NAME_PREFIX = "test.name.prefix";

    private Meter acrossAllMeter;
    private Meter meter;
    private MetricRegistry metricRegistry;
    private AggregateMeterFactory summingMeterFactory;

    @Before
    public void setup() {
        acrossAllMeter = mock(Meter.class);
        meter = mock(Meter.class);
        metricRegistry = mock(MetricRegistry.class);

        when(metricRegistry.meter(Matchers.endsWith(ACROSS_ALL)))
            .thenReturn(acrossAllMeter);
        when(metricRegistry.meter(anyString(), any(MetricRegistry.MetricSupplier.class)))
            .thenReturn(meter);

        summingMeterFactory = new SummingMeterFactory(metricRegistry, NAME_PREFIX);
    }

    @Test
    public void constructionShouldRegisterAcrossAllMeter() throws Exception {
        verify(metricRegistry).meter(name(NAME_PREFIX, ACROSS_ALL));
    }

    @Test
    public void createAggregateMeterShouldRegisterAndReturnAnAggregatedMeter() throws Exception {
        String meterName = "foo";

        Meter meter = summingMeterFactory.createMeter(meterName);

        verify(metricRegistry).meter(eq(name(NAME_PREFIX, meterName)), argThat(instanceOf(SummingMeterSupplier.class)));
        assertThat(meter, is(this.meter));
    }

    @Test
    public void getAcrossAllMeterShouldReturnTheAcrossAllMeter() throws Exception {
        Meter meter = summingMeterFactory.getAggregateMeter();

        assertThat(meter, is(acrossAllMeter));
    }

    @Test
    public void createChildFactoryShouldReturnASummingMeterFactoryThatProducesHierarchicallyCumulativeMeters() throws Exception {
        MetricRegistry registry = new MetricRegistry();
        AggregateMeterFactory parentFactory = new SummingMeterFactory(registry, "parent");
        AggregateMeterFactory childFactory = parentFactory.createChildFactory("child");
        AggregateMeterFactory grandchildFactory = childFactory.createChildFactory("grandchild");

        Meter grandchildAllMeter = grandchildFactory.getAggregateMeter();
        grandchildAllMeter.mark();

        assertThat(parentFactory.getAggregateMeter().getCount(), is(1L));
        assertThat(childFactory.getAggregateMeter().getCount(), is(1L));
        assertThat(grandchildFactory.getAggregateMeter().getCount(), is(1L));
    }
}
