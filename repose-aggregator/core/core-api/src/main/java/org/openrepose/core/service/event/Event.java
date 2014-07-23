package org.openrepose.core.service.event;

public interface Event <T extends Enum, P> {

    T type();

    P payload();
    
    EventService eventManager();
}
