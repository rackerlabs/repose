package com.rackspace.papi.service.datastore.impl;

import com.rackspace.papi.service.datastore.Datastore;
import com.rackspace.papi.service.datastore.DatastoreManager;

import java.util.Collection;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import static org.mockito.Mockito.*;

@RunWith(Enclosed.class)
public class DatastoreServiceImplTest {

    public static class WhenGettingDatastores {

        private DatastoreServiceImpl instance;
        private Datastore localDatastore;
        private DatastoreManager localManager;
        private Datastore remoteDatastore;
        private DatastoreManager remoteManager;

        @Before
        public void setUp() {
            instance = new DatastoreServiceImpl();
            localDatastore = mock(Datastore.class);
            localManager = mock(DatastoreManager.class);
            when(localManager.getDatastore()).thenReturn(localDatastore);
            when(localManager.isDistributed()).thenReturn(false);

            remoteDatastore = mock(Datastore.class);
            remoteManager = mock(DatastoreManager.class);
            when(remoteManager.getDatastore()).thenReturn(remoteDatastore);
            when(remoteManager.isDistributed()).thenReturn(true);
        }

        @Test
        public void shouldGetNullDatastoreWhenNoneRegistered() {
            assertNull(instance.defaultDatastore());
        }

        @Test
        public void shouldGetDefaultDatastore() {
            instance.registerDatastoreManager(Datastore.DEFAULT_LOCAL, localManager);
            assertNotNull(instance.defaultDatastore());
        }
        
        @Test
        public void shouldGetLocalDatastoreByName() {
            final String name = "blah";
            instance.registerDatastoreManager(name, localManager);
            assertNotNull(instance.getDatastore(name));
        }
        
        @Test
        public void shouldUnregisterLocalDatastore() {
            final String name = "blah";
            instance.registerDatastoreManager(name, localManager);
            assertNotNull(instance.getDatastore(name));
            instance.unregisterDatastoreManager(name);
            assertNull(instance.getDatastore(name));
        }
        
        @Test
        public void shouldGetRemoteDatastoreByName() {
            final String name = "blah";
            instance.registerDatastoreManager(name, remoteManager);
            assertNotNull(instance.getDatastore(name));
        }
        
        @Test
        public void shouldGetDistributedManagers() {
            Datastore remoteDatastore = mock(Datastore.class);
            DatastoreManager remoteManager2 = mock(DatastoreManager.class);
            when(remoteManager2.getDatastore()).thenReturn(remoteDatastore);
            when(remoteManager2.isDistributed()).thenReturn(true);

            instance.registerDatastoreManager("local", this.localManager);
            instance.registerDatastoreManager("remote", remoteManager);
            instance.registerDatastoreManager("remote2", remoteManager2);
            Collection<DatastoreManager> availableDistributedDatastores = instance.availableDistributedDatastores();
            
            assertEquals(2, availableDistributedDatastores.size());
            Collection<DatastoreManager> availableLocalDatastores = instance.availableLocalDatastores();
            
            assertEquals(1, availableLocalDatastores.size());
        }
        
    }
}
