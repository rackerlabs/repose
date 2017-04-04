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
import com.codahale.metrics.MetricRegistry.MetricSupplier;

import static com.codahale.metrics.MetricRegistry.name;

public class SummingMeterFactoryImpl implements SummingMeterFactory {

    public static final String ACROSS_ALL = "ACROSS ALL";

    private final String namePrefix;
    private final MetricRegistry metricRegistry;
    private final Meter acrossAllMeter;
    private final SummingMeterSupplier summingMeterSupplier;

    public SummingMeterFactoryImpl(MetricRegistry metricRegistry, String namePrefix) {
        this.namePrefix = namePrefix;
        this.metricRegistry = metricRegistry;
        this.acrossAllMeter = metricRegistry.meter(name(namePrefix, ACROSS_ALL));
        this.summingMeterSupplier = new SummingMeterSupplier(acrossAllMeter);
    }

    private SummingMeterFactoryImpl(MetricRegistry metricRegistry, String namePrefix, SummingMeterSupplier ancestralMeterSupplier) {
        this.namePrefix = namePrefix;
        this.metricRegistry = metricRegistry;
        this.acrossAllMeter = metricRegistry.meter(name(namePrefix, ACROSS_ALL), ancestralMeterSupplier);
        this.summingMeterSupplier = new SummingMeterSupplier(acrossAllMeter);
    }

    @Override
    public Meter createSummingMeter(String name) {
        return metricRegistry.meter(name(namePrefix, name), summingMeterSupplier);
    }

    @Override
    public Meter getAcrossAllMeter() {
        return acrossAllMeter;
    }

    @Override
    public SummingMeterFactoryImpl createChildFactory(String name) {
        return new SummingMeterFactoryImpl(metricRegistry, name(namePrefix, name), summingMeterSupplier);
    }

    public static class SummingMeterSupplier implements MetricSupplier<Meter> {

        private final Meter[] auxiliaryMeters;

        public SummingMeterSupplier(Meter... auxiliaryMeters) {
            this.auxiliaryMeters = auxiliaryMeters;
        }

        @Override
        public Meter newMetric() {
            return new MultiMeter(auxiliaryMeters);
        }
    }
}
