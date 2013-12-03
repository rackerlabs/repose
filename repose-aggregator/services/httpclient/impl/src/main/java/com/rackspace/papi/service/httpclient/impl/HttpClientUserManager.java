package com.rackspace.papi.service.httpclient.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
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
