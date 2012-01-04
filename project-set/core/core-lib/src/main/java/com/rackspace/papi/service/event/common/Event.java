package com.rackspace.papi.service.event.common;

public interface Event <T extends Enum, P> {

    T type();

    P payload();
    
    EventService eventManager();
}
