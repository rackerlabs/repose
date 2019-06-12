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
package org.openrepose.core.services.datastore.impl.ehcache;

import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import org.openrepose.core.services.datastore.Datastore;
import org.openrepose.core.services.datastore.Patch;
import org.openrepose.core.services.datastore.Patchable;

import java.util.concurrent.TimeUnit;

public class EHCacheDatastore implements Datastore {

    private static final String NAME = "local/default";
    private final Ehcache ehCacheInstance;

    public EHCacheDatastore(Ehcache ehCacheInstance) {
        this.ehCacheInstance = ehCacheInstance;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public boolean remove(String key) {
        return ehCacheInstance.remove(key);
    }

    @Override
    public Object get(String key) {
        Element element = ehCacheInstance.get(key);
        if (element != null) {
            return element.getObjectValue();
        } else {
            return null;
        }
    }

    @Override
    public void put(String key, Object value) {
        ehCacheInstance.put(new Element(key, value));
    }

    @Override
    public void put(String key, Object value, int ttl, TimeUnit timeUnit) {
        // todo: does not allow for eternal caching (need to either setEternal(true) or set ttl and tti to 0
        Element putMe = new Element(key, value);
        putMe.setTimeToLive((int) TimeUnit.SECONDS.convert(ttl, timeUnit));
        //todo: switch to time to idle instead of time to live?

        ehCacheInstance.put(putMe);
    }

    @Override
    public <T extends Patchable<T, P>, P extends Patch<T>> T patch(String key, P patch) {
        return patch(key, patch, -1, TimeUnit.MINUTES);
    }

    @Override
    public <T extends Patchable<T, P>, P extends Patch<T>> T patch(String key, P patch, int ttl, TimeUnit timeUnit) {
        final T newValue = patch.newFromPatch();
        final Element newElement = new Element(key, newValue);

        T returnValue = newValue;
        Element returnElement = newElement;

        Element oldElement;
        while ((oldElement = ehCacheInstance.putIfAbsent(newElement)) != null) {
            T patchedValue = ((T) oldElement.getObjectValue()).applyPatch(patch);
            Element patchedElement = new Element(key, patchedValue);

            if (ehCacheInstance.replace(oldElement, patchedElement)) {
                returnValue = patchedValue;
                returnElement = patchedElement;
                break;
            }
        }

        //todo: setting ttl can die once we move to tti
        if (ttl == 0) {
            returnElement.setTimeToLive(0);
            returnElement.setTimeToIdle(0);
        } else if (ttl > 0) {
            int convertedTtl = (int) TimeUnit.SECONDS.convert(ttl, timeUnit);
            int currentLifeSpan = (int) TimeUnit.SECONDS.convert(System.currentTimeMillis() - returnElement.getCreationTime(), TimeUnit.MILLISECONDS);
            if ((currentLifeSpan + convertedTtl) > returnElement.getTimeToLive()) {
                returnElement.setTimeToLive(currentLifeSpan + convertedTtl);
            }
            if (convertedTtl > returnElement.getTimeToIdle()) {
                returnElement.setTimeToIdle(convertedTtl);
            }
        }

        return returnValue;
    }

    @Override
    public void removeAll() {
        ehCacheInstance.removeAll();
    }
}
