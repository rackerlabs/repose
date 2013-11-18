package com.rackspace.papi.filter.logic;

import com.rackspace.papi.commons.config.manager.UpdateListener;
import com.rackspace.papi.commons.util.thread.KeyedStackLock;

import java.util.Map;

/**
 * @author Dan Daley
 */
public abstract class AbstractConfiguredFilterHandlerFactory<T extends FilterLogicHandler> implements UpdateListener {

    private final Map<Class, UpdateListener<?>> listeners;
    private final KeyedStackLock configurationLock;
    private final Object readKey, updateKey;

    public AbstractConfiguredFilterHandlerFactory() {
        configurationLock = new KeyedStackLock();

        readKey = new Object();
        updateKey = new Object();
        listeners = getListeners();
    }

    protected abstract T buildHandler();

    protected abstract Map<Class, UpdateListener<?>> getListeners();

    public T newHandler() {
        configurationLock.lock(readKey);
        try {
            return buildHandler();
        } finally {
            configurationLock.unlock(readKey);
        }
    }

    public UpdateListener getListener(Class configClass) {
        return listeners.get(configClass);
    }

    @Override
    public void configurationUpdated(Object configurationObject) {
        UpdateListener listener = listeners.get(configurationObject.getClass());
        if (listener != null) {

            configurationLock.lock(updateKey);
            try {
                listener.configurationUpdated(configurationObject);
            } finally {
                configurationLock.unlock(updateKey);
            }

        }

    }

    @Override
    public boolean isInitialized() {

        for (UpdateListener<?> listener : listeners.values()) {
            if (!listener.isInitialized()) {
                return false;
            }
        }

        return true;
    }
}
