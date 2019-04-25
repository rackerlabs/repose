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
package org.openrepose.core.servlet.boot.event;

import org.junit.Before;
import org.junit.Test;
import org.openrepose.core.services.event.EventDispatcher;
import org.openrepose.core.services.event.EventListener;
import org.openrepose.core.services.event.EventService;
import org.openrepose.core.services.event.EventServiceImpl;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.*;

public class EventServiceImplTest {

    private EventService manager;
    private boolean eventFiredTracker;

    @Before
    public void standUp() {
        eventFiredTracker = false;
        manager = new EventServiceImpl();
    }

    @Test
    public void shouldRegisterListeners() throws Exception {
        final String expected = "expected";

        manager.listen(e -> {
            assertEquals("Event payload must match expected value", expected, e.payload());
            eventFiredTracker = true;
        }, TestEvent.ONE);

        manager.newEvent(TestEvent.ONE, expected);
        manager.nextDispatcher().dispatch();

        assertTrue("Event must be fired", eventFiredTracker);
    }

    @Test
    public void shouldUpdateListener() throws Exception {
        final String expected = "expected";

        final EventListener<TestEvent, String> listener = e -> {
            if (e.type() == TestEvent.ONE) {
                assertTrue("Event must be fired", eventFiredTracker);
            }

            if (e.type() == TestEvent.TWO) {
                assertEquals("Event payload must match expected value", expected, e.payload());
                eventFiredTracker = true;
            }
        };

        manager.listen(listener, TestEvent.ONE);
        manager.listen(e -> {}, TestEvent.class);

        manager.listen(listener, TestEvent.TWO);

        manager.newEvent(TestEvent.TWO, expected);
        manager.nextDispatcher().dispatch();

        manager.newEvent(TestEvent.ONE, expected);
        manager.nextDispatcher().dispatch();
    }

    @Test
    public void shouldSquelchIndividualEventsOnListener() throws Exception {
        final String expectedOne = "expectedOne", expectedTwo = "expectedTwo";

        final EventListener<TestEvent, String> listener = e -> {
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
        final EventListener<TestEvent, String> myListener = e -> eventFiredTracker = true;

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

    public enum TestEvent {
        ONE, TWO, THREE
    }

    @Test
    public void shouldBlockWhenNoEventsAreAvailable() throws InterruptedException {
        final OtherTestEvent sentEvent = OtherTestEvent.EVENT_OCCURRED;
        final AtomicReference<Enum> receivedEvent = new AtomicReference<>();
        final AtomicReference<Exception> listenerException = new AtomicReference<>();

        final Thread listener = new Thread(() -> {
            try {
                final EventDispatcher dispatcher = manager.nextDispatcher();
                receivedEvent.set(dispatcher.getEvent().type());
            } catch (Exception ex) {
                listenerException.set(ex);
            }
        }, "Testing thread");

        listener.start();

        for (int i = 0; listener.getState() == Thread.State.RUNNABLE && i < 5; i++) {
            Thread.sleep(500);
        }

        assertEquals("Dispatcher thread must be waiting for further assertions to be valid", Thread.State.WAITING, listener.getState());

        manager.newEvent(sentEvent, "");

        for (int i = 0; listener.getState() != Thread.State.TERMINATED && i < 5; i++) {
            Thread.sleep(500);
        }

        assertEquals("Dispatcher thread must exit successfully", Thread.State.TERMINATED, listener.getState());
        assertNull("Exception caught in listener thread.", listenerException.get());
        assertEquals("Event type received by listener did not match.", sentEvent, receivedEvent.get());
    }

    public enum OtherTestEvent {
        EVENT_OCCURRED
    }
}
