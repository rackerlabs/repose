package com.rackspace.papi.service.datastore.impl;

import com.rackspace.papi.service.datastore.Datastore;
import com.rackspace.papi.service.datastore.DatastoreManager;
import com.rackspace.papi.service.datastore.DatastoreService;
import java.util.Collection;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import static org.mockito.Mockito.*;

@RunWith(Enclosed.class)
public class PowerApiDatastoreServiceTest {

    public static class WhenGettingDatastores {

        private PowerApiDatastoreService instance;
        private Datastore localDatastore;
        private DatastoreManager localManager;
        private Datastore remoteDatastore;
        private DatastoreManager remoteManager;

        @Before
        public void setUp() {
            instance = new PowerApiDatastoreService();
            localDatastore = mock(Datastore.class);
            localManager = mock(DatastoreManager.class);
            when(localManager.getDatastore()).thenReturn(localDatastore);
            when(localManager.isDistributed()).thenReturn(false);
            when(localManager.isAvailable()).thenReturn(true);

            remoteDatastore = mock(Datastore.class);
            remoteManager = mock(DatastoreManager.class);
            when(remoteManager.getDatastore()).thenReturn(remoteDatastore);
            when(remoteManager.isDistributed()).thenReturn(true);
            when(remoteManager.isAvailable()).thenReturn(true);
        }

        @Test
        public void shouldGetNullDatastoreWhenNoneRegistered() {
            assertNull(instance.defaultDatastore());
        }

        @Test
        public void shouldGetDefaultDatastore() {
            instance.registerDatastoreManager(DatastoreService.DEFAULT_LOCAL, localManager);
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
        public void shouldGetAvailableDistManagers() {
            Datastore unavailableDatastore = mock(Datastore.class);
            DatastoreManager unavailableManager = mock(DatastoreManager.class);
            when(unavailableManager.getDatastore()).thenReturn(unavailableDatastore);
            when(unavailableManager.isDistributed()).thenReturn(true);
            when(unavailableManager.isAvailable()).thenReturn(false);
            
            instance.registerDatastoreManager("local", localManager);
            instance.registerDatastoreManager("remote", remoteManager);
            instance.registerDatastoreManager("unavail", unavailableManager);
            Collection<DatastoreManager> availableDistributedDatastores = instance.availableDistributedDatastores();
            
            assertEquals(1, availableDistributedDatastores.size());
            Collection<DatastoreManager> availableLocalDatastores = instance.availableLocalDatastores();
            
            assertEquals(1, availableLocalDatastores.size());
        }
        
    }
}
