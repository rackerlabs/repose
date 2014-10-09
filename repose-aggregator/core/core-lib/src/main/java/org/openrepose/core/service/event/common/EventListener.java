package org.openrepose.core.service.event.common;

public interface EventListener<T extends Enum, P> {

    void onEvent(Event<T, P> e);
}
