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

import com.yammer.metrics.core.Meter;

import java.util.concurrent.TimeUnit;

/**
 * Implements a collection of Meters which share the same JMX type & scope.  These Meters are usually related in some
 * fashion.
 * <p/>
 * By calling the mark() methods, a Meter object is automatically registered under the key as name and can be marked
 * by later calls.
 * <p/>
 * Additionally, an additional Meter registered under the name ACROSS ALL tracks the summary of all Meters in this
 * object.
 * <p/>
 * This is created by the {@link MetricsServiceImpl} factory class.
 * <p/>
 * This class is thread-safe.
 */
public class MeterByCategorySum extends MeterByCategoryImpl {

    public static String ALL = "ACROSS ALL";

    private Meter meter;

    MeterByCategorySum(MetricsService metricsServiceP, Class klassP, String scopeP, String eventTypeP,
                       TimeUnit unitP) {
        super(metricsServiceP, klassP, scopeP, eventTypeP, unitP);

        meter = metricsServiceP.newMeter(klassP, ALL, scopeP, eventTypeP, unitP);
    }

    @Override
    public void mark(String key) {

        checkKey(key);
        super.mark(key);
        meter.mark();
    }

    @Override
    public void mark(String key, long n) {

        checkKey(key);
        super.mark(key, n);
        meter.mark(n);
    }

    private void checkKey(String key) {
        if (key.equals(ALL)) {

            throw new IllegalArgumentException(
                    getClass().getName() + ": The key value '" + key + "' is a reserved key to track stats across all Meters.");
        }
    }
}