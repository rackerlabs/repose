/*
 * _=_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_=
 * Repose
 * _-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-
 * Copyright (C) 2010 - 2015 Rackspace US, Inc.
 * _-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * =_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_=_
 */
package org.openrepose.core.filter.logic;

import org.openrepose.commons.config.manager.UpdateFailedException;
import org.openrepose.commons.config.manager.UpdateListener;
import org.openrepose.commons.utils.thread.KeyedStackLock;

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
    public void configurationUpdated(Object configurationObject) throws UpdateFailedException {
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
