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
package org.openrepose.core.services.httpclient.impl;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class HttpClientUserManager {

    private Map<String, List<String>> registeredClientUsers = new ConcurrentHashMap<String, List<String>>();

    String addUser(String clientInstanceId) {

        if (clientInstanceId == null || clientInstanceId.isEmpty()) {
            throw new IllegalArgumentException("No client ID provided!");
        }

        String userId = UUID.randomUUID().toString();
        final List<String> users;

        if (registeredClientUsers.containsKey(clientInstanceId)) {
            users = registeredClientUsers.get(clientInstanceId);
        } else {
            users = Collections.synchronizedList(new ArrayList<String>());
        }

        users.add(userId);
        registeredClientUsers.put(clientInstanceId, users);
        return userId;
    }

    void removeUser(String clientInstanceId, String userId) {

        if (clientInstanceId == null || clientInstanceId.isEmpty() || userId == null || userId.isEmpty()) {
            throw new IllegalArgumentException("No client and/or user ID provided!");
        }

        if (registeredClientUsers.containsKey(clientInstanceId)) {
            registeredClientUsers.get(clientInstanceId).remove(userId);
        }
    }

    public boolean hasUsers(String clientInstanceId) {
        if (!registeredClientUsers.containsKey(clientInstanceId) || registeredClientUsers.get(clientInstanceId).size() == 0) {
            return false;
        }

        return true;
    }
}
