package org.openrepose.core.services.event.common;

public interface Event <T extends Enum, P> {

    T type();

    P payload();
    
    EventService eventManager();
}
