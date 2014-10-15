package org.openrepose.services.datastore.impl
import org.openrepose.services.datastore.Datastore
import org.openrepose.services.datastore.DatastoreManager
import org.openrepose.services.datastore.distributed.DistributedDatastore
import org.junit.Before
import org.junit.Test

import static org.junit.Assert.assertNotNull
import static org.junit.Assert.assertNull
import static org.mockito.Mockito.mock
import static org.mockito.Mockito.when

public class DatastoreServiceImplTest {

    private DatastoreServiceImpl datastoreService;
    private Datastore localDatastore;
    private DatastoreManager localManager;
    private DistributedDatastore remoteDatastore;
    private DatastoreManager remoteManager;

    @Before
    public void setUp() {
        datastoreService = new DatastoreServiceImpl();
        localDatastore = mock(Datastore.class);
        localManager = mock(DatastoreManager.class);
        when(localManager.getDatastore()).thenReturn(localDatastore);
        when(localManager.isDistributed()).thenReturn(false);

        remoteDatastore = mock(DistributedDatastore.class);
        remoteManager = mock(DatastoreManager.class);
        when(remoteManager.getDatastore()).thenReturn(remoteDatastore);
        when(remoteManager.isDistributed()).thenReturn(true);
    }

    @Test
    public void shouldGetDefaultDatastore() {
        assertNotNull(datastoreService.getDefaultDatastore());
    }

    @Test
    public void shouldDeleteDatastoreByName() {
        datastoreService.distributedManagers.put("foo", remoteManager)
        assertNotNull(datastoreService.getDatastore("foo"));
        datastoreService.destroyDatastore("foo");
        assertNull(datastoreService.getDatastore("foo"));
    }

    @Test
    public void shouldGetRemoteDatastoreByName() {
        datastoreService.distributedManagers.put("foo", remoteManager)
        assertNotNull(datastoreService.getDatastore("foo"));
    }

    @Test
    public void shouldGetDistributedDatastore() {
        DatastoreManager remoteManager2 = mock(DatastoreManager.class);
        when(remoteManager2.getDatastore()).thenReturn(remoteDatastore);
        when(remoteManager2.isDistributed()).thenReturn(true);
        datastoreService.distributedManagers.put("remote1", remoteManager)
        datastoreService.distributedManagers.put("remote2", remoteManager)

        DistributedDatastore availableDistributedDatastores = datastoreService.getDistributedDatastore();

        assertNotNull(availableDistributedDatastores);
    }

}
