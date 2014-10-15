package org.openrepose.core.services.event.common;

public interface EventListener<T extends Enum, P> {

    void onEvent(Event<T, P> e);
}
