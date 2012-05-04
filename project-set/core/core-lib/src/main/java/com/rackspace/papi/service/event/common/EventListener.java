package com.rackspace.papi.service.event.common;

public interface EventListener<T extends Enum, P> {

    void onEvent(Event<T, P> e);
}
