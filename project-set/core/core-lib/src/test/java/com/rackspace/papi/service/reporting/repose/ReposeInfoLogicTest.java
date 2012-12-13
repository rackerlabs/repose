package com.rackspace.papi.service.reporting.repose;

import com.rackspace.papi.service.reporting.StatusCodeResponseStore;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;

@RunWith(Enclosed.class)
public class ReposeInfoLogicTest {

    public static class WhenStoring {
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
    }

    public static class WhenRetrieving {
        ReposeInfoLogic reposeInfoLogic;

        @Before
        public void setup() {
            reposeInfoLogic = new ReposeInfoLogic();
        }

        @Test
        public void shouldGetAverageRequestSize() {
            long totalRequests = 1000l;
            long requestSize = 2l;
            double expectedAverageRequestSize = requestSize/totalRequests;

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
            double expectedAverageResponseSize = responseSize/totalResponses;

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
    }

    public static class WhenCopying {
        ReposeInfoLogic reposeInfoLogic;

        @Before
        public void setup() {
            reposeInfoLogic = new ReposeInfoLogic();
        }

        @Test
        public void shouldCopy(){
            reposeInfoLogic.incrementStatusCodeCount(200, 10);

            ReposeInfo copy = reposeInfoLogic.copy();

            reposeInfoLogic.incrementStatusCodeCount(200, 20);
            
            assertNotSame(copy.getTotalStatusCode(200), reposeInfoLogic.getTotalStatusCode(200));
        }
    }

    /*
    public static class WhenComparing {
        ReposeInfoLogic reposeInfoLogic;

        @Before
        public void setup() {
            reposeInfoLogic = new ReposeInfoLogic();
        }

        @Test
        public void shouldEqualUnmodifiedCopy() {
            reposeInfoLogic.getStatusCodeCounts().put(400, new StatusCodeResponseStore(5l, 0));

            ReposeInfo copy = reposeInfoLogic.copy();

            assertTrue(copy.equals(reposeInfoLogic));
        }

        @Test
        public void shouldProduceDifferentHashcodes() {
            reposeInfoLogic.getStatusCodeCounts().put(400, new StatusCodeResponseStore(5l, 0));

            ReposeInfo copy = reposeInfoLogic.copy();
            reposeInfoLogic.getStatusCodeCounts().put(500, new StatusCodeResponseStore(5l, 0));

            assertTrue(copy.hashCode() != reposeInfoLogic.hashCode());
        }
    }
    */
}
