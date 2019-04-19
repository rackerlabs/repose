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
package org.openrepose.core.services.reporting.destinations;

import org.junit.Before;
import org.junit.Test;
import org.openrepose.core.services.reporting.StatusCodeResponseStore;
import org.openrepose.core.services.reporting.destinations.impl.DestinationInfoLogic;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.*;

public class DestinationInfoLogicTest {

    private static final String destinationId = "my_destination";
    private DestinationInfoLogic destinationInfoLogic;

    @Before
    public void setup() {
        destinationInfoLogic = new DestinationInfoLogic(destinationId);
    }

    @Test
    public void shouldIncrementRequestCount() {
        destinationInfoLogic.incrementRequestCount();

        assertEquals(1, destinationInfoLogic.getTotalRequests());
    }

    @Test
    public void shouldIncrementResponseCount() {
        destinationInfoLogic.incrementResponseCount();

        assertEquals(1, destinationInfoLogic.getTotalResponses());
    }

    @Test
    public void shouldIncrementStatusCodeCount() {
        destinationInfoLogic.incrementStatusCodeCount(400, 10);
        destinationInfoLogic.incrementStatusCodeCount(400, 20);

        assertEquals(2, destinationInfoLogic.getTotalStatusCode(400));
        assertEquals(30, destinationInfoLogic.getTotalResponseTime(400));
    }

    @Test
    public void shouldAccumulateResponseTime() {
        destinationInfoLogic.accumulateResponseTime(1000L);

        assertEquals(1000L, destinationInfoLogic.getAccumulatedResponseTime());
    }

    @Test
    public void shouldGetId() {
        assertEquals(destinationId, destinationInfoLogic.getDestinationId());
    }

    @Test
    public void shouldReturnZeroIfStatusCodeNotTracked() {
        assertEquals(0, destinationInfoLogic.getTotalStatusCode(500));
    }

    @Test
    public void shouldGetAverageResponseTime() {
        long totalResponses = 1000L;
        long responseTime = 2L;
        double expectedAverageResponseTime = totalResponses / responseTime;

        for (int i = 0; i < totalResponses; i++) {
            destinationInfoLogic.incrementResponseCount();
        }

        destinationInfoLogic.accumulateResponseTime(responseTime);

        assertEquals(expectedAverageResponseTime, destinationInfoLogic.getAverageResponseTime(), 0.1);
    }

    @Test
    public void shouldReturnZeroIfNoResponseTime() {
        assertEquals(0, destinationInfoLogic.getAverageResponseTime(), 0.1);
    }

    @Test
    public void shouldGetThroughput() throws InterruptedException {
        long totalResponses = 1000L;

        // As noted below, this value helps normalize the test result by minimizing the effect of test runtime.
        // This value should be significantly longer than the runtime of the test itself.
        // Since this test is currently running in no more than 2ms, 1s should be sufficient.
        // todo: rather than trying to account for test runtime, use a time service that can be injected for
        // todo: more exact testing
        long sleepTime = 1000L;

        // These values are the maximum allowable variance in test runtime.
        // Since this test should definitely complete within 10ms, we use that.
        // We then convert the variance to seconds and adjust for the sleep time to calculate
        // the acceptable throughput variance.
        double maxVarianceInMs = 10.0;
        double maxThroughputVariance = maxVarianceInMs / 1000.0 * sleepTime;

        for (int i = 0; i < totalResponses; i++) {
            destinationInfoLogic.incrementResponseCount();
        }

        // By sleeping for a certain amount of time that is significantly longer than the expected runtime of this
        // test, we can minimize the potential difference between the expected and actual throughput.
        // That is, as wait time increases while runtime is relatively constant (or, at least, with minimal variance),
        // our expected and actual throughput converge.
        Thread.sleep(sleepTime);

        double expectedThroughput = totalResponses / destinationInfoLogic.elapsedTimeInSeconds();
        double actualThroughput = destinationInfoLogic.getThroughput();

        assertEquals(expectedThroughput, actualThroughput, maxThroughputVariance);
    }

    @Test
    public void shouldReturnZeroIfNoElapsedTime() {
        assertEquals(0, destinationInfoLogic.getThroughput(), 0.1);
    }

    @Test
    public void shouldCopy() {
        destinationInfoLogic.getStatusCodeCounts().put(200, new StatusCodeResponseStore(7L, 0));

        DestinationInfo copy = destinationInfoLogic.copy();

        destinationInfoLogic.incrementStatusCodeCount(200, 10);

        assertNotSame(copy.getTotalStatusCode(200), destinationInfoLogic.getTotalStatusCode(200));
    }

    @Test
    public void shouldEqualUnmodifiedCopy() {
        destinationInfoLogic.incrementRequestCount();
        destinationInfoLogic.getStatusCodeCounts().put(400, new StatusCodeResponseStore(5L, 0));

        DestinationInfo copy = destinationInfoLogic.copy();

        assertThat(copy, equalTo(destinationInfoLogic));
    }

    @Test
    public void shouldProduceDifferentHashcodes() {
        destinationInfoLogic.incrementRequestCount();
        destinationInfoLogic.getStatusCodeCounts().put(400, new StatusCodeResponseStore(5L, 0));

        DestinationInfo copy = destinationInfoLogic.copy();
        destinationInfoLogic.getStatusCodeCounts().put(500, new StatusCodeResponseStore(5L, 0));

        assertThat(copy.hashCode(), not(equalTo(destinationInfoLogic.hashCode())));
    }
}
