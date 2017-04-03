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
import static org.openrepose.core.services.reporting.metrics.SummingMeterFactoryImpl.ACROSS_ALL;
import static org.openrepose.core.services.reporting.metrics.SummingMeterFactoryImpl.SummingMeterSupplier;

public class SummingMeterFactoryImplTest {

    private final static String NAME_PREFIX = "test.name.prefix";

    private Meter acrossAllMeter;
    private Meter aggregateMeter;
    private MetricRegistry metricRegistry;
    private SummingMeterFactory summingMeterFactory;

    @Before
    public void setup() {
        acrossAllMeter = mock(Meter.class);
        aggregateMeter = mock(SummingMeter.class);
        metricRegistry = mock(MetricRegistry.class);

        when(metricRegistry.meter(Matchers.endsWith(ACROSS_ALL)))
            .thenReturn(acrossAllMeter);
        when(metricRegistry.meter(anyString(), any(MetricRegistry.MetricSupplier.class)))
            .thenReturn(aggregateMeter);

        summingMeterFactory = new SummingMeterFactoryImpl(metricRegistry, NAME_PREFIX);
    }

    @Test
    public void constructionShouldRegisterAcrossAllMeter() throws Exception {
        verify(metricRegistry).meter(name(NAME_PREFIX, ACROSS_ALL));
    }

    @Test
    public void createAggregateMeterShouldRegisterAndReturnAnAggregatedMeter() throws Exception {
        String meterName = "foo";

        Meter meter = summingMeterFactory.createSummingMeter(meterName);

        verify(metricRegistry).meter(eq(name(NAME_PREFIX, meterName)), argThat(instanceOf(SummingMeterSupplier.class)));
        assertThat(meter, is(aggregateMeter));
    }

    @Test
    public void getAcrossAllMeterShouldReturnTheAcrossAllMeter() throws Exception {
        Meter meter = summingMeterFactory.getAcrossAllMeter();

        assertThat(meter, is(acrossAllMeter));
    }

    @Test
    public void createChildFactoryShouldReturnASummingMeterFactoryThatProducesHierarchicallyCumulativeMeters() throws Exception {
        MetricRegistry registry = new MetricRegistry();
        SummingMeterFactory parentFactory = new SummingMeterFactoryImpl(registry, "parent");
        SummingMeterFactory childFactory = parentFactory.createChildFactory("child");
        SummingMeterFactory grandchildFactory = childFactory.createChildFactory("grandchild");

        Meter grandchildAllMeter = grandchildFactory.getAcrossAllMeter();
        grandchildAllMeter.mark();

        assertThat(parentFactory.getAcrossAllMeter().getCount(), is(1L));
        assertThat(childFactory.getAcrossAllMeter().getCount(), is(1L));
        assertThat(grandchildFactory.getAcrossAllMeter().getCount(), is(1L));
    }
}
