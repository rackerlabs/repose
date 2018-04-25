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

/**
 * A {@link Meter} that links multiple {@link Meter}s together.
 * <p>
 * Linked {@link Meter}s will all be marked when this {@link Meter}
 * is marked.
 * <p>
 * This class also provides convenient static methods for marking a set of {@link Meter}s.
 */
public class MultiMeter extends Meter {

    private final Meter[] auxiliaryMeters;

    public MultiMeter(Meter... auxiliaryMeters) {
        this.auxiliaryMeters = auxiliaryMeters;
    }

    public static void markAll(Meter... meters) {
        for (Meter meter : meters) {
            meter.mark();
        }
    }

    public static void markNAll(long n, Meter... meters) {
        for (Meter meter : meters) {
            meter.mark(n);
        }
    }

    @Override
    public void mark() {
        // This is an exact duplicate of the method body in super.
        // It is included so that if the inherited method changes, our behavior does not.
        mark(1L);
    }

    @Override
    public void mark(long n) {
        super.mark(n);
        markNAll(n, auxiliaryMeters);
    }
}
