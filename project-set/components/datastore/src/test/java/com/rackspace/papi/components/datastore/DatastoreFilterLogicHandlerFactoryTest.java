package com.rackspace.papi.components.datastore;

import com.rackspace.papi.components.datastore.hash.HashRingDatastoreManager;
import com.rackspace.papi.model.PowerProxy;
import com.rackspace.papi.service.datastore.DatastoreManager;
import com.rackspace.papi.service.datastore.DatastoreService;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import static org.mockito.Mockito.*;

@RunWith(Enclosed.class)
public class DatastoreFilterLogicHandlerFactoryTest {

    public static class WhenRegisteringDatastore {

        private DatastoreFilterLogicHandlerFactory factory;
        private DatastoreService datastoreService;
        private PowerProxy powerProxy;

        @Before
        public void standUp() {
            powerProxy = new PowerProxy();
            
            datastoreService = mock(DatastoreService.class);
            factory = new DatastoreFilterLogicHandlerFactory(datastoreService);
        }

        @Test
        public void shouldRegisterDatastore() throws Exception {
            factory.configurationUpdated(powerProxy);

            verify(datastoreService).registerDatastoreManager(eq(HashRingDatastoreManager.DATASTORE_MANAGER_NAME), any(DatastoreManager.class));
        }

        @Test
        public void shouldNotReRegisterDatastore() throws Exception {
            factory.configurationUpdated(powerProxy);
            factory.configurationUpdated(powerProxy);
            
            verify(datastoreService, times(1)).registerDatastoreManager(eq(HashRingDatastoreManager.DATASTORE_MANAGER_NAME), any(DatastoreManager.class));
        }

        @Test @Ignore
        public void shouldUnRegisterDatastore() throws Exception {
            factory.configurationUpdated(powerProxy);
            // Not implemented
            
            verify(datastoreService, times(1)).registerDatastoreManager(eq(HashRingDatastoreManager.DATASTORE_MANAGER_NAME), any(DatastoreManager.class));
            verify(datastoreService, times(1)).unregisterDatastoreManager(HashRingDatastoreManager.DATASTORE_MANAGER_NAME);
        }
    }
}
