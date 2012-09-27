package com.rackspace.papi.service.event.common.impl;

import com.rackspace.papi.service.event.common.Event;
import com.rackspace.papi.service.event.common.EventDispatcher;

import java.util.Set;

public class EventDispatcherImpl implements EventDispatcher {

    private final Set<EventListenerDescriptor> listeners;
    private final Event e;

    public EventDispatcherImpl(Event e, Set<EventListenerDescriptor> listeners) {
        this.listeners = listeners;
        this.e = e;
    }

    @Override
    public Event getEvent() {
        return e;
    }

    @Override
    public void dispatch() {
        final Thread currentThread = Thread.currentThread();
        final ClassLoader previousClassLoader = currentThread.getContextClassLoader();

        for (EventListenerDescriptor eventListenerWrapper : listeners) {
            if (eventListenerWrapper.answersTo(e.type())) {
                currentThread.setContextClassLoader(eventListenerWrapper.getClassLoader());
                try {
                    eventListenerWrapper.getListener().onEvent(e);
                } finally {
                    currentThread.setContextClassLoader(previousClassLoader);
                }
            }
        }
    }
}
