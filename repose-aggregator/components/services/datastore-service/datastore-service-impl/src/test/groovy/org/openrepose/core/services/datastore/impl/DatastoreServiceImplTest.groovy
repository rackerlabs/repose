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
package org.openrepose.core.services.datastore.impl

import org.junit.Before
import org.junit.Test
import org.openrepose.core.services.datastore.Datastore
import org.openrepose.core.services.datastore.DatastoreManager

import static org.junit.Assert.assertNotNull
import static org.junit.Assert.assertNull
import static org.mockito.Mockito.mock
import static org.mockito.Mockito.when

public class DatastoreServiceImplTest {

    private DatastoreServiceImpl datastoreService
    private Datastore localDatastore
    private DatastoreManager localManager
    private Datastore remoteDatastore
    private DatastoreManager remoteManager

    @Before
    public void setUp() {
        datastoreService = new DatastoreServiceImpl(Optional.empty())
        localDatastore = mock(Datastore.class)
        localManager = mock(DatastoreManager.class)
        when(localManager.getDatastore()).thenReturn(localDatastore)
        when(localManager.isDistributed()).thenReturn(false)

        remoteDatastore = mock(Datastore.class)
        remoteManager = mock(DatastoreManager.class)
        when(remoteManager.getDatastore()).thenReturn(remoteDatastore)
        when(remoteManager.isDistributed()).thenReturn(true)
    }

    @Test
    public void shouldGetDefaultDatastore() {
        assertNotNull(datastoreService.getDefaultDatastore())
    }

    @Test
    public void shouldDeleteDatastoreByName() {
        datastoreService.distributedManagers.put("foo", remoteManager)
        assertNotNull(datastoreService.getDatastore("foo"))
        datastoreService.destroyDatastore("foo")
        assertNull(datastoreService.getDatastore("foo"))
    }

    @Test
    public void shouldGetRemoteDatastoreByName() {
        datastoreService.distributedManagers.put("foo", remoteManager)
        assertNotNull(datastoreService.getDatastore("foo"))
    }

    @Test
    public void shouldGetDistributedDatastore() {
        DatastoreManager remoteManager2 = mock(DatastoreManager.class)
        when(remoteManager2.getDatastore()).thenReturn(remoteDatastore)
        when(remoteManager2.isDistributed()).thenReturn(true)
        datastoreService.distributedManagers.put("remote1", remoteManager)
        datastoreService.distributedManagers.put("remote2", remoteManager)

        Datastore availableDistributedDatastores = datastoreService.getDistributedDatastore()

        assertNotNull(availableDistributedDatastores)
    }

}
