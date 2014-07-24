package org.openrepose.core.service.event;

public interface EventListener<T extends Enum, P> {

    void onEvent(Event<T, P> e);
}
