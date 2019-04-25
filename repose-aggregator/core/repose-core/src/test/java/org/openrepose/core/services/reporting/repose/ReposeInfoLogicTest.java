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
package org.openrepose.core.services.reporting.repose;

import org.junit.Before;
import org.junit.Test;
import org.openrepose.core.services.reporting.ReposeInfo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;

public class ReposeInfoLogicTest {

    ReposeInfoLogic reposeInfoLogic;

    @Before
    public void setup() {
        reposeInfoLogic = new ReposeInfoLogic();
    }

    @Test
    public void shouldIncrementStatusCode() {
        reposeInfoLogic.incrementStatusCodeCount(400, 10);

        assertEquals(1, reposeInfoLogic.getTotalStatusCode(400));
    }

    @Test
    public void shouldIncrementRequests() {
        reposeInfoLogic.incrementRequestCount();

        assertEquals(1, reposeInfoLogic.getTotalRequests());
    }

    @Test
    public void shouldIncrementResponses() {
        reposeInfoLogic.incrementResponseCount();

        assertEquals(1, reposeInfoLogic.getTotalResponses());
    }

    @Test
    public void shouldAccumulateRequestSize() {
        reposeInfoLogic.processRequestSize(105l);

        assertEquals(105l, reposeInfoLogic.getAccumulatedRequestSize());
    }

    @Test
    public void shouldAccumulateResponseSize() {
        reposeInfoLogic.processResponseSize(107l);

        assertEquals(107l, reposeInfoLogic.getAccumulatedResponseSize());
    }

    @Test
    public void shouldUpdateRequestMinMax() {
        reposeInfoLogic.processRequestSize(85l);

        assertEquals(85l, reposeInfoLogic.getMinimumRequestSize());
        assertEquals(85l, reposeInfoLogic.getMaximumRequestSize());
    }

    @Test
    public void shouldUpdateResponseMinMax() {
        reposeInfoLogic.processResponseSize(55l);

        assertEquals(55l, reposeInfoLogic.getMinimumResponseSize());
        assertEquals(55l, reposeInfoLogic.getMaximumResponseSize());
    }

    @Test
    public void shouldGetAverageRequestSize() {
        long totalRequests = 1000l;
        long requestSize = 2l;
        double expectedAverageRequestSize = requestSize / totalRequests;

        for (int i = 0; i < 1000; i++) {
            reposeInfoLogic.incrementRequestCount();
        }

        reposeInfoLogic.processRequestSize(requestSize);

        assertEquals(expectedAverageRequestSize, reposeInfoLogic.getAverageRequestSize(), 0.1);
    }

    @Test
    public void shouldReturnZeroIfNoRequests() {
        assertEquals(0, reposeInfoLogic.getAverageRequestSize(), 0.1);
    }

    @Test
    public void shouldGetAverageResponseSize() {
        long totalResponses = 1000l;
        long responseSize = 2l;
        double expectedAverageResponseSize = responseSize / totalResponses;

        for (int i = 0; i < 1000; i++) {
            reposeInfoLogic.incrementResponseCount();
        }

        reposeInfoLogic.processResponseSize(responseSize);

        assertEquals(expectedAverageResponseSize, reposeInfoLogic.getAverageResponseSize(), 0.1);
    }

    @Test
    public void shouldReturnZeroIfNoResponses() {
        assertEquals(0, reposeInfoLogic.getAverageResponseSize(), 0.1);
    }

    @Test
    public void shouldCopy() {
        reposeInfoLogic.incrementStatusCodeCount(200, 10);

        ReposeInfo copy = reposeInfoLogic.copy();

        reposeInfoLogic.incrementStatusCodeCount(200, 20);

        assertNotSame(copy.getTotalStatusCode(200), reposeInfoLogic.getTotalStatusCode(200));
    }

    /*
    @Test
    public void shouldEqualUnmodifiedCopy() {
        reposeInfoLogic.getStatusCodeCounts().put(400, new StatusCodeResponseStore(5l, 0));

        ReposeInfo copy = reposeInfoLogic.copy();

        assertThat(copy, equalTo(reposeInfoLogic));
    }

    @Test
    public void shouldProduceDifferentHashcodes() {
        reposeInfoLogic.getStatusCodeCounts().put(400, new StatusCodeResponseStore(5l, 0));

        ReposeInfo copy = reposeInfoLogic.copy();
        reposeInfoLogic.getStatusCodeCounts().put(500, new StatusCodeResponseStore(5l, 0));

        assertThat(copy.hashCode(), not(equalTo(reposeInfoLogic.hashCode())));
    }
    */
}
