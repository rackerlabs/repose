package com.rackspace.papi.service.event.listener;

import com.rackspace.papi.service.event.common.Event;

public interface EventListener<T extends Enum, P> {

    void onEvent(Event<T, P> e);
}
