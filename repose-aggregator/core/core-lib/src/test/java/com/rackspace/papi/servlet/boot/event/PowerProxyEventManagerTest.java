package com.rackspace.papi.servlet.boot.event;

import com.rackspace.papi.service.event.PowerProxyEventManager;
import com.rackspace.papi.service.event.common.Event;
import com.rackspace.papi.service.event.common.EventDispatcher;
import com.rackspace.papi.service.event.common.EventListener;
import com.rackspace.papi.service.event.common.EventService;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;

@RunWith(Enclosed.class)
public class PowerProxyEventManagerTest {

    public static class WhenAddingEventListeners {

        public static enum TestEvent {
            ONE, TWO, THREE
        }

        private EventService manager;
        private boolean eventFiredTracker;

        @Before
        public void standUp() {
            eventFiredTracker = false;
            manager = new PowerProxyEventManager();
        }

        @Test
        public void shouldRegisterListeners() throws Exception {
            final String expected = "expected";

            manager.listen(new EventListener<TestEvent, String>() {

                @Override
                public void onEvent(Event<TestEvent, String> e) {
                    assertEquals("Event payload must match expected value", expected, e.payload());
                    eventFiredTracker = true;
                }
            }, TestEvent.ONE);

            manager.newEvent(TestEvent.ONE, expected);
            manager.nextDispatcher().dispatch();

            assertTrue("Event must be fired", eventFiredTracker);
        }

        @Test
        public void shouldUpdateListener() throws Exception {
            final String expected = "expected";

            final EventListener<TestEvent, String> listener = new EventListener<TestEvent, String>() {

                @Override
                public void onEvent(Event<TestEvent, String> e) {
                    if (e.type() == TestEvent.ONE) {
                        assertTrue("Event must be fired", eventFiredTracker);
                    }

                    if (e.type() == TestEvent.TWO) {
                        assertEquals("Event payload must match expected value", expected, e.payload());
                        eventFiredTracker = true;
                    }
                }
            };

            manager.listen(listener, TestEvent.ONE);
            manager.listen(new EventListener<TestEvent, String>() {

                @Override
                public void onEvent(Event<TestEvent, String> e) {
                }
            }, TestEvent.class);
            
            manager.listen(listener, TestEvent.TWO);

            manager.newEvent(TestEvent.TWO, expected);
            manager.nextDispatcher().dispatch();
            
            manager.newEvent(TestEvent.ONE, expected);
            manager.nextDispatcher().dispatch();
        }

        @Test
        public void shouldSquelchIndividualEventsOnListener() throws Exception {
            final String expectedOne = "expectedOne", expectedTwo = "expectedTwo";

            final EventListener<TestEvent, String> listener = new EventListener<TestEvent, String>() {

                @Override
                public void onEvent(Event<TestEvent, String> e) {
                    switch (e.type()) {
                        case ONE:
                            if (eventFiredTracker) {
                                fail("Must not call squelched events");
                            }

                            assertEquals("Event payload must match expected value", expectedOne, e.payload());
                            eventFiredTracker = true;
                            break;

                        case TWO:
                            assertEquals("Event payload must match expected value", expectedTwo, e.payload());
                            break;

                        case THREE:
                            fail("Must not call squelched events");
                            break;
                    }
                }
            };

            manager.listen(listener, TestEvent.class);

            manager.newEvent(TestEvent.ONE, expectedOne);
            manager.nextDispatcher().dispatch();

            manager.squelch(listener, TestEvent.ONE, TestEvent.THREE);

            manager.newEvent(TestEvent.ONE, expectedTwo);
            manager.nextDispatcher().dispatch();

            manager.newEvent(TestEvent.TWO, expectedTwo);
            manager.nextDispatcher().dispatch();

            manager.newEvent(TestEvent.THREE, expectedTwo);
            manager.nextDispatcher().dispatch();

            assertTrue("Event must be fired", eventFiredTracker);
        }

        @Test
        public void shouldSquelchEventsForListeners() throws Exception {
            final EventListener<TestEvent, String> myListener = new EventListener<TestEvent, String>() {

                @Override
                public void onEvent(Event<TestEvent, String> e) {
                    eventFiredTracker = true;
                }
            };

            manager.listen(myListener, TestEvent.class);

            manager.newEvent(TestEvent.ONE, "");
            manager.nextDispatcher().dispatch();

            assertTrue("Event must be fired", eventFiredTracker);

            eventFiredTracker = false;

            manager.squelch(myListener, TestEvent.class);

            manager.newEvent(TestEvent.ONE, "");
            manager.nextDispatcher().dispatch();

            assertFalse("Event must not be fired once squelched", eventFiredTracker);
        }

        @Test
        public void shouldNotFailWhenRemovingNonExistentListener() {
            manager.squelch(null, TestEvent.ONE);
        }
    }

    public static class WhenHandlingMulithreadedManagement {
        public static enum TestEvent {
            ONE
        }

        private EventService manager;

        @Before
        public void standUp() {
            manager = new PowerProxyEventManager();
        }

        @Test
        public void shouldBlockWhenNoEventsAreAvailable() throws InterruptedException {
            final TestEvent expectedEvent = TestEvent.ONE;

            final Thread myThread = new Thread(new Runnable() {

                @Override
                public void run() {
                    try {
                        final EventDispatcher dispatcher = manager.nextDispatcher();
                        assertEquals("Event type must match expected", expectedEvent, dispatcher.getEvent().type());
                    } catch (Exception ex) {
                        fail("Exception caught while waiting for event. This is a failure case. Exception: " + ex.toString() + "  - Reason: " + ex.getMessage());
                    }
                }
            }, "Testing thread");

            myThread.start();

            for (int i = 0; myThread.getState() == Thread.State.RUNNABLE && i < 5; i++) {
                Thread.sleep(500);
            }

            assertEquals("Dispatcher thread must be waiting for further assertions to be valid", Thread.State.WAITING, myThread.getState());

            manager.newEvent(expectedEvent, "");

            for (int i = 0; myThread.getState() != Thread.State.TERMINATED && i < 5; i++) {
                Thread.sleep(500);
            }

            assertEquals("Dispatcher thread must exit successfully", Thread.State.TERMINATED, myThread.getState());
        }
    }
}
