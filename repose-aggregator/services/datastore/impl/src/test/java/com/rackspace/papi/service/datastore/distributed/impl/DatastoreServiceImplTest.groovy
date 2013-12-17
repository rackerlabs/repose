package com.rackspace.papi.service.datastore.distributed.impl;

import com.rackspace.papi.components.datastore.Datastore;
import com.rackspace.papi.components.datastore.DatastoreManager;
import com.rackspace.papi.components.datastore.distributed.DistributedDatastore;
import org.junit.After;
import org.junit.Before;
import org.junit.Test

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DatastoreServiceImplTest {

        private DatastoreServiceImpl instance;
        private Datastore localDatastore;
        private DatastoreManager localManager;
        private DistributedDatastore remoteDatastore;
        private DatastoreManager remoteManager;

        @Before
        public void setUp() {
            instance = new DatastoreServiceImpl();
            localDatastore = mock(Datastore.class);
            localManager = mock(DatastoreManager.class);
            when(localManager.getDatastore()).thenReturn(localDatastore);
            when(localManager.isDistributed()).thenReturn(false);

            remoteDatastore = mock(DistributedDatastore.class);
            remoteManager = mock(DatastoreManager.class);
            when(remoteManager.getDatastore()).thenReturn(remoteDatastore);
            when(remoteManager.isDistributed()).thenReturn(true);
        }

        @After
        public void tearDown() {
            instance.destroyDatastore(Datastore.DEFAULT_LOCAL);
        }

        @Test
        public void shouldGetDefaultDatastore() {
            assertNotNull(instance.getDefaultDatastore());
        }
        
        @Test
        public void shouldGetLocalDatastoreByName() {
            assertNotNull(instance.getDatastore(Datastore.DEFAULT_LOCAL));
        }
        
        @Test
        public void shouldDeleteLocalDatastore() {

            assertNotNull(instance.getDatastore(Datastore.DEFAULT_LOCAL));
            instance.destroyDatastore(Datastore.DEFAULT_LOCAL);
            assertNull(instance.getDatastore(Datastore.DEFAULT_LOCAL));
        }
        
        @Test
        public void shouldGetRemoteDatastoreByName() {
            instance.registerDatastoreManager(remoteDatastore.getName(), remoteManager);
           assertNotNull(instance.getDatastore(remoteDatastore.getName()));
        }
        
        @Test
        public void shouldGetDistributedDatastore() {
            DatastoreManager remoteManager2 = mock(DatastoreManager.class);
            when(remoteManager2.getDatastore()).thenReturn(remoteDatastore);
            when(remoteManager2.isDistributed()).thenReturn(true);
            instance.registerDatastoreManager(remoteDatastore.getName(), remoteManager2);

            DistributedDatastore availableDistributedDatastores = instance.getDistributedDatastore();
            
            assertNotNull(availableDistributedDatastores);
        }

}
