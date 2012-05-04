package com.rackspace.papi.service.event.common;


public interface EventService {

    void newEvent(Enum e, Object payload);

    EventDispatcher nextDispatcher() throws InterruptedException;

     <T extends Enum> void listen(EventListener<T, ?> el, Class<T> events);

     <T extends Enum> void listen(EventListener<T, ?> el, T... events);

     <T extends Enum> void squelch(EventListener<T, ?> el, Class<T> events);

     <T extends Enum> void squelch(EventListener<T, ?> el, T... events);
}
