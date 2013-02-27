package com.rackspace.repose.management.reporting;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertTrue;

@RunWith(Enclosed.class)
public class DestinationTest {

    public static class TestParent {

        String string;
        Destination destination;

        @Before
        public void setUp() throws Exception {
            string = "string";
            destination = new Destination();
        }

        @Test
        public void shouldSetAndGetDestinationId() {
            destination.setDestinationId(string);
            assertTrue(destination.getDestinationId().equals(string));
        }

        @Test
        public void shouldSetAndGetTotalRequests() {
            destination.setTotalRequests(string);
            assertTrue(destination.getTotalRequests().equals(string));
        }

        @Test
        public void shouldSetAndGetTotal400s() {
            destination.setTotal400s(string);
            assertTrue(destination.getTotal400s().equals(string));
        }

        @Test
        public void shouldSetAndGetTotal500s() {
            destination.setTotal500s(string);
            assertTrue(destination.getTotal500s().equals(string));
        }

        @Test
        public void shouldSetAndGetResponseTimeInMillis() {
            destination.setResponseTimeInMillis(string);
            assertTrue(destination.getResponseTimeInMillis().equals(string));
        }

        @Test
        public void shouldSetAndGetThroughputInSeconds() {
            destination.setThroughputInSeconds(string);
            assertTrue(destination.getThroughputInSeconds().equals(string));
        }
    }
}
