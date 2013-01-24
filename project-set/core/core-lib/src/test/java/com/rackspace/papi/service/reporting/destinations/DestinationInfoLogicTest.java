package com.rackspace.papi.service.reporting.destinations;

import com.rackspace.papi.service.reporting.destinations.impl.DestinationInfoLogic;
import com.rackspace.papi.service.reporting.StatusCodeResponseStore;
import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;


@RunWith(Enclosed.class)
public class DestinationInfoLogicTest {

    public static class WhenStoring {

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
            destinationInfoLogic.accumulateResponseTime(1000l);

            assertEquals(1000l, destinationInfoLogic.getAccumulatedResponseTime());
        }
    }

    public static class WhenRetrieving {

        private static final String destinationId = "my_destination";
        private DestinationInfoLogic destinationInfoLogic;
        @Before
        public void setup() {
            destinationInfoLogic = new DestinationInfoLogic(destinationId);
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
            long totalResponses = 1000l;
            long responseTime = 2l;
            double expectedAverageResponseTime = totalResponses/responseTime;

            for (int i = 0; i < 1000; i++) {
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
            long totalResponses = 1000l;

            for (int i = 0; i < 1000; i++) {
                destinationInfoLogic.incrementResponseCount();
            }

            double expectedThroughput = totalResponses/destinationInfoLogic.elapsedTimeInSeconds();
            double actualThroughput = destinationInfoLogic.getThroughput();

            assertEquals(expectedThroughput, actualThroughput, 0.1);
        }

        @Test
        public void shouldReturnZeroIfNoElapsedTime() {
            assertEquals(0, destinationInfoLogic.getThroughput(), 0.1);
        }
    }

    public static class WhenCopying {
        private static final String destinationId = "my_destination";
        private DestinationInfoLogic destinationInfoLogic;
        @Before
        public void setup() {
            destinationInfoLogic = new DestinationInfoLogic(destinationId);
        }

        @Test
        public void shouldCopy() {
            destinationInfoLogic.getStatusCodeCounts().put(200, new StatusCodeResponseStore(7l, 0));

            DestinationInfo copy = destinationInfoLogic.copy();

            destinationInfoLogic.incrementStatusCodeCount(200, 10);

            assertNotSame(copy.getTotalStatusCode(200), destinationInfoLogic.getTotalStatusCode(200));
        }
    }

    public static class WhenComparing {
        private static final String destinationId = "my_destination";
        private DestinationInfoLogic destinationInfoLogic;
        @Before
        public void setup() {
            destinationInfoLogic = new DestinationInfoLogic(destinationId);
        }

        @Test
        public void shouldEqualUnmodifiedCopy() {
            destinationInfoLogic.incrementRequestCount();
            destinationInfoLogic.getStatusCodeCounts().put(400, new StatusCodeResponseStore(5l, 0));

            DestinationInfo copy = destinationInfoLogic.copy();

            assertTrue(copy.equals(destinationInfoLogic));
        }

        @Test
        public void shouldProduceDifferentHashcodes() {
            destinationInfoLogic.incrementRequestCount();
            destinationInfoLogic.getStatusCodeCounts().put(400, new StatusCodeResponseStore(5l, 0));

            DestinationInfo copy = destinationInfoLogic.copy();
            destinationInfoLogic.getStatusCodeCounts().put(500, new StatusCodeResponseStore(5l, 0));

            assertTrue(copy.hashCode() != destinationInfoLogic.hashCode());
        }
    }
}
