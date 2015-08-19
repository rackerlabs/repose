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
package org.openrepose.filters.clientauth.atomfeed;

import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Deprecated
public class FeedCacheKeys implements CacheKeys {


    private Map<CacheKeyType, Set<String>> keys;

    public FeedCacheKeys() {
        keys = new EnumMap(CacheKeyType.class);
        for (CacheKeyType type : CacheKeyType.values()) {
            keys.put(type, new HashSet<String>());
        }
    }


    @Override
    public void addTokenKey(String key) {
        keys.get(CacheKeyType.TOKEN).add(key);
    }

    @Override
    public void addUserKey(String key) {
        keys.get(CacheKeyType.USER).add(key);
    }

    @Override
    public Set<String> getTokenKeys() {
        return keys.get(CacheKeyType.TOKEN);
    }


    @Override
    public Set<String> getUserKeys() {
        return keys.get(CacheKeyType.USER);
    }
}
