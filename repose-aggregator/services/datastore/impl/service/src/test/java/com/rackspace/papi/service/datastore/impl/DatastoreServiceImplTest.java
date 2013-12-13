package com.rackspace.papi.service.datastore.impl;

import com.rackspace.papi.service.datastore.Datastore;
import com.rackspace.papi.service.datastore.DatastoreManager;
import com.rackspace.papi.service.datastore.DistributedDatastore;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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
            assertNull(instance.getDefaultDatastore());
        }

        @Test
        public void shouldGetDefaultDatastore() {
            instance.createDatastoreManager(Datastore.DEFAULT_LOCAL, localManager);
            assertNotNull(instance.getDefaultDatastore());
        }
        
        @Test
        public void shouldGetLocalDatastoreByName() {
            final String name = "blah";
            instance.createDatastoreManager(name, localManager);
            assertNotNull(instance.getDatastore(name));
        }
        
        @Test
        public void shouldUnregisterLocalDatastore() {
            final String name = "blah";
            instance.createDatastoreManager(name, localManager);
            assertNotNull(instance.getDatastore(name));
            instance.destroyDatastore(name);
            assertNull(instance.getDatastore(name));
        }
        
        @Test
        public void shouldGetRemoteDatastoreByName() {
            final String name = "blah";
            instance.createDatastoreManager(name, remoteManager);
            assertNotNull(instance.getDatastore(name));
        }
        
        @Test
        public void shouldGetDistributedDatastore() {
            DistributedDatastore remoteDatastore = mock(DistributedDatastore.class);
            DatastoreManager remoteManager2 = mock(DatastoreManager.class);
            when(remoteManager2.getDatastore()).thenReturn(remoteDatastore);
            when(remoteManager2.isDistributed()).thenReturn(true);

            instance.createDatastoreManager("local", this.localManager);
            instance.createDatastoreManager("remote", remoteManager);
            instance.createDatastoreManager("remote2", remoteManager2);
            DistributedDatastore availableDistributedDatastores = instance.getDistributedDatastore();
            
            assertNotNull(availableDistributedDatastores);
        }
        
    }
}
