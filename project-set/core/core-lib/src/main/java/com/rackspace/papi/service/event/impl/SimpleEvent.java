package com.rackspace.papi.service.event.impl;

import com.rackspace.papi.service.event.common.Event;
import com.rackspace.papi.service.event.common.EventService;

public class SimpleEvent <T extends Enum, P> implements Event<T, P> {

    private final P payload;
    private final T type;
    private final EventService em;

    public SimpleEvent(T type, P payload, EventService em) {
        this.type = type;
        this.payload = payload;
        this.em = em;
    }

    @Override
    public P payload() {
        return payload;
    }

    @Override
    public T type() {
        return type;
    }

    @Override
    public EventService eventManager() {
        return em;
    }
}
