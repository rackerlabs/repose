package com.rackspace.papi.service.httpclient.impl

import org.junit.Before
import org.junit.Test

import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertTrue

class HttpClientUserManagerTest {

    HttpClientUserManager clientUserManager
    String clientId1 = "test"

    @Before
    void setUp() {
        clientUserManager = new HttpClientUserManager()
    }

    @Test
    void whenAddingAUserToRegisteredClientUsers() {
        String result = clientUserManager.addUser(clientId1)

        assertTrue(clientUserManager.registeredClientUsers.containsKey(clientId1))
        assertTrue(clientUserManager.registeredClientUsers.get(clientId1).size() == 1)
        assertEquals(result, clientUserManager.registeredClientUsers.get(clientId1).get(0))
    }

    @Test
    void whenAddingMultipleUsersForTheSameClient() {
        String user1 = clientUserManager.addUser(clientId1)
        String user2 = clientUserManager.addUser(clientId1)

        assertTrue(clientUserManager.registeredClientUsers.containsKey(clientId1))
        assertEquals(clientUserManager.registeredClientUsers.get(clientId1).size(), 2)
        assertEquals(user1, clientUserManager.registeredClientUsers.get(clientId1).get(0))
        assertEquals(user2, clientUserManager.registeredClientUsers.get(clientId1).get(1))
    }


    @Test (expected = IllegalArgumentException.class)
    void whenAddingNullUsers() {
        clientUserManager.addUser(null)
    }

    @Test (expected = IllegalArgumentException.class)
    void whenAddingEmptyUsers() {
        clientUserManager.addUser("")
    }

    @Test (expected = IllegalArgumentException.class)
    void whenRemovingNullUsers() {
        clientUserManager.removeUser("clientInstanceId", null)
    }

    @Test (expected = IllegalArgumentException.class)
    void whenRemovingEmptyUsers() {
        clientUserManager.removeUser("clientInstanceId", "")
    }

    @Test (expected = IllegalArgumentException.class)
    void whenRemovingNullClient() {
        clientUserManager.removeUser(null, "userId")
    }

    @Test (expected = IllegalArgumentException.class)
    void whenAddingEmptyClient() {
        clientUserManager.removeUser("", "userId")
    }

    @Test
    void whenRemovingAUserFromRegisteredClientUsers() {
        String result = clientUserManager.addUser(clientId1)

        assertTrue(clientUserManager.registeredClientUsers.containsKey(clientId1))
        assertTrue(clientUserManager.registeredClientUsers.get(clientId1).size() == 1)

        clientUserManager.removeUser(clientId1, result)

        assertTrue(clientUserManager.registeredClientUsers.containsKey(clientId1))
        assertTrue(clientUserManager.registeredClientUsers.get(clientId1).size() == 0)
    }

    @Test
    void whenRemovingAnUnregisteredUserFromRegisteredClientUsers() {
        String userId = clientUserManager.addUser(clientId1)

        assertTrue(clientUserManager.registeredClientUsers.containsKey(clientId1))
        assertTrue(clientUserManager.registeredClientUsers.get(clientId1).size() == 1)

        clientUserManager.removeUser(clientId1, "notTheRegisteredUserId")

        assertTrue(clientUserManager.registeredClientUsers.containsKey(clientId1))
        assertTrue(clientUserManager.registeredClientUsers.get(clientId1).size() == 1)
        assertEquals(clientUserManager.registeredClientUsers.get(clientId1).get(0), userId)
    }

    @Test
    void whenRemovingARegisteredUserFromAnIncorrectClientId() {
        String userId = clientUserManager.addUser(clientId1)

        assertTrue(clientUserManager.registeredClientUsers.containsKey(clientId1))
        assertTrue(clientUserManager.registeredClientUsers.get(clientId1).size() == 1)

        clientUserManager.removeUser("invalid client id", userId)

        assertTrue(clientUserManager.registeredClientUsers.containsKey(clientId1))
        assertTrue(clientUserManager.registeredClientUsers.get(clientId1).size() == 1)
        assertEquals(clientUserManager.registeredClientUsers.get(clientId1).get(0), userId)
    }
}
