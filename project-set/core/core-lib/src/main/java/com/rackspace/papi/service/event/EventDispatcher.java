package com.rackspace.papi.service.event;

import com.rackspace.papi.service.event.common.Event;
import com.rackspace.papi.service.event.listener.EventListenerDescriptor;
import java.util.Set;

public class EventDispatcher {

    private final Set<EventListenerDescriptor> listeners;
    private final Event e;

    public EventDispatcher(Event e, Set<EventListenerDescriptor> listeners) {
        this.listeners = listeners;
        this.e = e;
    }

    public Event getEvent() {
        return e;
    }

    public void dispatch() {
        for (EventListenerDescriptor eventListenerWrapper : listeners) {
            if (eventListenerWrapper.answersTo(e.type())) {
                eventListenerWrapper.getListener().onEvent(e);
            }
        }
    }
}
