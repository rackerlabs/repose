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
package org.openrepose.core.filter;

import org.junit.Ignore;
import org.junit.Test;
import org.openrepose.commons.utils.servlet.http.MutableHttpServletResponse;
import org.openrepose.powerfilter.RequestTracer;

import static org.mockito.Mockito.mock;

/**
 * A jUnit test class for the performance of the RequestTracer class.
 */
@Ignore("Should only be run manually")
public class RequestTracerPerfTest {

    private final long NUM_HITS = 1000L;
    private final int NUM_THREADS = 100;
    private final MutableHttpServletResponse response = mock(MutableHttpServletResponse.class);

    @Test
    public void checkPerformanceUnderLoad() {
        boolean trace = true;
        boolean addHeader = true;
        long totalMillis = 0;

        final RequestTracer rt = new RequestTracer(trace, addHeader);

        // Ramp up, should not be necessary
        for (int i = 0; i < 100; ++i) {
            rt.traceEnter();
            rt.traceExit(response, "myFilter");
        }

        // Sequential request test
        for (int i = 0; i < NUM_HITS; ++i) {
            long startTime = System.currentTimeMillis();
            rt.traceEnter();
            rt.traceExit(response, "myFilter");
            long endTime = System.currentTimeMillis();
            totalMillis += endTime - startTime;
        }

        // Average time (ms) delay per trace
        double avg = totalMillis / NUM_HITS;
    }

    @Test
    public void checkPerformanceUnderLoadMultiThreaded() {
        boolean trace = true;
        boolean addHeader = true;
        long totalTime = 0;

        final RequestTracer rt = new RequestTracer(trace, addHeader);

        final Thread[] threads = new Thread[NUM_THREADS];
        final long[] times = new long[NUM_THREADS];

        // Ramp up, should not be necessary
        for (int i = 0; i < 100; ++i) {
            rt.traceEnter();
            rt.traceExit(response, "myFilter");
        }

        // Threaded request test
        for (int i = 0; i < NUM_THREADS; ++i) {
            final int index = i;
            Thread thread = new Thread() {
                public void run() {
                    long totalMillis = 0;
                    for (int i = 0; i < NUM_HITS; ++i) {
                        long startTime = System.currentTimeMillis();
                        rt.traceEnter();
                        rt.traceExit(response, "myFilter");
                        long endTime = System.currentTimeMillis();
                        totalMillis += endTime - startTime;
                    }
                    times[index] = totalMillis;
                }
            };
            threads[index] = thread;
        }

        for (Thread t : threads) {
            t.start();
        }
        for (Thread t : threads) {
            try {
                t.join();
            } catch (Exception e) {
            }
        }
        for (long l : times) {
            totalTime += l;
        }

        // Average time (ms) delay per trace
        double avg = (double) totalTime / (double) (NUM_HITS * NUM_THREADS);
    }
}
