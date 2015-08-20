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
package org.openrepose.commons.utils.servlet.http;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;

/**
 * @author malconis
 */
@RunWith(Enclosed.class)
public class RouteDestinationTest {

    public static class WhenCreatingDestinations {

        private RouteDestination routeDst1, routeDst2, routeDst3, routeDst4;

        @Before
        public void setUp() {

            routeDst1 = new RouteDestination("dst1", "/service/dst1", new Float(1.0));
            routeDst2 = new RouteDestination("dst2", "/service/dst2", new Float(-1.0));
            routeDst3 = new RouteDestination("dst3", "/service/dst3", new Float(1.0));
            routeDst4 = new RouteDestination("dst1", "/service/dst4", new Float(1.0));

        }

        @Test
        public void shouldReturnDestinationWithHighestQuality() {

            int compared = routeDst1.compareTo(routeDst2);

            assertEquals(compared, 1);
        }

        @Test
        public void shouldReturnDestinationWithFirstDestinationId() {
            Integer compared = routeDst1.compareTo(routeDst3);

            assertTrue(compared < 0);
        }

        @Test
        public void shouldCompareDestinationsWithUri() {
            int compared = routeDst1.compareTo(routeDst4);

            assertTrue(compared < 0);
        }

        @Test
        public void shouldHaveDifferentHashPerDestination() {
            Integer h1 = routeDst1.hashCode();
            Integer h2 = routeDst2.hashCode();
            assertFalse(h1.equals(h2));
        }

        @Test(expected=IllegalArgumentException.class)
        public void shouldThrowErrorWhenIdIsNull() {
            new RouteDestination(null, null, 0);
        }

        @Test(expected=IllegalArgumentException.class)
        public void shouldThrowErrorWhenCompareNotRouteDestination() {
            routeDst1.compareTo("invalid");
        }

        @Test
        public void shouldNotBeEqual() {
            assertFalse("RouteDestination objects should not be equal", routeDst1.equals(routeDst2));
        }

        @Test
        public void shouldReturnFalseWhenObjectNotRouteDestination() {
            assertFalse("Compare RouteDestination to String", routeDst1.equals("invalid"));
        }

        @Test
        public void shouldGetDestinationId() {
            assertTrue("Compare destinationId to String", routeDst1.getDestinationId().equals("dst1"));
        }

        @Test
        public void shouldGetUri() {
            assertTrue("Compare uri to String", routeDst1.getUri().equals("/service/dst1"));
        }

        @Test
        public void shouldGetQuality() {
            assertTrue("Compare quality to number", routeDst1.getQuality() == 1.0);
        }

        @Test
        public void shouldSetAndGetContextRemoved() {
            routeDst1.setContextRemoved("context");
            assertTrue("Context should equal getContext", routeDst1.getContextRemoved().equals("context"));
        }
    }
}
