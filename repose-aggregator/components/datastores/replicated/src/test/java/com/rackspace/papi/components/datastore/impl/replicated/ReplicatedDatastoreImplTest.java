package com.rackspace.papi.components.datastore.impl.replicated;

import com.rackspace.papi.components.datastore.impl.replicated.data.Subscriber;
import com.rackspace.papi.components.datastore.impl.replicated.notification.out.UpdateNotifier;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.Configuration;
import org.junit.*;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.*;

public class ReplicatedDatastoreImplTest {

    private static CacheManager ehCacheManager;

    @BeforeClass
    public static void setUpClass() {
        Configuration defaultConfiguration = new Configuration();
        defaultConfiguration.setName("TestCacheManager");
        defaultConfiguration.setDefaultCacheConfiguration(new CacheConfiguration().diskPersistent(false));
        defaultConfiguration.setUpdateCheck(false);

        ehCacheManager = CacheManager.newInstance(defaultConfiguration);
    }

    @AfterClass
    public static void tearDownClass() {
        ehCacheManager.removalAll();
        ehCacheManager.shutdown();
    }
    private ReplicatedCacheDatastoreManager manager;
    private ReplicatedDatastoreImpl datastore;
    private Subscriber subscriber1;
    private Subscriber subscriber2;

    @Before
    public void setUp() {
        manager = new ReplicatedCacheDatastoreManager(ehCacheManager, null, "127.0.0.1", 0, 0);
        datastore = (ReplicatedDatastoreImpl) manager.getDatastore();
        datastore.leaveGroup();
        subscriber1 = new Subscriber("host1", 1, 1);
        subscriber2 = new Subscriber("host2", 2, 2);
    }

    @After
    public void tearDown() {
    }

    @Test
    public void shouldAddSubscribers() {
        datastore.addSubscriber(subscriber1);
        datastore.addSubscriber(subscriber2);

        assertEquals(2, datastore.getUpdateNotifier().getSubscribers().size());
    }

    @Test
    public void shouldPutMessageInNotificationQueue() {
        datastore.addSubscriber(subscriber1);
        datastore.addSubscriber(subscriber2);

        String key = "key";
        String data = "1,2,3";

        String actual = (String) datastore.get(key);
        assertNull(actual);

        assertEquals(0, ((UpdateNotifier) datastore.getUpdateNotifier()).getQueue().size());

        datastore.put(key, data, true);
        actual = (String)datastore.get(key);
        assertNotNull(actual);

        assertEquals(2, ((UpdateNotifier) datastore.getUpdateNotifier()).getQueue().size());

    }

    @Test
    public void shouldPutMessageInNotificationQueueWhenRemovingItems() {
        datastore.addSubscriber(subscriber1);
        datastore.addSubscriber(subscriber2);

        String key = "key";
        String data = "1,2,3";

        String actual = (String)datastore.get(key);
        assertNull(actual);
        datastore.put(key, data, false);
        actual = (String)datastore.get(key);
        assertNotNull(actual);

        assertEquals(0, ((UpdateNotifier) datastore.getUpdateNotifier()).getQueue().size());
        datastore.remove(key, true);

        assertEquals(2, ((UpdateNotifier) datastore.getUpdateNotifier()).getQueue().size());

    }

    @Test
    public void getName_returnsExpectedName() throws Exception {
        assertThat(datastore.getName(), equalTo(ReplicatedCacheDatastoreManager.REPLICATED_DISTRIBUTED));

    }
}
