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
package org.openrepose.core.services.httpclient.impl

import org.junit.Before
import org.junit.Test

import static org.hamcrest.CoreMatchers.equalTo
import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertThat
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
        assertThat(clientUserManager.registeredClientUsers.get(clientId1).size(), equalTo(1))
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


    @Test(expected = IllegalArgumentException.class)
    void whenAddingNullUsers() {
        clientUserManager.addUser(null)
    }

    @Test(expected = IllegalArgumentException.class)
    void whenAddingEmptyUsers() {
        clientUserManager.addUser("")
    }

    @Test(expected = IllegalArgumentException.class)
    void whenRemovingNullUsers() {
        clientUserManager.removeUser("clientInstanceId", null)
    }

    @Test(expected = IllegalArgumentException.class)
    void whenRemovingEmptyUsers() {
        clientUserManager.removeUser("clientInstanceId", "")
    }

    @Test(expected = IllegalArgumentException.class)
    void whenRemovingNullClient() {
        clientUserManager.removeUser(null, "userId")
    }

    @Test(expected = IllegalArgumentException.class)
    void whenAddingEmptyClient() {
        clientUserManager.removeUser("", "userId")
    }

    @Test
    void whenRemovingAUserFromRegisteredClientUsers() {
        String result = clientUserManager.addUser(clientId1)

        assertTrue(clientUserManager.registeredClientUsers.containsKey(clientId1))
        assertThat(clientUserManager.registeredClientUsers.get(clientId1).size(), equalTo(1))

        clientUserManager.removeUser(clientId1, result)

        assertTrue(clientUserManager.registeredClientUsers.containsKey(clientId1))
        assertThat(clientUserManager.registeredClientUsers.get(clientId1).size(), equalTo(0))
    }

    @Test
    void whenRemovingAnUnregisteredUserFromRegisteredClientUsers() {
        String userId = clientUserManager.addUser(clientId1)

        assertTrue(clientUserManager.registeredClientUsers.containsKey(clientId1))
        assertThat(clientUserManager.registeredClientUsers.get(clientId1).size(), equalTo(1))

        clientUserManager.removeUser(clientId1, "notTheRegisteredUserId")

        assertTrue(clientUserManager.registeredClientUsers.containsKey(clientId1))
        assertThat(clientUserManager.registeredClientUsers.get(clientId1).size(), equalTo(1))
        assertEquals(clientUserManager.registeredClientUsers.get(clientId1).get(0), userId)
    }

    @Test
    void whenRemovingARegisteredUserFromAnIncorrectClientId() {
        String userId = clientUserManager.addUser(clientId1)

        assertTrue(clientUserManager.registeredClientUsers.containsKey(clientId1))
        assertThat(clientUserManager.registeredClientUsers.get(clientId1).size(), equalTo(1))

        clientUserManager.removeUser("invalid client id", userId)

        assertTrue(clientUserManager.registeredClientUsers.containsKey(clientId1))
        assertThat(clientUserManager.registeredClientUsers.get(clientId1).size(), equalTo(1))
        assertEquals(clientUserManager.registeredClientUsers.get(clientId1).get(0), userId)
    }
}
