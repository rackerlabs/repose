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
package org.openrepose.core.services.event.common.impl;

import org.openrepose.core.services.event.common.EventListener;

import java.util.*;

public class EventListenerDescriptor<T extends Enum> {

    private final EventListener<T, ?> listener;
    private final Set<T> subscriptions;

    public EventListenerDescriptor(EventListener<T, ?> listener, Collection<T> targetedEvents) {
        this.subscriptions = new HashSet<T>(targetedEvents);
        this.listener = listener;
    }

    public EventListener<T, ?> getListener() {
        return listener;
    }

    public void listenFor(Collection<T> types) {
        subscriptions.addAll(types);
    }

    public boolean silence(Collection<T> types) {
        // Create a local copy
        final List<T> typesToRemove = new LinkedList<T>(types);
        final Iterator<T> targetedEventIterator = subscriptions.iterator();

        while (targetedEventIterator.hasNext()) {
            final T event = targetedEventIterator.next();

            if (typesToRemove.remove(event)) {
                targetedEventIterator.remove();
            }
        }

        return subscriptions.isEmpty();
    }

    public boolean answersTo(T typeToLookFor) {
        for (T eventType : subscriptions) {
            if (eventType == typeToLookFor) {
                return true;
            }
        }

        return false;
    }
}
