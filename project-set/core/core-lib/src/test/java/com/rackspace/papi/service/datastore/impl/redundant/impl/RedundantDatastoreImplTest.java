package com.rackspace.papi.service.datastore.impl.redundant.impl;

import com.rackspace.papi.service.datastore.Datastore;
import com.rackspace.papi.service.datastore.StoredElement;
import com.rackspace.papi.service.datastore.impl.redundant.RedundantDatastore;
import com.rackspace.papi.service.datastore.impl.redundant.data.Subscriber;
import com.rackspace.papi.service.datastore.impl.redundant.notification.out.UpdateNotifier;
import java.util.Collection;
import java.util.concurrent.TimeUnit;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.Configuration;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

@RunWith(Enclosed.class)
public class RedundantDatastoreImplTest {

    public static class WhenStoreData {

        private static CacheManager ehCacheManager;

        @BeforeClass
        public static void setUpClass() {
            Configuration defaultConfiguration = new Configuration();
            defaultConfiguration.setDefaultCacheConfiguration(new CacheConfiguration().diskPersistent(false));
            defaultConfiguration.setUpdateCheck(false);

            ehCacheManager = CacheManager.create(defaultConfiguration);
        }

        @AfterClass
        public static void tearDownClass() {
            ehCacheManager.removalAll();
            ehCacheManager.shutdown();
        }
        private RedundantCacheDatastoreManager manager;
        private RedundantDatastoreImpl instance;
        private Subscriber subscriber1;
        private Subscriber subscriber2;

        @Before
        public void setUp() {
            manager = new RedundantCacheDatastoreManager(ehCacheManager, null, "127.0.0.1", 0);
            instance = (RedundantDatastoreImpl) manager.getDatastore();
            instance.leaveGroup();
            subscriber1 = new Subscriber("host1", 1, 1);
            subscriber2 = new Subscriber("host2", 2, 2);
        }

        @After
        public void tearDown() {
        }

        @Test
        public void shouldAddSubscribers() {
            instance.addSubscriber(subscriber1);
            instance.addSubscriber(subscriber2);
            
            assertEquals(2, instance.getUpdateNotifier().getSubscribers().size());
        }
        
        @Test
        public void shouldPutMessageInNotificationQueue() {
            instance.addSubscriber(subscriber1);
            instance.addSubscriber(subscriber2);
            
            String key = "key";
            byte[] data = new byte[] {1,2,3};

            byte[] actual = instance.get(key).elementBytes();
            assertNull(actual);
            
            assertEquals(0, ((UpdateNotifier)instance.getUpdateNotifier()).getQueue().size());

            instance.put(key, data, true);
            actual = instance.get(key).elementBytes();
            assertNotNull(actual);
            
            assertEquals(2, ((UpdateNotifier)instance.getUpdateNotifier()).getQueue().size());

        }

        @Test
        public void shouldPutMessageInNotificationQueueWhenRemovingItems() {
            instance.addSubscriber(subscriber1);
            instance.addSubscriber(subscriber2);
            
            String key = "key";
            byte[] data = new byte[] {1,2,3};

            byte[] actual = instance.get(key).elementBytes();
            assertNull(actual);
            instance.put(key, data, false);
            actual = instance.get(key).elementBytes();
            assertNotNull(actual);
            
            assertEquals(0, ((UpdateNotifier)instance.getUpdateNotifier()).getQueue().size());
            instance.remove(key, true);
            
            assertEquals(2, ((UpdateNotifier)instance.getUpdateNotifier()).getQueue().size());

        }
    }
}
