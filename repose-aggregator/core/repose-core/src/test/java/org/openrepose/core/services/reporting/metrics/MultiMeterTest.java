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
import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class MultiMeterTest {

    @Test
    public void markAllShouldMarkAllInputMetersOnCall() throws Exception {
        Meter meterOne = mock(Meter.class);
        Meter meterTwo = mock(Meter.class);
        Meter meterThree = mock(Meter.class);

        MultiMeter.markAll(meterOne, meterTwo, meterThree);

        verify(meterOne).mark();
        verify(meterTwo).mark();
        verify(meterThree).mark();
    }

    @Test
    public void markNAllShouldMarkAllInputMetersNTimesOnCall() throws Exception {
        long n = 10;

        Meter meterOne = mock(Meter.class);
        Meter meterTwo = mock(Meter.class);
        Meter meterThree = mock(Meter.class);

        MultiMeter.markNAll(n, meterOne, meterTwo, meterThree);

        verify(meterOne).mark(n);
        verify(meterTwo).mark(n);
        verify(meterThree).mark(n);
    }

    @Test
    public void markShouldMarkAllMetersOnce() throws Exception {
        Meter meterOne = mock(Meter.class);
        Meter meterTwo = mock(Meter.class);

        MultiMeter multiMeter = new MultiMeter(meterOne, meterTwo);
        multiMeter.mark();

        verify(meterOne).mark(1L);
        verify(meterTwo).mark(1L);
        assertThat(multiMeter.getCount(), is(1L));
    }

    @Test
    public void markNShouldMarkAllMetersNTimes() throws Exception {
        long n = 10;

        Meter meterOne = mock(Meter.class);
        Meter meterTwo = mock(Meter.class);

        MultiMeter multiMeter = new MultiMeter(meterOne, meterTwo);
        multiMeter.mark(n);

        verify(meterOne).mark(n);
        verify(meterTwo).mark(n);
        assertThat(multiMeter.getCount(), is(n));
    }
}
