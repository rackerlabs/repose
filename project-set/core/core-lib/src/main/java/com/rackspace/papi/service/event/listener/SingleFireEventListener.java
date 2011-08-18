package com.rackspace.papi.service.event.listener;

import com.rackspace.papi.service.event.Event;

public abstract class SingleFireEventListener <T extends Enum, P> implements EventListener<T, P> {

    private final Class<T> eventClass;
    private boolean fired;

    public SingleFireEventListener(Class<T> eventClass) {
        this.eventClass = eventClass;
    }
    
    @Override
    public synchronized void onEvent(Event<T, P> e) {
        if (!fired) {
            onlyOnce(e);
            
            fired = true;
        }
        
        e.eventManager().squelch(this, eventClass);
    }
    
    public abstract void onlyOnce(Event<T, P> e);
}
