package com.rackspace.papi.service.event;

import com.rackspace.papi.service.event.listener.EventListener;

public interface EventService {

    void newEvent(Enum e, Object payload);

    EventDispatcher nextDispatcher() throws InterruptedException;

     <T extends Enum> void listen(EventListener<T, ?> el, Class<T> events);

     <T extends Enum> void listen(EventListener<T, ?> el, T... events);

     <T extends Enum> void squelch(EventListener<T, ?> el, Class<T> events);

     <T extends Enum> void squelch(EventListener<T, ?> el, T... events);
}
