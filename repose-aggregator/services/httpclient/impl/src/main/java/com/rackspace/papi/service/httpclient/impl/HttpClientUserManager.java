package com.rackspace.papi.service.httpclient.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class HttpClientUserManager {

    private Map<String, ArrayList<String>> registeredClientUsers = new ConcurrentHashMap<String, ArrayList<String>>();

    String addUser(String clientId) {

        if (clientId == null || clientId.isEmpty()) {
            throw new IllegalArgumentException("No client ID provided!");
        }

        String userId = UUID.randomUUID().toString();

        if (registeredClientUsers.containsKey(clientId)) {
            ArrayList<String> users = registeredClientUsers.get(clientId);
            users.addAll(Arrays.asList(userId));
            registeredClientUsers.put(clientId, users);
        } else {
            ArrayList<String> users = new ArrayList<String>();
            users.add(userId);
            registeredClientUsers.put(clientId, users);
        }

        return userId;
    }

    void removeUser(String clientId, String userId) {

        if (clientId == null || clientId.isEmpty() || userId == null || userId.isEmpty()) {
            throw new IllegalArgumentException("No client and/or user ID provided!");
        }

        if (registeredClientUsers.containsKey(clientId)) {
            registeredClientUsers.get(clientId).remove(userId);
        }
    }

    public boolean hasUsers(String clientId) {
        if (!registeredClientUsers.containsKey(clientId) || registeredClientUsers.get(clientId).size() == 0) {
            return false;
        }

        return true;
    }
}
